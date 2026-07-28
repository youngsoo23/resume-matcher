package com.jobhunting.resumematcher.chat

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatRequest(
    val resume: String,
    val jobDescription: String,
    val messages: List<ChatMessageDto>
)

data class ChatResponse(
    val reply: String,
    val updatedResume: String?
)

data class ChatReplyResult(
    @field:JsonPropertyDescription(
        "A conversational reply to the user: explain your suggestions, ask clarifying questions, or answer their question."
    )
    val reply: String,
    @field:JsonPropertyDescription(
        "If you are proposing a resume revision this turn, the complete updated resume text in full (never a partial diff). " +
            "Omit this field if you are not proposing a change this turn."
    )
    val updatedResume: String? = null
)
