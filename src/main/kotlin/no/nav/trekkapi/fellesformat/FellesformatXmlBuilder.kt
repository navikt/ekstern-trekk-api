package no.nav.trekkapi.fellesformat

import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class FellesformatXmlBuilder {
    // Bygger XML uten unmarshal/marshal, for å beholde input-payload uendret.
    // Produsert XML vil ha samme namespace-definisjoner som input, men vil få sorterte attributter.
    // Siden Trekkopplysning (fagsystem) må ha annen sortering av attributter i MottakenhetBlokk,
    // hardkodes MottakenhetBlokk-XML med skreddersydd attributt-sortering.

    fun buildXml(
        mottakenhetBlokk: EIFellesformat.MottakenhetBlokk,
        payload: ByteArray,
    ): String {
        val doc = buildFellesformatDocument(payload)
        val mottakenhetBlokkXml = buildCustomXml(mottakenhetBlokk)
        return toXmlAddingMottakenhetBlokk(doc, mottakenhetBlokkXml)
    }

    // Skreddersydd attributt-sortering, verifisert for trekkopplysning og sykmelding
    fun buildCustomXml(m: EIFellesformat.MottakenhetBlokk): String {
        var xml = "<MottakenhetBlokk"
        if (m.ediLoggId != null) xml += " ediLoggId=\"${m.ediLoggId}\""
        if (m.avsender != null) xml += " avsender=\"${m.avsender}\""
        if (m.ebXMLSamtaleId != null) xml += " ebXMLSamtaleId=\"${m.ebXMLSamtaleId}\""
        if (m.meldingsType != null) xml += " meldingsType=\"${m.meldingsType}\""
        if (m.avsenderRef != null) xml += " avsenderRef=\"${m.avsenderRef}\""
        if (m.avsenderFnrFraDigSignatur != null) xml += " avsenderFnrFraDigSignatur=\"${m.avsenderFnrFraDigSignatur}\""
        if (m.mottattDatotid != null) xml += " mottattDatotid=\"${m.mottattDatotid.toXMLFormat()}\""
        if (m.orgNummer != null) xml += " orgNummer=\"${m.orgNummer}\""
        if (m.partnerReferanse != null) xml += " partnerReferanse=\"${m.partnerReferanse}\""
        if (m.herIdentifikator != null) xml += " herIdentifikator=\"${m.herIdentifikator}\""
        if (m.ebAction != null) xml += " ebAction=\"${m.ebAction}\""
        if (m.ebRole != null) xml += " ebRole=\"${m.ebRole}\""
        if (m.ebService != null) xml += " ebService=\"${m.ebService}\""
        xml += "/>"
        return xml
    }

    fun buildFellesformatDocument(payload: ByteArray): Document {
        val f: DocumentBuilderFactory = createDocumentBuilderFactory()
        val doc = f.newDocumentBuilder().newDocument()
        doc.xmlStandalone = true
        val ffElement = doc.createElementNS("http://www.trygdeetaten.no/xml/eiff/1/", "EI_fellesformat")
        doc.appendChild(ffElement)

        val payloadDoc = f.newDocumentBuilder().parse(ByteArrayInputStream(payload))
        val payloadElement = payloadDoc.childNodes.item(0) // MsgHead
        val msgHead = doc.importNode(payloadElement, true)
        ffElement.appendChild(msgHead)

        return doc
    }

    fun toXmlAddingMottakenhetBlokk(
        doc: Document,
        mottakenhetBlokkXml: String,
    ): String {
        val result = StringWriter()
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.transform(DOMSource(doc), StreamResult(result))
        val docXml = result.toString()

        val tokenWithoutNamespace = "</EI_fellesformat>"
        val insertPos = docXml.indexOf(tokenWithoutNamespace)
        if (insertPos != -1) return docXml.substring(0, insertPos) + mottakenhetBlokkXml + docXml.substring(insertPos)

        val tokenWithNamespace = Regex("</ns\\d*:EI_fellesformat>")
        val found = tokenWithNamespace.find(docXml)
        if (found != null) {
            val insertPos = found.range.first
            return docXml.substring(0, insertPos) + mottakenhetBlokkXml + docXml.substring(insertPos)
        }

        return docXml
    }

    private fun createDocumentBuilderFactory(): DocumentBuilderFactory {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        factory.isXIncludeAware = false
        factory.isExpandEntityReferences = false
        factory.isNamespaceAware = true
        return factory
    }
}
