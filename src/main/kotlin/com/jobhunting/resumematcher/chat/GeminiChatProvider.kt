package com.jobhunting.resumematcher.chat

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.google.genai.types.Schema
import com.google.genai.types.Type
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class GeminiChatProvider(
    @Value("\${gemini.api-key}") apiKey: String,
    @Value("\${gemini.model:gemini-2.5-flash}") private val model: String
) : ChatProvider {

    override val id = "gemini"

    private val client = Client.builder().apiKey(apiKey).build()
    private val objectMapper = ObjectMapper()

    private val responseSchema: Schema = Schema.builder()
        .type(Type.Known.OBJECT)
        .properties(
            mapOf(
                "reply" to Schema.builder().type(Type.Known.STRING).build(),
                "updatedResume" to Schema.builder().type(Type.Known.STRING).build()
            )
        )
        .required("reply")
        .build()

    private val resumeHtmlSchema: Schema = Schema.builder()
        .type(Type.Known.OBJECT)
        .properties(mapOf("html" to Schema.builder().type(Type.Known.STRING).build()))
        .required("html")
        .build()

    override fun chat(request: ChatRequest): ChatResponse {
        val systemInstruction = Content.fromParts(
            Part.fromText(ChatPrompts.systemPrompt(request.resume, request.jobDescription))
        )

        val config = GenerateContentConfig.builder()
            .systemInstruction(systemInstruction)
            .responseMimeType("application/json")
            .responseSchema(responseSchema)
            .build()

        val contents = request.messages.map { message ->
            val role = if (message.role == "assistant") "model" else "user"
            Content.builder().role(role).parts(Part.fromText(message.content)).build()
        }

        val response = client.models.generateContent(model, contents, config)
        val json = response.text() ?: throw IllegalStateException("Gemini returned no content")
        val result = objectMapper.readValue(json, ChatReplyResult::class.java)

        return ChatResponse(reply = result.reply, updatedResume = result.updatedResume)
    }

    override fun formatResumeHtml(resume: String): String {
        val config = GenerateContentConfig.builder()
            .responseMimeType("application/json")
            .responseSchema(resumeHtmlSchema)
            .build()

        val contents = listOf(
            Content.builder().role("user").parts(Part.fromText(ChatPrompts.resumeHtmlFormatPrompt(resume))).build()
        )

        val response = client.models.generateContent(model, contents, config)
        val json = response.text() ?: throw IllegalStateException("Gemini returned no content")

        return objectMapper.readValue(json, ResumeHtmlResult::class.java).html
    }
}
