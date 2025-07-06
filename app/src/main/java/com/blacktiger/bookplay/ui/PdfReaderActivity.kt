package com.blacktiger.bookplay.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.blacktiger.bookplay.R
import com.blacktiger.bookplay.util.OnSwipeTouchListener
import java.io.IOException

class PdfReaderActivity : AppCompatActivity() {

    private lateinit var pdfRenderer: PdfRenderer
    private var currentPage: PdfRenderer.Page? = null
    private lateinit var parcelFileDescriptor: ParcelFileDescriptor
    private lateinit var imageView: ImageView

    private var bookUri: Uri? = null
    private var currentPageIndex = 0

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
        currentPage.closeIfInitialized()
        pdfRenderer.close()
        parcelFileDescriptor.close()
    }

    private fun PdfRenderer.Page?.closeIfInitialized() {
        this?.close()
    }
}
