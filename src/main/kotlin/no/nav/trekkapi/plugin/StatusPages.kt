package no.nav.trekkapi.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import no.nav.trekkapi.api.ErrorResponseDto
import no.nav.trekkapi.log

class UnauthorizedException(
    message: String = "Unauthorized",
) : Exception(message)

class ForbiddenException(
    message: String = "Forbidden",
) : Exception(message)

class ValidationException(
    message: String,
    throwable: Throwable,
) : Exception(message, throwable)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<UnauthorizedException> { call, cause ->
            log.error("Unauthorized: ${cause.message}")
            call.respond(HttpStatusCode.Unauthorized, ErrorResponseDto(cause.message ?: "Unauthorized"))
        }
        exception<ForbiddenException> { call, cause ->
            log.error("Forbidden: ${cause.message}")
            call.respond(HttpStatusCode.Forbidden, ErrorResponseDto(cause.message ?: "Forbidden"))
        }
        exception<ValidationException> { call, cause ->
            log.error("Validation error: ${cause.message}")
            call.respond(HttpStatusCode.UnprocessableEntity, ErrorResponseDto(cause.message ?: "Validation error"))
        }
        exception<BadRequestException> { call, cause ->
            log.error("Bad request: ${cause.message}")
            call.respond(HttpStatusCode.BadRequest, ErrorResponseDto(cause.message ?: "Bad request"))
        }
        exception<NotFoundException> { call, cause ->
            log.error("Not found: ${cause.message}")
            call.respond(HttpStatusCode.NotFound, ErrorResponseDto(cause.message ?: "Not found"))
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponseDto("Unexpected error"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponseDto("Not found"))
        }
    }
}
