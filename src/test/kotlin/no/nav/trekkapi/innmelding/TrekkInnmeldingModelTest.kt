package no.nav.trekkapi.innmelding

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrekkInnmeldingModelTest {
    @Test
    fun `Build FellesFormat wraps payload and produces MottakenhetBlokk`() {
        val orgnr = "123456789"
        val id = "the-ID-is-333444555"
        val payload = this::class.java.getResource("/trekkopplysning_innmelding.xml")?.readText() ?: ""

        val trekkInnmeldingModel = TrekkInnmeldingModel()
        val result = trekkInnmeldingModel.buildTrekkInnmeldingAsFellesFormat(orgnr, id, payload)

        assertTrue(result.contains("""<EI_fellesformat xmlns="http://www.trygdeetaten.no/xml/eiff/1/">"""), "EI_fellesformat wrapper")
        assertTrue(result.contains("""ediLoggId="the-ID-is-333444555""""), "ediLoggId")
        assertTrue(result.contains("""ebService="Trekkopplysning""""), "ebService")
        assertTrue(result.contains("""ebAction="Innmelding""""), "ebAction")
        assertTrue(result.contains("""ebRole="Fordringshaver""""), "ebRole")
        assertTrue(result.contains("""ebXMLSamtaleId="the-ID-is-333444555""""), "convId")
        assertTrue(result.contains("""orgNummer="123456789""""), "orgnr")
        assertTrue(result.contains("""herIdentifikator=""""), "HER-id")
        assertTrue(result.contains("""avsender="123456789""""), "avsender")
        assertTrue(result.contains("""partnerReferanse="EKSTERN_TREKK_API""""), "partnerRef")
        assertTrue(result.contains("""meldingsType="xml""""), "meldingsType")
        assertTrue(result.contains("""avsenderRef=""""), "avsenderRef")
        // payloaden skal pakkes inn ordrett, slik at fagmeldingen bevares
        assertTrue(result.contains("<InnrapporteringTrekk"), "payload preserved")
        assertTrue(result.contains("SV:Innrapportering av trekk til NAV"), "payload contentDescription preserved")
    }

    @Test
    fun `Marshalled FellesFormat produces expected text message`() {
        val orgnr = "123456789"
        val id = "the-ID-is-333444555"
        val timestamp: Instant = Instant.parse("2026-04-29T13:20:49.692+02:00")
        val payload = this::class.java.getResource("/trekkopplysning_innmelding.xml")?.readText() ?: ""

        val trekkInnmeldingModel = TrekkInnmeldingModel()
        val message = trekkInnmeldingModel.buildTrekkInnmeldingAsFellesFormat(orgnr, id, payload, timestamp = timestamp)

        val expectedProlog = """
            <?xml version='1.0' encoding='UTF-8'?>
            <EI_fellesformat xmlns="http://www.trygdeetaten.no/xml/eiff/1/">            
        """
        val expectedEpilog = """
            <MottakenhetBlokk ediLoggId="the-ID-is-333444555" avsender="123456789" 
            ebXMLSamtaleId="the-ID-is-333444555" meldingsType="xml" avsenderRef="" 
            mottattDatotid="2026-04-29T13:20:49.692+02:00" orgNummer="123456789" partnerReferanse="EKSTERN_TREKK_API" 
            herIdentifikator="" ebAction="Innmelding" ebRole="Fordringshaver" ebService="Trekkopplysning"/></EI_fellesformat>
        """
        val expected = expectedProlog + payload + expectedEpilog
        assertEquals(expected.replace("\\s".toRegex(), ""), message.replace("\\s".toRegex(), ""), "generated XML")
    }

    @Test
    fun `Parse FellesFormat response produces expected object`() {
        val respons = this::class.java.getResource("/trekkopplysning_respons_avvist_duplikat.xml")?.readText() ?: ""

        val trekkInnmeldingModel = TrekkInnmeldingModel()
        val result = trekkInnmeldingModel.parseTrekkInnmeldingResponseAsFellesFormat(respons)

        assertEquals("69abb69f-b491-4d34-aeb1-10c02c7b98b6", result.mottakenhetBlokk.ediLoggId, "ediLoggId")
        assertEquals("Trekkopplysning", result.mottakenhetBlokk.ebService, "ebService")
        assertEquals("Avvisning", result.mottakenhetBlokk.ebAction, "ebAction")
        assertEquals("Ytelsesutbetaler", result.mottakenhetBlokk.ebRole, "ebRole")
        assertEquals("91e01f3c-b754-4ea3-98fe-07c249661bba", result.mottakenhetBlokk.ebXMLSamtaleId, "convId")
        assertEquals("123456789", result.mottakenhetBlokk.orgNummer, "orgnr")
        assertEquals("000000001", result.mottakenhetBlokk.herIdentifikator, "HER-id")
        assertEquals("123456789", result.mottakenhetBlokk.avsender, "avsender")
        assertEquals("EKSTERN_TREKK_API", result.mottakenhetBlokk.partnerReferanse, "partnerRef")
        assertEquals("xml", result.mottakenhetBlokk.meldingsType, "meldingsType")
        assertTrue(result.mottakenhetBlokk.mottattDatotid.isNotEmpty(), "mottattDatotid")
        assertEquals("someRef", result.mottakenhetBlokk.avsenderRef, "avsenderRef")

        assertEquals(true, trekkInnmeldingModel.isRejected(result), "Avvist")
        assertEquals(false, trekkInnmeldingModel.isAccepted(result), "Akseptert")
        assertEquals("Trekkvedtak finnes fra før", trekkInnmeldingModel.getRejectionDescription(result), "Beskrivelse")
        assertEquals("B720007F", trekkInnmeldingModel.getRejectionCode(result), "Avvisningskode")
        assertEquals("123456789", trekkInnmeldingModel.orgnrOgMeldingsId(result).first, "DB orgnr")
        assertEquals("69abb69f-b491-4d34-aeb1-10c02c7b98b6", trekkInnmeldingModel.orgnrOgMeldingsId(result).second, "DB id")
    }

    @Test
    fun `getFagmeldingXmlFraFellesformat returns AppRec for rejected response`() {
        val respons = this::class.java.getResource("/trekkopplysning_respons_avvist_duplikat.xml")?.readText() ?: ""
        val trekkInnmeldingModel = TrekkInnmeldingModel()
        val fellesFormat = trekkInnmeldingModel.parseTrekkInnmeldingResponseAsFellesFormat(respons)

        val innerXml = trekkInnmeldingModel.getFagmeldingXmlFraFellesformat(respons, fellesFormat)

        assertTrue(innerXml != null, "innerXml should not be null")
        assertTrue(innerXml.contains("AppRec"), "innerXml should contain AppRec element")
        assertTrue(!innerXml.contains("EI_fellesformat"), "innerXml should not contain EI_fellesformat wrapper")
        assertTrue(!innerXml.contains("MottakenhetBlokk"), "innerXml should not contain MottakenhetBlokk")
        assertTrue(innerXml.contains("B720007F"), "innerXml should contain rejection code")
    }

    @Test
    fun `getFagmeldingXmlFraFellesformat returns MsgHead for accepted response`() {
        val respons = this::class.java.getResource("/trekkopplysning_respons_akseptert.xml")?.readText() ?: ""
        val trekkInnmeldingModel = TrekkInnmeldingModel()
        val fellesFormat = trekkInnmeldingModel.parseTrekkInnmeldingResponseAsFellesFormat(respons)

        val innerXml = trekkInnmeldingModel.getFagmeldingXmlFraFellesformat(respons, fellesFormat)

        assertTrue(innerXml != null, "innerXml should not be null")
        assertTrue(innerXml.contains("MsgHead"), "innerXml should contain MsgHead element")
        assertTrue(!innerXml.contains("EI_fellesformat"), "innerXml should not contain EI_fellesformat wrapper")
        assertTrue(!innerXml.contains("MottakenhetBlokk"), "innerXml should not contain MottakenhetBlokk")
        assertTrue(innerXml.contains("INNRAPPORTERING_TREKK_RETUR"), "innerXml should contain message type")
    }
}
