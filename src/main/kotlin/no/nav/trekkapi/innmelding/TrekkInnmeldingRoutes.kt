package no.nav.trekkapi.innmelding

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import no.nav.trekkapi.auth.orgNrFromTokenValidationContext
import no.nav.trekkapi.log
import no.nav.trekkapi.plugin.UnauthorizedException
import java.io.InputStream
import kotlin.use

// todo lag test for denne, hvis vi får til maskinporten mock

fun Route.innmeldingRoutes(
    trekkInnmeldingService: TrekkInnmeldingService
) {
    post("/v1/innrapportering") {
        log.debug("Innrapportering kalt")
        val orgnr = orgNrFromTokenValidationContext() ?: throw UnauthorizedException()

        val id = trekkInnmeldingService.register(orgnr, call.receiveText())
        log.info("Videresendt trekkopplysningsmelding for orgnr: $orgnr, med ny id: $id")
        call.response.header(HttpHeaders.Location, "/v1/innrapportering/$id")
        call.respond(HttpStatusCode.Accepted)
    }

    // Alternativ 2: PUT hvor ID genereres av kaller
    put("/v1/innrapportering/{id}") {
        val id = call.pathParameters["id"]!!
        log.debug("Innrapportering kalt med id: $id")
        val orgnr = orgNrFromTokenValidationContext() ?: throw UnauthorizedException()

        if (trekkInnmeldingService.alreadyRegistered(orgnr, id)) {
            log.info("Trekkopplysningsmelding allerede registrert for orgnr: $orgnr, id: $id")
            call.response.header(HttpHeaders.Location, "/v1/innrapportering/$id")
            call.respond(HttpStatusCode.Accepted)
            return@put
        }

        trekkInnmeldingService.register(orgnr, id, call.receiveText())
        log.info("Videresendt trekkopplysningsmelding for orgnr: $orgnr, id: $id")
        call.response.header(HttpHeaders.Location, "/v1/innrapportering/$id")
        call.respond(HttpStatusCode.Accepted)
    }

    get("/v1/innrapportering/{id}") {
        val id = call.pathParameters["id"]!!
        log.debug("Hent innrapporteringstatus kalt med id: $id")
        val orgnr = orgNrFromTokenValidationContext() ?: throw UnauthorizedException()

        val status = trekkInnmeldingService.getStatus(orgnr, id)
            ?: throw NotFoundException("Finnes ingen status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
        log.info("Returnerer status $status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
        call.respond(HttpStatusCode.OK, status)
    }
}

fun Route.testRoutes(
    trekkInnmeldingService: TrekkInnmeldingService
) {
    // TESTVERSJON av PUT hvor ID genereres av kaller, ingen auth, og body leses fra testfil
    get("/test/putinnrapportering/{id}") {
        val id = call.pathParameters["id"]!!
        log.debug("TEST-Innrapportering kalt med id: $id")

        val orgnr = "123456789"
        // Vil bli lagret i DB med ID "orgnr-id",
        // og lagt på MQ med ediLoggId (og messageId, convId) = "trekkapi-orgnr-id"

        if (trekkInnmeldingService.alreadyRegistered(orgnr, id)) {
            log.info("Trekkopplysningsmelding allerede registrert for orgnr: $orgnr, id: $id")
            call.response.header(HttpHeaders.Location, "/v1/innrapportering/$id")
            call.respond(HttpStatusCode.Accepted)
            return@get
        }

        val inputStream: InputStream? = this::class.java.getResourceAsStream("/testbody.xml")
        val body = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        trekkInnmeldingService.register(orgnr, id, body)
        log.info("Videresendt trekkopplysningsmelding for orgnr: $orgnr, id: $id")
        call.response.header(HttpHeaders.Location, "/test/innrapportering/$id")
        call.respond(HttpStatusCode.Accepted)
    }

    get("/test/innrapportering/{id}") {
        val id = call.pathParameters["id"]!!
        log.debug("TEST Hent innrapporteringstatus kalt med id: $id")

        val orgnr = "123456789"
        val status = trekkInnmeldingService.getStatus(orgnr, id)
        if (status != null) {
            log.info("Returnerer status $status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
            call.respond(HttpStatusCode.OK, status)
        } else {
            log.warn("Finnes ingen status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/testMq") {
        log.info("Testing MQ......")
        trekkInnmeldingService.verifyConnection()
        log.info("MQ connection OK")
        call.respondText("MQ connection OK")
    }
}
