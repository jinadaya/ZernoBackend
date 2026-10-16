package com.royaal.rbchurch.main.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Language(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("local_name")
    val localName: String
)