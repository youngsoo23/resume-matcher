package com.jobhunting.resumematcher.chat

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.ThinkingConfigDisabled
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ClaudeChatProvider(
    @Value("\${anthropic.api-key}") apiKey: String,
    @Value("\${anthropic.model:claude-opus-5}") private val model: String
) : ChatProvider {

    override val id = "claude"

    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    override fun chat(request: ChatRequest): ChatResponse {
        val builder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(16000L)
            .system(ChatPrompts.systemPrompt(request.resume, request.jobDescription))
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

    override fun formatResumeHtml(resume: String): String {
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(16000L)
            .outputConfig(ResumeHtmlResult::class.java)
            .thinking(ThinkingConfigDisabled.builder().build())
            .addUserMessage(ChatPrompts.resumeHtmlFormatPrompt(resume))
            .build()

        val response = client.messages().create(params)

        val result = response.content()
            .asSequence()
            .mapNotNull { it.text().orElse(null) }
            .firstOrNull()
            ?.text()
            ?: throw IllegalStateException("Claude returned no structured content")

        return result.html
    }
}
