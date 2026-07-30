package com.jobhunting.resumematcher.chat

interface ChatProvider {
    val id: String
    fun chat(request: ChatRequest): ChatResponse
    fun formatResumeHtml(resume: String): String
}
