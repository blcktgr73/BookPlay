package com.blacktiger.bookplay.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.blacktiger.bookplay.R
import com.blacktiger.bookplay.data.Book
import com.bumptech.glide.Glide
import java.io.File

class BookListAdapter(
    private val books: List<Book>,
    private val onClick: (Book) -> Unit
) : RecyclerView.Adapter<BookListAdapter.BookViewHolder>() {

    inner class BookViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val image = view.findViewById<ImageView>(R.id.bookThumbnail)
        val progress = view.findViewById<ProgressBar>(R.id.bookProgress)
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
        holder.view.setOnClickListener { onClick(book) }
    }
}