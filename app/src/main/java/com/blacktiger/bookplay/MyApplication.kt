package com.blacktiger.bookplay

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // PdfBox 내부 리소스 초기화 (glyphlist 등)
        PDFBoxResourceLoader.init(applicationContext)
    }
}
