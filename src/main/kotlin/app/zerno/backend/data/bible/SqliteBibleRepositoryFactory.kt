package app.zerno.backend.data.bible

import app.zerno.backend.data.sql.SqliteDatabases
import app.zerno.backend.domain.bible.BibleRepository
import app.zerno.backend.domain.meta.model.BibleMeta
import app.zerno.backend.plugins.NotFoundException
import java.io.File

/** Opens `<biblesDir>/<languageId>/<dbPath>`, the layout of the `databases/bibles` folder. */
class SqliteBibleRepositoryFactory(
    private val biblesDir: File,
) : BibleRepository.Factory {

    override fun create(meta: BibleMeta): BibleRepository {
        val file = File(File(biblesDir, meta.languageId), meta.dbPath)
        if (!file.isFile) {
            throw NotFoundException("bible_file_missing", "Bible '${meta.id}' has no file on this server")
        }
        return SqliteBibleRepository(SqliteDatabases.openReadOnly(file), meta.id)
    }
}
