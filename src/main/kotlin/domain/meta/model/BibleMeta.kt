package domain.meta.model

data class BibleMeta(
    val id: String,
    val displayName: String,
    val languageId: String,
    val dbType: BibleDbType,
    val dbPathOrUrl: String,
    val dbUser: String? = null,
    val dbPassword: String? = null,
) {
    // In case there will be some others databases (hope not)
    enum class BibleDbType {
        SQLITE,
    }
}