package app.zerno.backend.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Language(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("local_name") val localName: String,
)

@Serializable
data class Bible(
    @SerialName("id") val id: String,
    @SerialName("translation_code") val translationCode: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("book_count") val bookCount: Int,
    @SerialName("language_id") val languageId: String,
    @SerialName("licence") val licence: String,
    @SerialName("attribution") val attribution: String,
)

@Serializable
data class BibleBooksResponse(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("books") val books: List<BookInfo>,
)

/** [id] is the MyBible `book_number` (10 = Genesis), the same number [Verse.book] carries. */
@Serializable
data class BookInfo(
    @SerialName("id") val id: String,
    @SerialName("order") val order: Int,
    @SerialName("chapter_count") val chapterCount: Int,
    @SerialName("display_name") val displayName: String,
)

@Serializable
data class ChapterInfo(
    @SerialName("id") val id: String,
    @SerialName("order") val order: Int,
    @SerialName("verse_count") val verseCount: Int,
)

@Serializable
data class Verse(
    @SerialName("book") val book: Int,
    @SerialName("chapter") val chapter: Int,
    @SerialName("order") val order: Int,
    @SerialName("text") val text: String,
)

@Serializable
data class VersesPage(
    @SerialName("offset") val offset: Int,
    @SerialName("limit") val limit: Int,
    @SerialName("total") val total: Int,
    @SerialName("verses") val verses: List<Verse>,
)

@Serializable
data class ErrorResponse(
    @SerialName("error") val error: String,
    @SerialName("message") val message: String,
)
