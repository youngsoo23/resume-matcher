package com.jobhunting.resumematcher.chat

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChatController(private val chatService: ChatService) {

    @PostMapping("/api/chat")
    fun chat(@RequestBody request: ChatRequest): ChatResponse = chatService.chat(request)
}
