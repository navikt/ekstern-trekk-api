package no.nav.trekkapi

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SwaggerTest {
    @Test
    fun `GET swagger returns 200 with HTML`() =
        testApplication {
            application {
                routing {
                    swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
                }
            }

            val response = client.get("/swagger")

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `OpenAPI spec contains expected endpoints`() {
        val spec =
            SwaggerTest::class.java
                .getResourceAsStream("/openapi/documentation.yaml")
                ?.bufferedReader()
                ?.readText()
                ?: error("openapi/documentation.yaml not found in resources")

        assertContains(spec, "/v1/innrapportering")
        assertContains(spec, "post:")
        assertContains(spec, "get:")
        assertContains(spec, "MessageStatus")
        assertContains(spec, "\"422\"")
    }
}
