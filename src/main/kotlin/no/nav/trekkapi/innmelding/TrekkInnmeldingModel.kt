package no.nav.trekkapi.innmelding

import no.nav.trekkapi.fellesformat.AuthData
import no.nav.trekkapi.fellesformat.InputTrekkopplysning
import no.nav.trekkapi.fellesformat.createEIFellesFormatTrekkopplysning
import no.nav.trekkapi.fellesformat.unmarshalFellesformat
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import java.io.StringWriter
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
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
    ): EIFellesformat =
        createEIFellesFormatTrekkopplysning(
            InputTrekkopplysning(id, id, payload),
            AuthData("", orgnr),
            timestamp,
        )

    fun parseTrekkInnmeldingResponseAsFellesFormat(message: String): EIFellesformat = message.unmarshalFellesformat()

    fun orgnrOgMeldingsId(fellesFormatRespons: EIFellesformat): Pair<String, String> =
        Pair(fellesFormatRespons.mottakenhetBlokk.orgNummer, fellesFormatRespons.mottakenhetBlokk.ediLoggId)

    fun isRejected(fellesFormatRespons: EIFellesformat): Boolean = "Avvisning" == ebAction(fellesFormatRespons)

    fun isAccepted(fellesFormatRespons: EIFellesformat): Boolean = "Kvittering" == ebAction(fellesFormatRespons)

    fun getRejectionDescription(fellesFormatRespons: EIFellesformat): String = appRecError(fellesFormatRespons).dn ?: ""

    fun getRejectionCode(fellesFormatRespons: EIFellesformat): String? = appRecError(fellesFormatRespons).v

    fun ebAction(fellesFormatRespons: EIFellesformat): String = fellesFormatRespons.mottakenhetBlokk.ebAction

    fun appRecError(fellesFormatRespons: EIFellesformat) = fellesFormatRespons.appRec.error!![0]!!

    fun getFagmeldingXmlFraFellesformat(
        rawXml: String,
        fellesFormat: EIFellesformat,
    ): String? {
        val tagName =
            when {
                isAccepted(fellesFormat) -> "MsgHead"
                isRejected(fellesFormat) -> "AppRec"
                else -> return null
            }
        val doc =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
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
