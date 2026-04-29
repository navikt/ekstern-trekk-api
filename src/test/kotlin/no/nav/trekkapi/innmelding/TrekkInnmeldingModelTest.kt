package no.nav.trekkapi.innmelding

import no.nav.trekkapi.fellesformat.marshalTrekkopplysning
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrekkInnmeldingModelTest {

    @Test
    fun `Build FellesFormat produces expected object`() {
        val orgnr = "123456789"
        val id = "the-ID-is-333444555"
        val expectedId = "trekkapi-123456789-the-ID-is-333444555"
        val payload = this::class.java.getResource("/trekkopplysning_innmelding.xml")?.readText() ?: ""

        val trekkInnmeldingModel = TrekkInnmeldingModel()
        val result = trekkInnmeldingModel.buildTrekkInnmelding_FellesFormat(orgnr, id, payload)

        assertEquals(expectedId, result.mottakenhetBlokk.ediLoggId, "ediLoggId")
        assertEquals("Trekkopplysning", result.mottakenhetBlokk.ebService, "ebService")
        assertEquals("Innmelding", result.mottakenhetBlokk.ebAction, "ebAction")
        assertEquals("Fordringshaver", result.mottakenhetBlokk.ebRole, "ebRole")
        assertEquals(expectedId, result.mottakenhetBlokk.ebXMLSamtaleId, "convId")
        assertEquals("123456789", result.mottakenhetBlokk.orgNummer, "orgnr")
        assertEquals("", result.mottakenhetBlokk.herIdentifikator, "HER-id")
        assertEquals("123456789", result.mottakenhetBlokk.avsender, "avsender")
        assertEquals("", result.mottakenhetBlokk.partnerReferanse, "partnerRef")
        assertEquals("xml", result.mottakenhetBlokk.meldingsType, "meldingsType")
        assertTrue(result.mottakenhetBlokk.mottattDatotid != null, "mottattDatotid")
        assertEquals("", result.mottakenhetBlokk.avsenderRef, "avsenderRef")

        assertEquals(1, result.msgHead.document.size, "payload documents")
        assertEquals("SV:Innrapportering av trekk til NAV", result.msgHead.document.get(0).contentDescription, "payload contentDescription")
    }

    // todo fiks verifikasjon av XML, f.ex les 1 og 1 element
    @Test
    @Disabled
    fun `Marshalled FellesFormat produces expected text message`() {
        val orgnr = "123456789"
        val id = "the-ID-is-333444555"
        val payload = this::class.java.getResource("/trekkopplysning_innmelding.xml")?.readText() ?: ""

        val trekkInnmeldingModel = TrekkInnmeldingModel()
        val fellesformat = trekkInnmeldingModel.buildTrekkInnmelding_FellesFormat(orgnr, id, payload)
        val message = marshalTrekkopplysning(fellesformat)

        val expectedProlog = """
<?xml version='1.0' encoding='UTF-8'?>
<EI_fellesformat xmlns="http://www.trygdeetaten.no/xml/eiff/1/">            
        """.trimIndent()
        val expectedEpilog = """
<MottakenhetBlokk ediLoggId="trekkapi-123456789-the-ID-is-333444555" avsender="123456789" 
  ebXMLSamtaleId="trekkapi-123456789-the-ID-is-333444555" meldingsType="xml" avsenderRef="" 
  mottattDatotid="2026-04-29T13:20:49.692+02:00" orgNummer="123456789" partnerReferanse="" 
  herIdentifikator="" ebAction="Innmelding" ebRole="Fordringshaver" ebService="Trekkopplysning"/></EI_fellesformat>>
        """.trimIndent()
        val expected = expectedProlog + payload.trimIndent() + expectedEpilog
        assertEquals(expected, message, "generated text message")
    }

    @Test
    fun `Parse FellesFormat response produces expected object`() {
        val respons = this::class.java.getResource("/trekkopplysning_respons.xml")?.readText() ?: ""

        val trekkInnmeldingModel = TrekkInnmeldingModel()
        val result = trekkInnmeldingModel.parseTrekkInnmeldingResponse_FellesFormat(respons.toByteArray())

        assertEquals("trekkapi-69abb69f-b491-4d34-aeb1-10c02c7b98b6", result.mottakenhetBlokk.ediLoggId, "ediLoggId")
        assertEquals("Trekkopplysning", result.mottakenhetBlokk.ebService, "ebService")
        assertEquals("Avvisning", result.mottakenhetBlokk.ebAction, "ebAction")
        assertEquals("Ytelsesutbetaler", result.mottakenhetBlokk.ebRole, "ebRole")
        assertEquals("91e01f3c-b754-4ea3-98fe-07c249661bba", result.mottakenhetBlokk.ebXMLSamtaleId, "convId")
//        assertEquals("123456789", result.mottakenhetBlokk.orgNummer, "orgnr")
        assertEquals("8142626", result.mottakenhetBlokk.herIdentifikator, "HER-id")
        assertEquals("TODO1", result.mottakenhetBlokk.avsender, "avsender")
        assertEquals("nav:qass:36181", result.mottakenhetBlokk.partnerReferanse, "partnerRef")
        assertEquals("xml", result.mottakenhetBlokk.meldingsType, "meldingsType")
        assertTrue(result.mottakenhetBlokk.mottattDatotid != null, "mottattDatotid")
        assertEquals("TODO2", result.mottakenhetBlokk.avsenderRef, "avsenderRef")

        assertEquals(true, trekkInnmeldingModel.avvist(result), "Avvist")
        assertEquals(false, trekkInnmeldingModel.akseptert(result), "Akseptert")
        assertEquals("Trekkvedtak finnes fra før", trekkInnmeldingModel.hentAvvisningsBeskrivelse(result), "Beskrivelse")
        assertEquals("69abb69f-b491-4d34-aeb1-10c02c7b98b6", trekkInnmeldingModel.getDbId(result), "DB id")
    }
}
