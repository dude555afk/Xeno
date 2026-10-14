package com.dude555afk.xeno.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible streaming chat client.
 *
 * Talks SSE by hand rather than pulling in an SDK — the format is trivial and
 * it keeps the dependency list to OkHttp, which the app already needs.
 */
class AiClient(
    private val client: OkHttpClient = defaultClient(),
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {

    /**
     * Streams the assistant's reply as text deltas.
     *
     * Emits `[DONE]`-terminated chunks. Throws [AiException] on an HTTP error
     * body so the UI can show something actionable instead of silence.
     */
    fun stream(config: ProviderConfig, request: ChatRequest): Flow<String> = flow {
        val payload = json.encodeToString(ChatRequest.serializer(), request)

        val httpRequest = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(payload.toRequestBody(JSON))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw AiException(response.code, response.body?.string().orEmpty())
            }
            val body = response.body ?: throw AiException(-1, "empty body")

            body.charStream().buffered().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line!!.trim()
                    if (trimmed.isEmpty()) continue
                    if (!trimmed.startsWith("data:")) continue

                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") break

                    val chunk = runCatching {
n                        json.decodeFromString(StreamChunk.serializer(), data)
                    }.getOrNull() ?: continue

                    if (chunk.text.isNotEmpty()) emit(chunk.text)
                    if (chunk.finishReason == "stop" || chunk.finishReason == "length") break
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /** One-shot, non-streaming call. For intent classification and short jobs. */
    suspend fun complete(
        config: ProviderConfig,
        request: ChatRequest
    ): String = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val nonStreaming = request.copy(stream = false)
        val payload = json.encodeToString(ChatRequest.serializer(), nonStreaming)

        val httpRequest = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(JSON))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                throw AiException(response.code, response.body?.string().orEmpty())
            }
            val raw = response.body?.string().orEmpty()
            json.decodeFromString(ChatResponse.serializer(), raw).text
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)   // streaming needs a long read
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
