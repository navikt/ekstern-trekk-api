package no.nav.trekkapi.fellesformat

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.xml.sax.SAXException

class FellesformatParserTest {
    private val validInnmelding: String =
        checkNotNull(javaClass.getResource("/trekkopplysning_innmelding.xml")).readText()

    @Test
    fun `validateMsgHeadSchema accepts valid innmelding`() {
        validInnmelding.validateMsgHeadSchema()
    }

    @Test
    fun `validateMsgHeadSchema rejects XML with unknown root element`() {
        assertThrows<SAXException> {
            "<NotMsgHead/>".validateMsgHeadSchema()
        }
    }

    @Test
    fun `validateMsgHeadSchema rejects MsgHead with missing required MsgId`() {
        val noMsgId = validInnmelding.replace(Regex("<MsgId>[^<]*</MsgId>"), "")
        assertThrows<SAXException> {
            noMsgId.validateMsgHeadSchema()
        }
    }

    @Test
    fun `validateMsgHeadSchema rejects InnrapporteringTrekk in unknown namespace`() {
        val wrongNs =
            validInnmelding.replace(
                "http://www.kith.no/xmlstds/nav/innrapporteringtrekk/2010-02-04",
                "http://www.example.com/unknown",
            )
        assertThrows<SAXException> {
            wrongNs.validateMsgHeadSchema()
        }
    }
}
