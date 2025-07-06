package com.blacktiger.bookplay.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfUtils {
    fun generateThumbnail(context: Context, uri: Uri): String {
        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
        val renderer = PdfRenderer(ParcelFileDescriptor.dup(fileDescriptor!!))
        val page = renderer.openPage(0)

        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        val thumbFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
        FileOutputStream(thumbFile).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
        }

        page.close()
        renderer.close()
        return thumbFile.absolutePath
    }
}
