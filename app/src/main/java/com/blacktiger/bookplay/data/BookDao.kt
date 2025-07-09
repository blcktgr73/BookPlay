package com.blacktiger.bookplay.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy  // ← onConflict 쓰면 필요

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    suspend fun getAll(): List<Book>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("UPDATE books SET progress = :progress WHERE uri = :uri")
    suspend fun updateProgress(uri: String, progress: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM books WHERE uri = :uri)")
    suspend fun exists(uri: String): Boolean
}
