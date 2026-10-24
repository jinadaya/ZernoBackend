package app.zerno.backend.domain.translation

import app.zerno.backend.domain.meta.model.BibleMeta
import io.ktor.http.content.*
import java.io.InputStream

/** Where translation files are kept: the local `databases/bibles` folder now, S3-compatible storage later. */
interface TranslationStorage {

    /** The file of [meta], or null when this server doesn't hold it. */
    fun find(meta: BibleMeta): StoredFile?
}

interface StoredFile {
    val size: Long

    /** Milliseconds since the epoch; with [size] it tells whether a cached checksum still holds. */
    val modifiedAt: Long

    fun open(): InputStream

    /** The download body. It must be a content the `PartialContent` plugin can serve ranges of. */
    fun content(): OutgoingContent
}
