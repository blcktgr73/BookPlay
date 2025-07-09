package com.blacktiger.bookplay.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blacktiger.bookplay.R
import com.blacktiger.bookplay.data.Book
import com.bumptech.glide.Glide
import java.io.File

class BookListAdapter(
    private val books: List<Book>,
    private val onClick: (Book) -> Unit,
    private val onItemLongClick: (Book) -> Unit  // 롱클릭 콜백 추가
) : RecyclerView.Adapter<BookListAdapter.BookViewHolder>() {

    inner class BookViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val image = view.findViewById<ImageView>(R.id.bookThumbnail)
        val progress = view.findViewById<ProgressBar>(R.id.bookProgress)
        val title = view.findViewById<TextView>(R.id.bookTitle) // 🔹 추가
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun getItemCount() = books.size

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        Glide.with(holder.view).load(File(book.thumbnailPath)).into(holder.image)
        holder.progress.progress = book.progress
        holder.title.text = book.title // 🔹 제목 설정

        // 클릭 이벤트
        holder.view.setOnClickListener { onClick(book) }

        // ✅ 롱클릭 이벤트 추가
        holder.view.setOnLongClickListener {
            onItemLongClick(book)
            true  // 이벤트 소비했음을 명시
        }
    }
}