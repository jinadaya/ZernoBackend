package com.royaal.rbchurch.main.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleBooksResponse(
    @SerialName("id")
    val id: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("books")
    val books: List<BookInfo>,
)