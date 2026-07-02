package no.nav.trekkapi.innmelding

import no.nav.trekkapi.plugin.ValidationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class XmlValidationTest {
    @Test
    fun `Validate MsgHead in SOAP gives exception`() {
        val payload = this::class.java.getResource("/skatt_eksempel_med_soap.xml")?.readText() ?: ""
        assertThrows<ValidationException> { payload.validateInnmeldingXML() }
    }

    @Test
    fun `Validate MsgHead succeeds`() {
        val payload = this::class.java.getResource("/skatt_eksempel_uten_soap.xml")?.readText() ?: ""
        payload.validateInnmeldingXML()
    }
}
