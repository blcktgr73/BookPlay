package com.blacktiger.bookplay

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blacktiger.bookplay.adapter.BookListAdapter
import com.blacktiger.bookplay.data.Book
import com.blacktiger.bookplay.data.BookDatabase
import com.blacktiger.bookplay.ui.PdfReaderActivity
import com.blacktiger.bookplay.util.PdfUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var db: BookDatabase
    private lateinit var adapter: BookListAdapter
    private var bookList = mutableListOf<Book>()

    private val pdfPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                // ✅ 권한 영속화
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val title = getFileNameFromUri(uri)
                val thumbnailPath = PdfUtils.generateThumbnail(this, uri)
                val book = Book(uri.toString(), title, thumbnailPath)

                lifecycleScope.launch {
                    db.bookDao().insert(book)
                    loadBooks()
                }

                // ✅ PDF 리더로 이동 추가
                val intent = Intent(this, PdfReaderActivity::class.java).apply {
                    putExtra("bookUri", uri.toString())
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(intent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = BookDatabase.getInstance(this)

        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = BookListAdapter(bookList,
            onClick = { book ->
                val intent = Intent(this@MainActivity, PdfReaderActivity::class.java).apply {
                    putExtra("bookUri", book.uri)
                    putExtra("bookProgress", book.progress)  // ← 추가
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(intent)
            },
            onItemLongClick = { book ->
                // ✅ 삭제 확인 다이얼로그
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("책 삭제")
                    .setMessage("‘${book.title}’을 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        lifecycleScope.launch {
                            db.bookDao().delete(book)
                            loadBooks() // 목록 갱신
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        )
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        loadBooks()
    }

    override fun onResume() {
        super.onResume()
        loadBooks()  // ← MainActivity로 돌아올 때 DB 재로딩
    }

    private var isLoading = false

    private fun loadBooks() {
        if (isLoading) return
        isLoading = true

        val emptyView = findViewById<TextView>(R.id.emptyView)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        lifecycleScope.launch {
            bookList.clear()
            val books = db.bookDao().getAll()
                .filter { it.uri.isNotBlank() && it.title.isNotBlank() }
                .distinctBy { it.uri }

            if (books.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }

            bookList.addAll(books)
            adapter.notifyDataSetChanged()
            isLoading = false
        }
    }

    fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result ?: "Untitled"
    }
}