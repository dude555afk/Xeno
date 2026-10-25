package com.dude555afk.xeno.core.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
) {
    companion object {
        fun system(text: String) = ChatMessage("system", text)
        fun user(text: String) = ChatMessage("user", text)
        fun assistant(text: String) = ChatMessage("assistant", text)
    }
}

/**
 * A chat-completions request. Only the fields we actually set — serializing a
 * wall of nulls makes debugging gateway responses miserable.
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null
)

/** One streamed delta from the gateway. */
@Serializable
data class StreamChunk(
    val id: String? = null,
    val choices: List<ChunkChoice> = emptyList()
) {
    val text: String
        get() = choices.firstOrNull()?.delta?.content.orEmpty()

    val finishReason: String?
        get() = choices.firstOrNull()?.finish_reason
}

@Serializable
data class ChunkChoice(
    val index: Int = 0,
    val delta: ChunkDelta = ChunkDelta(),
    @SerialName("finish_reason") val finish_reason: String? = null
)

@Serializable
data class ChunkDelta(
    val content: String? = null,
    val role: String? = null
)

/** Non-streaming shape, used for cheap one-shot calls like intent detection. */
@Serializable
data class ChatResponse(
    val id: String? = null,
    val choices: List<ResponseChoice> = emptyList()
) {
    val text: String
        get() = choices.firstOrNull()?.message?.content.orEmpty()
}

@Serializable
data class ResponseChoice(
    val index: Int = 0,
    val message: ChatMessage
)

/** Surfaced when a gateway returns an error body instead of a stream. */
class AiException(
    val code: Int,
    val body: String
) : Exception("AI gateway returned $code: ${body.take(300)}")


/** Provider model catalogue response for OpenAI-compatible /models endpoints. */
@Serializable
data class ModelsResponse(
    val data: List<ModelInfo> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val context_length: Int? = null
)
