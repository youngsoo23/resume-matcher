package com.jobhunting.resumematcher.resume

import com.jobhunting.resumematcher.chat.ChatProvider
import com.jobhunting.resumematcher.chat.ResumeFormatRequest
import com.jobhunting.resumematcher.chat.ResumeFormatResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class ResumeController(
    private val resumeExtractionService: ResumeExtractionService,
    chatProviders: List<ChatProvider>
) {
    private val providersById = chatProviders.associateBy { it.id }

    @PostMapping("/api/resume/extract")
    fun extract(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, String>> {
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(mapOf("message" to "빈 파일입니다."))
        }

        return try {
            val text = resumeExtractionService.extractPdfText(file.bytes)
            ResponseEntity.ok(mapOf("text" to text))
        } catch (e: PdfNoTextException) {
            ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(mapOf("message" to e.message.orEmpty()))
        } catch (e: PdfUnreadableException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message.orEmpty()))
        }
    }

    @PostMapping("/api/resume/format")
    fun format(@RequestBody request: ResumeFormatRequest): ResumeFormatResponse {
        val provider = providersById[request.provider]
            ?: throw IllegalArgumentException("Unknown AI provider: ${request.provider}")
        return ResumeFormatResponse(html = provider.formatResumeHtml(request.resume))
    }
}
