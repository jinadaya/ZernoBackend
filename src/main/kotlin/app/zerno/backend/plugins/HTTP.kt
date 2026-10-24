package app.zerno.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.forwardedheaders.*

fun Application.configureHTTP() {
    install(ForwardedHeaders)
    install(XForwardedHeaders)
    // Application-wide because PartialContent only checks `If-Range` against versions when it finds it here.
    install(ConditionalHeaders)
}
