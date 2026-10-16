package com.royaal.rbchurch.main.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Bible(
    @SerialName("id")
    val id: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("book_count")
    val bookCount: Int,
    @SerialName("language_id")
    val languageId: String,
)
