package com.blacktiger.bookplay

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

                val title = uri.lastPathSegment ?: "Untitled"
                val thumbnailPath = PdfUtils.generateThumbnail(this, uri)
                val book = Book(uri.toString(), title, thumbnailPath)

                lifecycleScope.launch {
                    db.bookDao().insert(book)
                    loadBooks()
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = BookDatabase.getInstance(this)

        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = BookListAdapter(bookList) { book ->
            val intent = Intent(this@MainActivity, PdfReaderActivity::class.java).apply {
                putExtra("bookUri", book.uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        }
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            pdfPicker.launch(arrayOf("application/pdf"))
        }

        loadBooks()
    }

    private fun loadBooks() {
        lifecycleScope.launch {
            bookList.clear()
            bookList.addAll(db.bookDao().getAll())
            adapter.notifyDataSetChanged()
        }
    }
}
