package com.jobhunting.resumematcher.chat

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChatController(providers: List<ChatProvider>) {

    private val providersById = providers.associateBy { it.id }

    @PostMapping("/api/chat")
    fun chat(@RequestBody request: ChatRequest): ChatResponse {
        val provider = providersById[request.provider]
            ?: throw IllegalArgumentException("Unknown AI provider: ${request.provider}")
        return provider.chat(request)
    }
}
