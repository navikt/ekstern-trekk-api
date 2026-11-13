package no.nav.trekkapi.innmelding

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import no.nav.security.token.support.v3.TokenValidationContextPrincipal
import no.nav.trekkapi.api.pending
import no.nav.trekkapi.auth.MASKINPORTEN_AUTH_INNMELDING
import no.nav.trekkapi.log
import no.nav.trekkapi.plugin.configureContentNegotiation
import no.nav.trekkapi.plugin.configureStatusPages
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class TrekkInnmeldingLoggingTest {
    private val orgnr = "123456789"
    private val service = mockk<TrekkInnmeldingService>()
    private val appender =
        object : ListAppender<ILoggingEvent>() {
            override fun append(event: ILoggingEvent) {
                event.prepareForDeferredProcessing()
                super.append(event)
            }
        }

    @BeforeEach
    fun captureLogs() {
        appender.start()
        (log as Logger).addAppender(appender)
    }

    @AfterEach
    fun stopCapturingLogs() {
        (log as Logger).detachAppender(appender)
        appender.stop()
    }

    @Test
    fun `GET includes authenticated orgnr and given ID across coroutine switches`() =
        testApplication {
            val id = "given-message-id"
            coEvery { service.getStatus(orgnr, id) } coAnswers {
                withContext(Dispatchers.IO) {
                    delay(1)
                    assertEquals(orgnr, MDC.get("orgnr"))
                    assertEquals(id, MDC.get("messageId"))
                    log.info("Status lookup")
                }
                pending(id, Instant.now())
            }
            application { configureTestRoutes() }

            val response = client.get("/v1/innrapportering/$id")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("10", response.headers[HttpHeaders.RetryAfter])
            assertContext("Status lookup", orgnr, id)
            assertContext("Returnerer status", orgnr, id)
        }

    @Test
    fun `POST uses generated ID in service logs and Location`() =
        testApplication {
            var generatedId: String? = null
            coEvery { service.register(orgnr, any(), any()) } coAnswers {
                generatedId = secondArg()
                withContext(Dispatchers.IO) {
                    delay(1)
                    assertEquals(orgnr, MDC.get("orgnr"))
                    assertEquals(generatedId, MDC.get("messageId"))
                    log.info("Registering message")
                }
            }
            application { configureTestRoutes() }

            val response =
                client.post("/v1/innrapportering") {
                    setBody(validXml())
                }

            val id = assertNotNull(generatedId)
            Uuid.parse(id)
            assertEquals(HttpStatusCode.Accepted, response.status)
            assertEquals("/v1/innrapportering/$id", response.headers[HttpHeaders.Location])
            assertEquals("10", response.headers[HttpHeaders.RetryAfter])
            assertContext("Registering message", orgnr, id)
            assertContext("Videresendt trekkopplysningsmelding", orgnr, id)
        }

    @Test
    fun `GET not found retains context in StatusPages without leaking to next call`() =
        testApplication {
            val id = "missing-message-id"
            coEvery { service.getStatus(orgnr, id) } returns null
            application { configureTestRoutes() }

            assertEquals(HttpStatusCode.NotFound, client.get("/v1/innrapportering/$id").status)
            assertContext("Not found: No message found", orgnr, id)

            assertEquals(HttpStatusCode.OK, client.get("/unrelated").status)
            assertContext("Unrelated request", null, null)
        }

    @Test
    fun `POST registration failure retains generated ID in error log`() =
        testApplication {
            var generatedId: String? = null
            coEvery { service.register(orgnr, any(), any()) } coAnswers {
                generatedId = secondArg()
                throw IllegalStateException("Registration failed")
            }
            application { configureTestRoutes() }

            val response =
                client.post("/v1/innrapportering") {
                    setBody(validXml())
                }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertContext("Unhandled exception", orgnr, assertNotNull(generatedId))
        }

    @Test
    fun `Invalid XML logs authenticated orgnr without allocating a message ID`() =
        testApplication {
            application { configureTestRoutes() }

            val response =
                client.post("/v1/innrapportering") {
                    setBody("not xml")
                }

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertContext("Validation error", orgnr, null)
            coVerify(exactly = 0) { service.register(any(), any(), any()) }
        }

    @Test
    fun `Concurrent requests keep separate message IDs`() =
        testApplication {
            coEvery { service.getStatus(orgnr, any()) } coAnswers {
                val id = secondArg<String>()
                withContext(Dispatchers.IO) {
                    delay(10)
                    assertEquals(orgnr, MDC.get("orgnr"))
                    assertEquals(id, MDC.get("messageId"))
                }
                pending(id, Instant.now())
            }
            application { configureTestRoutes() }

            coroutineScope {
                val first = async { client.get("/v1/innrapportering/first") }
                val second = async { client.get("/v1/innrapportering/second") }
                assertEquals(HttpStatusCode.OK, first.await().status)
                assertEquals(HttpStatusCode.OK, second.await().status)
            }
            assertNull(MDC.get("orgnr"))
            assertNull(MDC.get("messageId"))
        }

    private fun Application.configureTestRoutes() {
        val principal =
            mockk<TokenValidationContextPrincipal> {
                every { context.getClaims(MASKINPORTEN_AUTH_INNMELDING).get("consumer") } returns
                    mapOf("ID" to "0192:$orgnr")
            }
        install(Authentication) {
            register(
                object : AuthenticationProvider(object : AuthenticationProvider.Config(MASKINPORTEN_AUTH_INNMELDING) {}) {
                    override suspend fun onAuthenticate(context: AuthenticationContext) {
                        context.principal(principal)
                    }
                },
            )
        }
        configureContentNegotiation()
        configureStatusPages()
        routing {
            authenticate(MASKINPORTEN_AUTH_INNMELDING) {
                innmeldingRoutes(service)
            }
            get("/unrelated") {
                log.info("Unrelated request")
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    private fun validXml(): String = assertNotNull(javaClass.getResource("/trekkopplysning_innmelding.xml")).readText()

    private fun assertContext(
        messagePrefix: String,
        expectedOrgnr: String?,
        expectedId: String?,
    ) {
        val event = appender.list.single { it.formattedMessage.startsWith(messagePrefix) }
        assertEquals(expectedOrgnr, event.mdcPropertyMap["orgnr"])
        assertEquals(expectedId, event.mdcPropertyMap["messageId"])
    }
}
