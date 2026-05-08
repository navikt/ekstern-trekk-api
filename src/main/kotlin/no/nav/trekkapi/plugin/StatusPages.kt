package no.nav.trekkapi.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import no.nav.trekkapi.innmelding.ErrorResponse
import no.nav.trekkapi.log

class UnauthorizedException(message: String = "Autorisasjonsfeil") : Exception(message)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<UnauthorizedException> { call, cause ->
            log.error("Unauthorized: ${cause.message}")
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", cause.message ?: "Autorisasjonsfeil"))
        }
        exception<BadRequestException> { call, cause ->
            log.error("Bad request: ${cause.message}")
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", cause.message ?: "Bad request"))
        }
        exception<NotFoundException> { call, cause ->
            log.error("Not found: ${cause.message}")
            call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", cause.message ?: "Not found"))
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("INTERNAL_SERVER_ERROR", "Unexpected error"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse("NOT_FOUND", "Not found"))
        }
    }
}
