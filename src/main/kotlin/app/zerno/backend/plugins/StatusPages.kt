package app.zerno.backend.plugins

import app.zerno.backend.model.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

/** Thrown by routes and repositories; StatusPages turns it into a JSON error with [status]. */
open class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
) : RuntimeException(message)

class NotFoundException(code: String, message: String) :
    ApiException(HttpStatusCode.NotFound, code, message)

class BadRequestException(message: String) :
    ApiException(HttpStatusCode.BadRequest, "bad_request", message)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(cause.status, ErrorResponse(cause.code, cause.message))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error on ${call.request.local.uri}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("internal_error", "Something went wrong"),
            )
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse("not_found", "No route for ${call.request.local.uri}"))
        }
    }
}
