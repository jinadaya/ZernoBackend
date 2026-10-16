import io.ktor.server.application.*
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

fun Application.configureHTTP() {
    install(ForwardedHeaders)
    install(XForwardedHeaders)
}
