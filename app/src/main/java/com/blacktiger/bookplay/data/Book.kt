package com.blacktiger.bookplay.data

import androidx.room.Entity
import androidx.room.PrimaryKey  // ← 이거 누락됨


@Entity(tableName = "books")
data class Book(
    @PrimaryKey val uri: String,
    val title: String,
    val thumbnailPath: String,
    val progress: Int = 0
)

