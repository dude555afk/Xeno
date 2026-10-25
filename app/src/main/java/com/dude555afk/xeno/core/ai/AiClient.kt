package com.dude555afk.xeno.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.buffer
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
 * both render text as it arrives, so [stream] has to emit per delta.
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
     * Stream a chat completion as incremental text deltas. The callback bridge
     * uses an unlimited channel so a temporarily slow UI collector cannot cause
     * trySend to drop tokens. Consumers should batch UI updates to avoid
     * repeatedly reparsing entire Markdown documents.
     */
    fun stream(config: ProviderConfig, request: ChatRequest): Flow<String> = callbackFlow {
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
        val source = factory.newEventSource(call, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) = Unit

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") {
                    close()
                    return
                }
                if (data.isBlank()) return

                val chunk = runCatching {
                    json.decodeFromString(StreamChunk.serializer(), data)
                }.getOrNull()
                val delta = chunk?.text.orEmpty()
                if (delta.isNotEmpty()) trySend(delta)
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                if (isClosedForSend) return
                val code = response?.code ?: 0
                val snippet = runCatching { response?.body?.string().orEmpty() }
                    .getOrDefault("")
                val error = when {
                    code in 400..599 && snippet.isNotBlank() -> AiException(code, snippet)
                    t != null -> t
                    else -> AiException(code, snippet.ifBlank { "stream failed with no body" })
                }
                close(error)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose { runCatching { source.cancel() } }
    }.buffer(Channel.UNLIMITED).flowOn(Dispatchers.IO)

    /**
     * Non-streaming one-shot call. Used for cheap decisions where round-tripping
     * deltas would be pure overhead.
     */
    /** Fetch the provider's live catalogue instead of relying on stale hard-coded model names. */
    suspend fun fetchModels(config: ProviderConfig): List<ModelInfo> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/models")
            .get()
            .header("Accept", "application/json")
            .apply {
                if (config.apiKey.isNotBlank()) header("Authorization", "Bearer ${config.apiKey}")
            }
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw AiException(response.code, body)
            runCatching {
                json.decodeFromString(ModelsResponse.serializer(), body).data
                    .filter { it.id.isNotBlank() }
                    .distinctBy { it.id }
                    .sortedBy { (it.name ?: it.id).lowercase() }
            }.getOrElse { throw AiException(response.code, "Invalid /models response: ${body.take(240)}") }
        }
    }

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
