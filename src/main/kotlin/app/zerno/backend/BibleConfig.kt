package app.zerno.backend

import io.ktor.server.config.*
import java.io.File

data class BibleConfig(
    val metaDbFile: File,
    val biblesDir: File,
) {
    companion object {
        fun from(config: ApplicationConfig) = BibleConfig(
            metaDbFile = File(config.property("bible.metaDbPath").getString()),
            biblesDir = File(config.property("bible.biblesDir").getString()),
        )
    }
}
