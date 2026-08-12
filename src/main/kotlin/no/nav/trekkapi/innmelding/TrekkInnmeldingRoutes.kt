package no.nav.trekkapi.innmelding

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.acceptItems
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingRequest
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.trekkapi.auth.orgNrFromTokenValidationContext
import no.nav.trekkapi.fellesformat.unmarshalMsgHead
import no.nav.trekkapi.log
import no.nav.trekkapi.persistence.table.MessageStatusEnum
import no.nav.trekkapi.plugin.UnauthorizedException
import no.nav.trekkapi.plugin.ValidationException
import java.io.InputStream
import java.util.Base64
import kotlin.io.bufferedReader
import kotlin.io.readText
import kotlin.use

fun Route.innmeldingRoutes(trekkInnmeldingService: TrekkInnmeldingService) {
    post("/v1/innrapportering") {
        log.debug("Innrapportering kalt")
        val orgnr = orgNrFromTokenValidationContext() ?: throw UnauthorizedException()

        val innmeldingXml = call.receiveText()
        log.debug("Received trekkopplysning with body: $innmeldingXml")
        innmeldingXml.validateInnmeldingXML()
        val id = trekkInnmeldingService.register(orgnr, innmeldingXml)
        log.info("Videresendt trekkopplysningsmelding for orgnr: $orgnr, med ny id: $id")
        call.response.header(HttpHeaders.Location, "/v1/innrapportering/$id")
        call.response.header(HttpHeaders.RetryAfter, "10")
        call.respond(HttpStatusCode.Accepted)
    }

    get("/v1/innrapportering/{id}") {
        val id = call.pathParameters["id"]!!
        log.debug("Hent innrapporteringstatus kalt med id: $id")
        val orgnr = orgNrFromTokenValidationContext() ?: throw UnauthorizedException()

        val status =
            trekkInnmeldingService.getStatus(orgnr, id)
                ?: throw NotFoundException("No message found with the given ID")
        log.info("Returnerer status $status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
        if (status.status == MessageStatusEnum.PENDING) {
            call.response.header(HttpHeaders.RetryAfter, "10")
        }
        if (call.request.acceptsXml() && status.responseXml != null) {
            call.respondText(
                Base64.getDecoder().decode(status.responseXml).toString(Charsets.UTF_8),
                ContentType.Application.Xml,
                HttpStatusCode.OK,
            )
        } else {
            call.respond(HttpStatusCode.OK, status)
        }
    }
}

fun RoutingRequest.acceptsXml(): Boolean = acceptItems().any { it.value == ContentType.Application.Xml.toString() }

fun String.validateInnmeldingXML() =
    runCatching { this.unmarshalMsgHead() }
        .onFailure { throw ValidationException("Invalid XML format", it) }

fun Route.testRoutes(trekkInnmeldingService: TrekkInnmeldingService) {
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

    post("/test/innrapportering") {
        log.debug("TEST-Innrapportering kalt med body")
        val orgnr = "974761076"
        val innmeldingXml = call.receiveText()
        log.debug("Received trekkopplysning with body: $innmeldingXml")
        innmeldingXml.validateInnmeldingXML()
        val id = trekkInnmeldingService.register(orgnr, innmeldingXml)
        log.info("Videresendt trekkopplysningsmelding for orgnr: $orgnr, med ny id: $id")
        call.response.header(HttpHeaders.Location, "/v1/innrapportering/$id")
        call.response.header(HttpHeaders.RetryAfter, "10")
        call.respond(HttpStatusCode.Accepted)
    }

    get("/test/innrapportering/{id}") {
        val id = call.pathParameters["id"]!!
        log.debug("TEST Hent innrapporteringstatus kalt med id: $id")

        val orgnr = "974761076"
        val status = trekkInnmeldingService.getStatus(orgnr, id)
        if (status != null) {
            log.info("Returnerer status $status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
            call.respond(HttpStatusCode.OK, status)
        } else {
            log.warn("Finnes ingen status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/test/listlast/{length}") {
        val length = call.pathParameters["length"]!!
        log.debug("TEST Vis de siste $length mottatte meldingene")

        val list: String = trekkInnmeldingService.listLast(length.toInt())
        log.info("Returnerer de siste $length mottatte meldingene")
        call.respondText(list, ContentType.Text.Html, HttpStatusCode.OK)
    }

    get("/testMq") {
        log.info("Testing MQ......")
        trekkInnmeldingService.verifyConnection()
        log.info("MQ connection OK")
        call.respondText("MQ connection OK")
    }
}
