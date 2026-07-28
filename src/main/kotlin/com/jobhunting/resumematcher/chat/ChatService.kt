package com.jobhunting.resumematcher.chat

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ChatService(
    @Value("\${anthropic.api-key}") apiKey: String,
    @Value("\${anthropic.model:claude-opus-5}") private val model: String
) {
    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    fun chat(request: ChatRequest): ChatResponse {
        val builder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(4096L)
            .system(buildSystemPrompt(request.resume, request.jobDescription))
            .outputConfig(ChatReplyResult::class.java)

        request.messages.forEach { message ->
            if (message.role == "assistant") {
                builder.addAssistantMessage(message.content)
            } else {
                builder.addUserMessage(message.content)
            }
        }

        val response = client.messages().create(builder.build())

        val result = response.content()
            .asSequence()
            .mapNotNull { it.text().orElse(null) }
            .firstOrNull()
            ?.text()
            ?: throw IllegalStateException("Claude returned no structured content")

        return ChatResponse(reply = result.reply, updatedResume = result.updatedResume)
    }

    private fun buildSystemPrompt(resume: String, jobDescription: String): String = """
        You are an expert resume coach helping a candidate tailor their resume to a specific job posting.
        Ground every suggestion in the resume and job description below, and in the conversation history.
        When you propose a resume revision, return the COMPLETE updated resume text in `updatedResume` —
        never a partial diff or excerpt. If you are only discussing or answering a question without
        proposing a change, omit `updatedResume`.

        ## Current resume
        $resume

        ## Job description
        $jobDescription
    """.trimIndent()
}
