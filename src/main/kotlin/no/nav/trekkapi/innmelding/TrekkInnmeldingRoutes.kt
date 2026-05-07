package no.nav.trekkapi.innmelding

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
import java.io.InputStream
import kotlin.use

// todo loag test for denne, hvis vi får til maskinporten mock

fun Route.innmeldingRoutes(
    trekkInnmeldingService: TrekkInnmeldingService
) {
    // Alternativ 1: POST hvor ID genereres av tjenesten
    post("/v1/innrapportering") {
        log.debug("Innrapportering kalt")

        val orgnr = orgNrFromTokenValidationContext()
        if (orgnr == null) {
            log.error("Tjeneste kalt uten auth med orgnr")
            call.respondText(
                "Autorisasjonsfeil",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.Unauthorized
            )
            return@post
        }

        val body = call.receiveText()
        try {
            val id = trekkInnmeldingService.register(orgnr, body)
            log.info("Videresendt trekkopplysningsmelding for orgnr: $orgnr, med ny id: $id")
            call.response.header(HttpHeaders.Location, "/v1/innrapportering/$id")
            call.respondText("Accepted", contentType = ContentType.Text.Plain, status = HttpStatusCode.OK)
        } catch (e: Exception) {
            // Dette vil dekke timeout fra backend, og alle feilsituasjoner
            log.error("Feil ved videresending av trekkopplysningsmelding for orgnr: $orgnr", e)
            call.respondText(
                "Unexpected error",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    // Alternativ 2: PUT hvor ID genereres av kaller
    put("/v1/innrapportering/{id}") {
        val id = call.pathParameters["id"]!!
        log.debug("Innrapportering kalt med id: $id")

        val orgnr = orgNrFromTokenValidationContext()
        if (orgnr == null) {
            log.error("Tjeneste kalt uten auth med orgnr")
            call.respondText(
                "Autorisasjonsfeil",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.Unauthorized
            )
            return@put
        }

        if (trekkInnmeldingService.alreadyRegistered(orgnr, id)) {
            log.info("Trekkopplysningsmelding allerede registrert for orgnr: $orgnr, id: $id")
            call.response.header(HttpHeaders.Location, "/v1/innrapportering/$id")
            call.respondText("Accepted", contentType = ContentType.Text.Plain, status = HttpStatusCode.OK)
            return@put
        }

        val body = call.receiveText()
        try {
            trekkInnmeldingService.register(orgnr, id, body)
            log.info("Videresendt trekkopplysningsmelding for orgnr: $orgnr, id: $id")
            call.response.header(HttpHeaders.Location, "/v1/innrapportering/$id")
            call.respondText("Accepted", contentType = ContentType.Text.Plain, status = HttpStatusCode.OK)
        } catch (e: Exception) {
            // Dette vil dekke timeout fra backend, og alle feilsituasjoner
            log.error("Feil ved videresending av trekkopplysningsmelding for orgnr: $orgnr, id: $id", e)
            call.respondText(
                "Unexpected error",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    get("/v1/innrapportering/{id}") {
        val id = call.pathParameters["id"]!!
        log.debug("Hent innrapporteringstatus kalt med id: $id")

        val orgnr = orgNrFromTokenValidationContext()
        if (orgnr == null) {
            log.error("Tjeneste kalt uten auth med orgnr")
            call.respondText(
                "Autorisasjonsfeil",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.Unauthorized
            )
            return@get
        }

        val status = trekkInnmeldingService.getStatus(orgnr, id)
        if (status != null) {
            log.info("Returnerer status $status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
            call.respond(status = HttpStatusCode.OK, status)
        } else {
            log.warn("Finnes ingen status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
            call.respondText("Not found", contentType = ContentType.Text.Plain, status = HttpStatusCode.NotFound)
        }
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
            call.respondText("Accepted", contentType = ContentType.Text.Plain, status = HttpStatusCode.OK)
            return@get
        }

        val inputStream: InputStream? = this::class.java.getResourceAsStream("/testbody.xml")
        val body = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        try {
            trekkInnmeldingService.register(orgnr, id, body)
            log.info("Videresendt trekkopplysningsmelding for orgnr: $orgnr, id: $id")
            call.response.header(HttpHeaders.Location, "/test/innrapportering/$id")
            call.respondText("Accepted", contentType = ContentType.Text.Plain, status = HttpStatusCode.OK)
        } catch (e: Exception) {
            // Dette vil dekke timeout fra backend, og alle feilsituasjoner
            log.error("Feil ved videresending av trekkopplysningsmelding for orgnr: $orgnr, id: $id", e)
            call.respondText("Unexpected error", contentType = ContentType.Text.Plain, status = HttpStatusCode.InternalServerError)
        }
    }

    get("/test/innrapportering/{id}") {
        val id = call.pathParameters["id"]!!
        log.debug("TEST Hent innrapporteringstatus kalt med id: $id")

        val orgnr = "123456789"

        val status = trekkInnmeldingService.getStatus(orgnr, id)
        if (status != null) {
            log.info("Returnerer status $status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
            call.respond(status = HttpStatusCode.OK, status)
        } else {
            log.warn("Finnes ingen status for trekkopplysningsmelding med orgnr: $orgnr, id: $id")
            call.respondText("Not found", contentType = ContentType.Text.Plain, status = HttpStatusCode.NotFound)
        }
    }
}
