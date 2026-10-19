package app.zerno.backend.domain.meta.model

data class BibleMeta(
    val id: String,
    val displayName: String,
    val languageId: String,
    val dbType: BibleDbType,
    val dbPath: String,
) {
    enum class BibleDbType {
        SQLITE,
    }
}
