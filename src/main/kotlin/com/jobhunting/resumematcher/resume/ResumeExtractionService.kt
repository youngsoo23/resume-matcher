package com.jobhunting.resumematcher.resume

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Service

sealed class ResumeExtractionException(message: String) : RuntimeException(message)
class PdfUnreadableException(message: String) : ResumeExtractionException(message)
class PdfNoTextException(message: String) : ResumeExtractionException(message)

@Service
class ResumeExtractionService {

    fun extractPdfText(bytes: ByteArray): String {
        val text = try {
            Loader.loadPDF(bytes).use { PDFTextStripper().getText(it) }
        } catch (e: Exception) {
            throw PdfUnreadableException("PDF에서 텍스트를 추출하지 못했습니다. 파일이 손상되었거나 지원하지 않는 형식일 수 있습니다.")
        }

        if (text.isBlank()) {
            throw PdfNoTextException("PDF에서 텍스트를 찾을 수 없습니다. 스캔본 이미지 PDF일 수 있습니다.")
        }

        return text.trim()
    }
}
