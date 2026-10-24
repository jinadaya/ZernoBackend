package app.zerno.backend.domain.meta.model

data class BibleMeta(
    val id: String,
    val displayName: String,
    val languageId: String,
    val dbType: BibleDbType,
    val dbPath: String,
    val translationCode: String = id.uppercase(),
    val licence: String = "",
    val attribution: String = "",
    /** Version of the file layout the app reads. */
    val schemaVersion: Int = 1,
    /** The app ships this file, so it never has to download it. */
    val bundled: Boolean = false,
    /** False until the licence allows handing the file out; see `TranslationCatalog`. */
    val licensed: Boolean = true,
) {
    enum class BibleDbType {
        SQLITE,
    }
}
