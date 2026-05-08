package no.nav.trekkapi.innmelding

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.trekkapi.auth.orgNrFromTokenValidationContext
import no.nav.trekkapi.fellesformat.unmarshal
import no.nav.trekkapi.log
import no.nav.trekkapi.plugin.UnauthorizedException
import no.nav.trekkapi.plugin.ValidationException
import java.io.InputStream
import kotlin.use

// todo lag test for denne, hvis vi får til maskinporten mock

fun Route.innmeldingRoutes(
    trekkInnmeldingService: TrekkInnmeldingService
) {
    post("/v1/innrapportering") {
        log.debug("Innrapportering kalt")
        val orgnr = orgNrFromTokenValidationContext() ?: throw UnauthorizedException()

        val innmeldingXml = call.receiveText()
        innmeldingXml.validateInnmeldingXML()
        val id = trekkInnmeldingService.register(orgnr, innmeldingXml)
        log.info("Videresendt trekkopplysningsmelding for orgnr: $orgnr, med ny id: $id")
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

private fun String.validateInnmeldingXML() =
    runCatching { unmarshal(this, MsgHead::class.java) }
        .onFailure { throw ValidationException("Ugyldig XML-format", it) }

fun Route.testRoutes(
    trekkInnmeldingService: TrekkInnmeldingService
) {
    // TESTVERSJON av POST hvor ID genereres, ingen auth, og body leses fra testfil
    get("/test/putinnrapportering") {
        log.debug("TEST-Innrapportering kalt")

        val orgnr = "123456789"
        // Vil bli lagret i DB og lagt på MQ

        val inputStream: InputStream? = this::class.java.getResourceAsStream("/testbody.xml")
        val body = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

        val id = trekkInnmeldingService.register(orgnr, body)
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
