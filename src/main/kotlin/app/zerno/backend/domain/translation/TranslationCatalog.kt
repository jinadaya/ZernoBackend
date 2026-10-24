package app.zerno.backend.domain.translation

import app.zerno.backend.content.ContentCatalog
import app.zerno.backend.domain.meta.MetaRepository
import app.zerno.backend.domain.meta.model.BibleMeta
import app.zerno.backend.model.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * The translations the app may download: every Bible in the meta database whose file this server holds, except
 * the ones still waiting for a licence (`licensed = 0`) unless [showUnlicensed] is on. Checksums are worked out on
 * first use and again whenever a file's size or date changes, so replacing a file needs no restart.
 */
class TranslationCatalog(
    private val meta: MetaRepository,
    private val storage: TranslationStorage,
    private val showUnlicensed: Boolean,
) {
    class Entry(val meta: BibleMeta, val file: StoredFile, val sha256: String)

    private class Checksum(val size: Long, val modifiedAt: Long, val sha256: String)

    private val checksums = ConcurrentHashMap<String, Checksum>()

    suspend fun entries(): List<Entry> = meta.getAllBibleMeta().mapNotNull { entryOf(it) }

    suspend fun entry(id: String): Entry? = meta.getBibleMeta(id)?.let { entryOf(it) }

    /** The `/v1/translations` body, versioned like the other `/v1` documents. */
    suspend fun document(): ContentCatalog.Document {
        val translations = entries().map { it.toTranslation() }
        return ContentCatalog.Document.of("translations", Json.encodeToJsonElement(translations).jsonArray)
    }

    private suspend fun entryOf(bible: BibleMeta): Entry? {
        if (!bible.licensed && !showUnlicensed) return null
        val file = storage.find(bible) ?: return null
        return Entry(bible, file, checksum(bible.id, file))
    }

    private suspend fun checksum(id: String, file: StoredFile): String {
        val size = file.size
        val modifiedAt = file.modifiedAt
        checksums[id]?.takeIf { it.size == size && it.modifiedAt == modifiedAt }?.let { return it.sha256 }
        val sha256 = withContext(Dispatchers.IO) { sha256(file) }
        checksums[id] = Checksum(size, modifiedAt, sha256)
        return sha256
    }

    companion object {
        const val FILE_VERSION_LENGTH = 16

        fun sha256(file: StoredFile): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.open().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        fun Entry.toTranslation() = Translation(
            id = meta.id,
            translationCode = meta.translationCode,
            displayName = meta.displayName,
            languageId = meta.languageId,
            licence = meta.licence,
            attribution = meta.attribution,
            schemaVersion = meta.schemaVersion,
            fileVersion = sha256.take(FILE_VERSION_LENGTH),
            size = file.size,
            sha256 = sha256,
            downloadUrl = "v1/translations/${meta.id}/file",
            bundled = meta.bundled,
        )
    }
}
