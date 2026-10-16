package com.royaal.rbchurch.main.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    @SerialName("id")
    val id: String,
    @SerialName("order")
    val order: Int,
    @SerialName("verses")
    val verses: List<Verse>,
)
