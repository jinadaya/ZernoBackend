package com.royaal.rbchurch.main.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Verse(
    @SerialName("book")
    val book: Int,
    @SerialName("chapter")
    val chapter: Int,
    @SerialName("order")
    val order: Int,
    @SerialName("text")
    val text: String,
)