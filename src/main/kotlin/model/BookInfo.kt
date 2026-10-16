package com.royaal.rbchurch.main.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookInfo(
    @SerialName("id")
    val id: String,
    @SerialName("order")
    val order: Int,
    @SerialName("chapter_count")
    val chapterCount: Int,
    @SerialName("display_name")
    val displayName: String,
)