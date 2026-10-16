package mainApp

import com.royaal.rbchurch.main.domain.bible.BibleRepository
import com.royaal.rbchurch.main.domain.meta.MetaRepository

class AppContext(
    val metaRepository: MetaRepository,
    val bibleRepositoryFactory: BibleRepository.Factory,
)