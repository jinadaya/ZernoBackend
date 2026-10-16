package model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChapterInfo(
    @SerialName("id")
    val id: String,
    @SerialName("order")
    val order: Int,
    @SerialName("verseCount")
    val verseCount: Int,
)