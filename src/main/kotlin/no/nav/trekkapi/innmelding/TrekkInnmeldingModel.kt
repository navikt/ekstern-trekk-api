package no.nav.trekkapi.innmelding

import no.nav.trekkapi.fellesformat.AuthData
import no.nav.trekkapi.fellesformat.FellesformatRespons
import no.nav.trekkapi.fellesformat.InputTrekkopplysning
import no.nav.trekkapi.fellesformat.createEIFellesFormatTrekkopplysning
import no.nav.trekkapi.fellesformat.parseFellesformatRespons
import no.nav.trekkapi.fellesformat.secureDocumentBuilderFactory
import java.io.StringWriter
import java.time.Instant
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class TrekkInnmeldingModel {
    // Holder rede på "formatene" i
    // objekter som skal sendes i retning fagsystem og responsene som kommer tilbake (Fellesformat)

    fun buildTrekkInnmeldingAsFellesFormat(
        orgnr: String,
        id: String,
        payload: String,
        timestamp: Instant = Instant.now(),
    ): String =
        createEIFellesFormatTrekkopplysning(
            InputTrekkopplysning(id, id, payload),
            AuthData("", orgnr),
            timestamp,
        )

    fun parseTrekkInnmeldingResponseAsFellesFormat(message: String): FellesformatRespons = message.parseFellesformatRespons()

    fun orgnrOgMeldingsId(fellesFormatRespons: FellesformatRespons): Pair<String, String> =
        Pair(fellesFormatRespons.mottakenhetBlokk.orgNummer, fellesFormatRespons.mottakenhetBlokk.ediLoggId)

    fun isRejected(fellesFormatRespons: FellesformatRespons): Boolean = "Avvisning" == ebAction(fellesFormatRespons)

    fun isAccepted(fellesFormatRespons: FellesformatRespons): Boolean = "Kvittering" == ebAction(fellesFormatRespons)

    fun getRejectionDescription(fellesFormatRespons: FellesformatRespons): String = fellesFormatRespons.appRecError?.dn ?: ""

    fun getRejectionCode(fellesFormatRespons: FellesformatRespons): String? = fellesFormatRespons.appRecError?.v

    fun ebAction(fellesFormatRespons: FellesformatRespons): String = fellesFormatRespons.mottakenhetBlokk.ebAction

    fun getFagmeldingXmlFraFellesformat(
        rawXml: String,
        fellesFormat: FellesformatRespons,
    ): String? {
        val tagName =
            when {
                isAccepted(fellesFormat) -> "MsgHead"
                isRejected(fellesFormat) -> "AppRec"
                else -> return null
            }
        val doc =
            secureDocumentBuilderFactory()
                .newDocumentBuilder()
                .parse(rawXml.byteInputStream())
        val nodes = doc.getElementsByTagNameNS("*", tagName)
        if (nodes.length == 0) return null
        val writer = StringWriter()
        TransformerFactory
            .newInstance()
            .newTransformer()
            .transform(DOMSource(nodes.item(0)), StreamResult(writer))
        return writer.toString()
    }
}
