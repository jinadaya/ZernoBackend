package app.zerno.backend.data.translation

import app.zerno.backend.domain.meta.model.BibleMeta
import app.zerno.backend.domain.translation.StoredFile
import app.zerno.backend.domain.translation.TranslationStorage
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.http.content.*
import java.io.File
import java.io.InputStream

/** Reads `<biblesDir>/<languageId>/<dbPath>`, the same file `SqliteBibleRepositoryFactory` opens. */
class LocalTranslationStorage(private val biblesDir: File) : TranslationStorage {

    override fun find(meta: BibleMeta): StoredFile? =
        File(File(biblesDir, meta.languageId), meta.dbPath).takeIf { it.isFile }?.let(::LocalFile)

    private class LocalFile(private val file: File) : StoredFile {
        override val size: Long get() = file.length()
        override val modifiedAt: Long get() = file.lastModified()
        override fun open(): InputStream = file.inputStream()
        override fun content(): OutgoingContent = LocalFileContent(file, SqliteContentType)
    }

    private companion object {
        val SqliteContentType = ContentType("application", "vnd.sqlite3")
    }
}
