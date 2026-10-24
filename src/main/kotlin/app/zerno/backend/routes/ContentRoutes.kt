package app.zerno.backend.routes

import app.zerno.backend.content.ContentCatalog
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/** `GET /v1/plans` and `GET /v1/achievements`; a matching `If-None-Match` gets 304 with no body. */
fun Route.contentRoutes() {
    val content by inject<ContentCatalog>()

    get("/v1/plans") { call.respondDocument(content.plans) }
    get("/v1/achievements") { call.respondDocument(content.achievements) }
}

internal suspend fun ApplicationCall.respondDocument(document: ContentCatalog.Document) {
    val etag = "\"${document.version}\""
    response.header(HttpHeaders.ETag, etag)
    // The app may keep a copy but asks every time, so a content change shows up on the next refresh.
    response.header(HttpHeaders.CacheControl, "no-cache")
    val known = request.headers[HttpHeaders.IfNoneMatch]?.split(",")?.map { it.trim() }.orEmpty()
    if (etag in known) {
        respond(HttpStatusCode.NotModified)
    } else {
        respond(document.body)
    }
}
