package app.zerno.backend

import io.ktor.server.config.*
import java.io.File

data class BibleConfig(
    val metaDbFile: File,
    val biblesDir: File,
    /** Plans and achievements served under `/v1`, see `ContentCatalog`. */
    val contentDir: File,
    /** Lists translations whose licence isn't signed yet (`licensed = 0`), for development only. */
    val showUnlicensed: Boolean = false,
) {
    companion object {
        fun from(config: ApplicationConfig) = BibleConfig(
            metaDbFile = File(config.property("bible.metaDbPath").getString()),
            biblesDir = File(config.property("bible.biblesDir").getString()),
            contentDir = File(config.property("content.dir").getString()),
            showUnlicensed = config.propertyOrNull("translations.showUnlicensed")?.getString().toBoolean(),
        )
    }
}
