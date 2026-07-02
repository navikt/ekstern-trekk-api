package no.nav.trekkapi.plugin

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.trekkapi.configuration.config
import no.nav.trekkapi.innmelding.TrekkInnmeldingService
import no.nav.trekkapi.innmelding.innmeldingRoutes
import no.nav.trekkapi.innmelding.testRoutes

fun Application.configureRoutes(
    trekkInnmeldingService: TrekkInnmeldingService,
    prometheusMeterRegistry: PrometheusMeterRegistry,
) {
    routing {
        naisRoutes(prometheusMeterRegistry)
        get("/") {
            call.respondText("Ekstern-trekk-api running properly")
        }
        get("/schemas/InnrapporteringTrekk-2010-02-04.xsd") {
            val xsd =
                Application::class.java
                    .getResourceAsStream("/InnrapporteringTrekk-2010-02-04.xsd")
                    ?.readBytes()
                    ?: error("XSD resource not found")
            call.respondBytes(xsd, ContentType.Application.Xml)
        }
        get("/schemas/MsgHead-v1_2.xsd") {
            val xsd =
                Application::class.java
                    .getResourceAsStream("/MsgHead-v1_2.xsd")
                    ?.readBytes()
                    ?: error("XSD resource not found")
            call.respondBytes(xsd, ContentType.Application.Xml)
        }
        get("/schemas/AppRec-v1-2004-11-21.xsd") {
            val xsd =
                Application::class.java
                    .getResourceAsStream("/AppRec-v1-2004-11-21.xsd")
                    ?.readBytes()
                    ?: error("XSD resource not found")
            call.respondBytes(xsd, ContentType.Application.Xml)
        }
        // authenticate(MASKINPORTEN_AUTH_INNMELDING) {
        innmeldingRoutes(trekkInnmeldingService)
        // }
        if (!config().environment.isProduction()) {
            testRoutes(trekkInnmeldingService)
        }
        swaggerUI(path = "v1/swagger", swaggerFile = "openapi/documentation.yaml")
    }
}
