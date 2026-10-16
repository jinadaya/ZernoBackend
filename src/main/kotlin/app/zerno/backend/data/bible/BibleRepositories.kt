package app.zerno.backend.data.bible

import app.zerno.backend.domain.bible.BibleRepository
import app.zerno.backend.domain.meta.MetaRepository
import app.zerno.backend.plugins.NotFoundException
import java.util.concurrent.ConcurrentHashMap

/** Looks a Bible up in the meta database and keeps one open repository per Bible. */
class BibleRepositories(
    private val metaRepository: MetaRepository,
    private val factory: BibleRepository.Factory,
) {
    private val cache = ConcurrentHashMap<String, BibleRepository>()

    suspend fun get(bibleId: String): BibleRepository {
        cache[bibleId]?.let { return it }
        val meta = metaRepository.getBibleMeta(bibleId)
            ?: throw NotFoundException("bible_not_found", "Unknown bible '$bibleId'")
        return cache.computeIfAbsent(bibleId) { factory.create(meta) }
    }
}
