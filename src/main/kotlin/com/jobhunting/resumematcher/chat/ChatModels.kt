package com.jobhunting.resumematcher.chat

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatRequest(
    val resume: String,
    val jobDescription: String,
    val messages: List<ChatMessageDto>,
    val provider: String = "claude"
)

data class ChatResponse(
    val reply: String,
    val updatedResume: String?
)

data class ResumeFormatRequest(
    val resume: String,
    val provider: String = "claude"
)

data class ResumeFormatResponse(
    val html: String
)

data class ResumeHtmlResult(
    @field:JsonPropertyDescription(
        "The resume re-rendered as a single HTML fragment (no <html>/<head>/<body> tags), using exactly the class " +
            "names described in the prompt. Do not invent content that isn't in the source resume; omit any section " +
            "the source doesn't have."
    )
    val html: String
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
