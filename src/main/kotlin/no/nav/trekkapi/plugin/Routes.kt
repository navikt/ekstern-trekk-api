package no.nav.trekkapi.plugin

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.emottak.utils.environment.isProdEnv
import no.nav.trekkapi.auth.MASKINPORTEN_AUTH_INMMELDING
import no.nav.trekkapi.innmelding.TrekkInnmeldingService
import no.nav.trekkapi.innmelding.innmeldingRoutes
import no.nav.trekkapi.innmelding.testRoutes

fun Application.configureRoutes(
    trekkInnmeldingService: TrekkInnmeldingService,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    routing {
        naisRoutes(prometheusMeterRegistry)
        get("/") {
            call.respondText("Ekstern-trekk-api running properly")
        }
        authenticate(MASKINPORTEN_AUTH_INMMELDING) {
            innmeldingRoutes(trekkInnmeldingService)
        }
        if (!isProdEnv()) {
            testRoutes(trekkInnmeldingService)
        }
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
}
