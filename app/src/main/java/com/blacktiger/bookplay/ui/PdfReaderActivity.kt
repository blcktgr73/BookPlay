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
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper


class PdfReaderActivity : AppCompatActivity() {

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

        if (bookUri == null) {
            finish() // 잘못된 접근일 경우 종료
            return
        }

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
        paragraphs = text.split(Regex("\n{2,}|\r\n\r\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        paragraphIndex = 0
        isReading = true
        speakNextParagraph()
    }

    private fun speakNextParagraph() {
        if (!isReading || paragraphIndex >= paragraphs.size) return

        val paragraph = paragraphs[paragraphIndex]
        tts.speak(paragraph, TextToSpeech.QUEUE_FLUSH, null, "paragraph_$paragraphIndex")
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

        currentPage.closeIfInitialized()

        currentPageIndex = index
        currentPage = pdfRenderer.openPage(index)

        val bitmap = Bitmap.createBitmap(
            currentPage!!.width, currentPage!!.height, Bitmap.Config.ARGB_8888
        )
        currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        imageView.setImageBitmap(bitmap)

        // TODO: 책 progress 저장 (DB 연동 시 적용)
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
