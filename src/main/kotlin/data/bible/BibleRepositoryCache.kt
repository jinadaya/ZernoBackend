package data.bible

import com.royaal.rbchurch.main.domain.bible.BibleRepository
import domain.meta.model.BibleMeta
import java.util.concurrent.ConcurrentHashMap

object BibleRepositoryCache {

    private val cache =
        ConcurrentHashMap<String, BibleRepository>()

    fun getOrCreate(
        meta: BibleMeta,
        factory: BibleRepository.Factory
    ): BibleRepository =
        cache.computeIfAbsent(meta.id) {
            factory.create(meta)
        }

    fun clear(bibleId: String) {
        cache.remove(bibleId)
    }

    fun clearAll() {
        cache.clear()
    }
}