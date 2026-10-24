package app.zerno.backend.routes

import app.zerno.backend.domain.translation.TranslationCatalog
import app.zerno.backend.plugins.NotFoundException
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import org.koin.ktor.ext.inject

/**
 * `GET /v1/translations`, the download catalog, and `GET /v1/translations/{id}/file`, the file itself. The file's
 * ETag is its SHA-256, so `If-None-Match` answers 304 and `If-Range` keeps a resumed download from mixing two files.
 */
fun Route.translationRoutes() {
    val catalog by inject<TranslationCatalog>()

    get("/v1/translations") { call.respondDocument(catalog.document()) }

    route("/v1/translations/{id}/file") {
        install(PartialContent)
        get {
            val id = call.parameters.getOrFail("id")
            val entry = catalog.entry(id)
                ?: throw NotFoundException("translation_not_found", "No downloadable translation '$id'")
            val content = entry.file.content()
            // Only the checksum: PartialContent wants every version to match `If-Range`, and that header holds one.
            content.versions = listOf(EntityTagVersion("\"${entry.sha256}\"", weak = false))
            call.response.header(HttpHeaders.CacheControl, "no-cache")
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "$id.SQLite3")
                    .toString(),
            )
            call.respond(content)
        }
    }
}
