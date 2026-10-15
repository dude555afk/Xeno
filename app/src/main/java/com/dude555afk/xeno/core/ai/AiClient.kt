package com.dude555afk.xeno.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

/**
 * The only thing in Xeno that talks to an AI provider.
 *
 * One client, no provider-specific branches. Every provider in
 * [ProviderPresets] speaks the OpenAI chat-completions wire format, so the
 * entire difference between Kilo, Groq, OpenRouter and a local llama.cpp box
 * is three strings in [ProviderConfig] — base URL, key, model.
 *
 * Streaming is not a nice-to-have here. The answer sheet and the summary sheet
 * both render text as it arrives, so [stream] has to emit per delta — see the
 * note on [stream] about why the obvious `flow {}` shape is the wrong one.
 */
class AiClient(
    private val http: OkHttpClient = defaultHttp(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }
) {

    /**
     * Stream a chat completion, emitting the text of each delta as it arrives.
     *
     * Built on [channelFlow] rather than `flow {}` for a specific reason. The
     * obvious implementation suspends on `suspendCancellableCoroutine` until the
     * gateway closes the stream, accumulates every delta into a buffer, and then
     * emits the whole thing once. It compiles, it type-checks, and it is
     * completely wrong: `AnswerEngine` collects this flow and re-emits a partial
     * `AnswerResult` on every item, so a single terminal emission means the
     * source cards, the skeleton and the progressive text all render as one
     * jump at the four-second mark — the exact "instant answer feels like a
     * bug" problem streaming exists to avoid.
     *
     * [channelFlow] lets the OkHttp callback push into the flow directly, so
     * downstream actually sees the tokens as the gateway produces them.
     *
     * Throws [AiException] when the gateway answers with an error body rather
     * than a stream — the caller's job is to degrade, not to crash.
     */
    fun stream(config: ProviderConfig, request: ChatRequest): Flow<String> = channelFlow {
        require(config.baseUrl.isNotBlank()) { "provider baseUrl is blank" }
        require(config.model.isNotBlank()) { "provider model is blank" }

        val url = config.baseUrl.trimEnd('/') + "/chat/completions"
        val body = json.encodeToString(ChatRequest.serializer(), request.copy(stream = true))
            .toRequestBody(JSON)

        val call = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .apply {
                if (config.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${config.apiKey}")
                }
            }
            .build()

        val factory = EventSources.createFactory(http)

        val listener = object : EventSourceListener() {

            override fun onOpen(eventSource: EventSource, response: Response) {
                // Nothing to do — events carry the payload.
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data.isBlank()) return
                if (data == "[DONE]") {
                    // Normal end of stream. The socket close usually follows
                    // immediately, but do not depend on it.
                    channel.close()
                    return
                }

                val chunk = runCatching {
                    json.decodeFromString(StreamChunk.serializer(), data)
                }.getOrNull() ?: return

                val text = chunk.text
                if (text.isNotEmpty()) trySend(text)
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val code = response?.code ?: 0
                val snippet = runCatching {
                    response?.body?.string().orEmpty()
                }.getOrDefault("")

                // A 4xx/5xx with a body is a gateway rejection, which is a
                // different failure from a dropped socket. The caller shows a
                // different message for each.
                val error = when {
                    code in 400..599 && snippet.isNotBlank() -> AiException(code, snippet)
                    t != null -> t
                    else -> AiException(code, snippet.ifBlank { "stream failed with no body" })
                }
                channel.close(error)
            }

            override fun onClosed(eventSource: EventSource) {
                // Normal termination without an explicit [DONE]. Plenty of
                // gateways just close the socket.
                channel.close()
            }
        }

        val source = factory.newEventSource(call, listener)
        awaitClose { runCatching { source.cancel() } }
    }.flowOn(Dispatchers.IO)

    /**
     * Non-streaming one-shot call. Used for cheap decisions where round-tripping
     * deltas would be pure overhead.
     */
    suspend fun complete(config: ProviderConfig, request: ChatRequest): String =
        withContext(Dispatchers.IO) {
            val url = config.baseUrl.trimEnd('/') + "/chat/completions"
            val body = json.encodeToString(ChatRequest.serializer(), request.copy(stream = false))
                .toRequestBody(JSON)

            val call = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .apply {
                    if (config.apiKey.isNotBlank()) {
                        header("Authorization", "Bearer ${config.apiKey}")
                    }
                }
                .build()

            http.newCall(call).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw AiException(response.code, text)
                runCatching {
                    json.decodeFromString(ChatResponse.serializer(), text).text
                }.getOrElse { throw AiException(response.code, text.take(300)) }
            }
        }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            // No write timeout worth speaking of — prompts are small, but a slow
            // gateway on a phone in India should not be cut off mid-sentence.
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
