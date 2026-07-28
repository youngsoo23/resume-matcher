package com.jobhunting.resumematcher.resume

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class ResumeController(private val resumeExtractionService: ResumeExtractionService) {

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
}
