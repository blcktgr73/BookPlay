package com.blacktiger.bookplay.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.blacktiger.bookplay.R
import com.blacktiger.bookplay.util.OnSwipeTouchListener
import java.io.IOException
import java.util.Locale
import android.content.Context
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.blacktiger.bookplay.data.BookDatabase
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.launch


class PdfReaderActivity : AppCompatActivity() {

    private lateinit var pageIndicator: TextView
    private lateinit var pdfRenderer: PdfRenderer
    private var currentPage: PdfRenderer.Page? = null
    private lateinit var parcelFileDescriptor: ParcelFileDescriptor
    private lateinit var imageView: ImageView

    private var bookUri: Uri? = null
    private var currentPageIndex = 0

    // TTS 관련
    private lateinit var tts: TextToSpeech
    private var paragraphs: List<String> = listOf()
    private var paragraphIndex = 0
    private var isReading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_reader)

        imageView = findViewById(R.id.pdfImageView)
        bookUri = intent.getStringExtra("bookUri")?.let { Uri.parse(it) }
        currentPageIndex = intent.getIntExtra("bookProgress", 0)  // ← 추가
        Log.d("PdfReader", "🔄 복원할 페이지: $currentPageIndex")

        if (bookUri == null) {
            finish() // 잘못된 접근일 경우 종료
            return
        }

        pageIndicator = findViewById(R.id.pageIndicator)

        openRenderer()
        showPage(currentPageIndex)

        imageView.setOnTouchListener(object : OnSwipeTouchListener(this@PdfReaderActivity) {
            override fun onSwipeLeft() {
                showPage(currentPageIndex + 1)
            }

            override fun onSwipeRight() {
                showPage(currentPageIndex - 1)
            }
        })

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.KOREAN
                setupTtsListener()
            }
        }

        findViewById<Button>(R.id.playTtsBtn).setOnClickListener {
            val text = extractTextFromPdf(this, bookUri!!, currentPageIndex)
            startParagraphTts(text)
        }

        findViewById<Button>(R.id.stopTtsBtn).setOnClickListener {
            stopTts()
        }
    }

    private fun setupTtsListener() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                paragraphIndex++
                runOnUiThread {
                    speakNextParagraph()
                }
            }
        })
    }

    private fun startParagraphTts(text: String) {
        // 줄 단위로 자르고 이어붙이기
        val rawLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val paragraphBuilder = StringBuilder()
        val refinedParagraphs = mutableListOf<String>()

        for (line in rawLines) {
            paragraphBuilder.append(line)

            // 문장 종료 부호로 문단 종료 추정
            if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?") || paragraphBuilder.length > 300) {
                refinedParagraphs.add(paragraphBuilder.toString().trim())
                paragraphBuilder.clear()
            } else {
                paragraphBuilder.append(" ")  // 줄 간 연결
            }
        }

        // 마지막 남은 줄 처리
        if (paragraphBuilder.isNotBlank()) {
            refinedParagraphs.add(paragraphBuilder.toString().trim())
        }

        paragraphs = refinedParagraphs
        paragraphIndex = 0
        isReading = true
        speakNextParagraph()
    }


    private fun speakNextParagraph() {
        if (!isReading) return

        if (paragraphIndex < paragraphs.size) {
            val paragraph = paragraphs[paragraphIndex]
            tts.speak(paragraph, TextToSpeech.QUEUE_FLUSH, null, "paragraph_$paragraphIndex")
        } else {
            // 모든 문단 낭독 완료 시 다음 페이지로 이동
            if (currentPageIndex < pdfRenderer.pageCount - 1) {
                currentPageIndex++
                showPage(currentPageIndex)

                // 다음 페이지 텍스트 추출 및 낭독 재시작
                val nextText = extractTextFromPdf(this, bookUri!!, currentPageIndex)
                startParagraphTts(nextText)
            } else {
                // 마지막 페이지까지 완료 → 낭독 종료
                isReading = false
                runOnUiThread {
                    Toast.makeText(this, "모든 페이지 낭독을 완료했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopTts() {
        isReading = false
        if (tts.isSpeaking) tts.stop()
    }

    private fun openRenderer() {
        try {
            val fd = contentResolver.openFileDescriptor(bookUri!!, "r")
            parcelFileDescriptor = fd!!
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
        } catch (e: IOException) {
            e.printStackTrace()
            finish()
        }
    }

    private fun showPage(index: Int) {
        if (index < 0 || index >= pdfRenderer.pageCount) return

        Log.d("PdfReader", "📘 페이지 표시: $index")

        currentPage.closeIfInitialized()
        currentPageIndex = index
        currentPage = pdfRenderer.openPage(index)

        val bitmap = Bitmap.createBitmap(
            currentPage!!.width, currentPage!!.height, Bitmap.Config.ARGB_8888
        )
        currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        imageView.setImageBitmap(bitmap)

        // 페이지 수 표시
        pageIndicator.text = "${index + 1} / ${pdfRenderer.pageCount}"

        // 책 progress 저장 (코루틴 사용)
        lifecycleScope.launch {
            Log.d("PdfReader", "💾 저장 중: $index → ${bookUri.toString()}")

            val uriStr = bookUri.toString()
            val db = BookDatabase.getInstance(applicationContext)
            db.bookDao().updateProgress(uriStr, index)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // TTS 정리
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        // PDF 관련 정리
        currentPage.closeIfInitialized()
        pdfRenderer.close()
        parcelFileDescriptor.close()
    }

    private fun PdfRenderer.Page?.closeIfInitialized() {
        this?.close()
    }

    private fun extractTextFromPdf(context: Context, uri: Uri, pageIndex: Int): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val document = PDDocument.load(inputStream)

            val stripper = PDFTextStripper().apply {
                startPage = pageIndex + 1  // PdfBox는 1-based 인덱스 사용
                endPage = pageIndex + 1
            }

            val text = stripper.getText(document)
            document.close()
            text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

}
