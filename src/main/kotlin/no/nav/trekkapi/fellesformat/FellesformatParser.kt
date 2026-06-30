package no.nav.trekkapi.fellesformat

import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

data class MottakenhetBlokk(
    val ediLoggId: String,
    val ebService: String,
    val ebAction: String,
    val ebRole: String,
    val ebXMLSamtaleId: String,
    val orgNummer: String,
    val herIdentifikator: String,
    val avsender: String,
    val partnerReferanse: String,
    val meldingsType: String,
    val mottattDatotid: String,
    val avsenderRef: String,
)

data class AppRecError(
    val v: String?,
    val dn: String?,
)

data class FellesformatRespons(
    val mottakenhetBlokk: MottakenhetBlokk,
    val appRecError: AppRecError?,
)

fun String.parseFellesformatRespons(): FellesformatRespons {
    val document = parseSecureDocument(this)
    val mottakenhetBlokk =
        document.firstElementByLocalName("MottakenhetBlokk")
            ?: error("Mangler MottakenhetBlokk i fellesformat-respons")
    val errorElement = document.firstElementByLocalName("Error")
    return FellesformatRespons(
        mottakenhetBlokk =
            MottakenhetBlokk(
                ediLoggId = mottakenhetBlokk.getAttribute("ediLoggId"),
                ebService = mottakenhetBlokk.getAttribute("ebService"),
                ebAction = mottakenhetBlokk.getAttribute("ebAction"),
                ebRole = mottakenhetBlokk.getAttribute("ebRole"),
                ebXMLSamtaleId = mottakenhetBlokk.getAttribute("ebXMLSamtaleId"),
                orgNummer = mottakenhetBlokk.getAttribute("orgNummer"),
                herIdentifikator = mottakenhetBlokk.getAttribute("herIdentifikator"),
                avsender = mottakenhetBlokk.getAttribute("avsender"),
                partnerReferanse = mottakenhetBlokk.getAttribute("partnerReferanse"),
                meldingsType = mottakenhetBlokk.getAttribute("meldingsType"),
                mottattDatotid = mottakenhetBlokk.getAttribute("mottattDatotid"),
                avsenderRef = mottakenhetBlokk.getAttribute("avsenderRef"),
            ),
        appRecError =
            errorElement?.let {
                AppRecError(
                    v = it.getAttribute("V").ifEmpty { null },
                    dn = it.getAttribute("DN").ifEmpty { null },
                )
            },
    )
}

private fun xsdSource(path: String): StreamSource {
    val url = checkNotNull(object {}.javaClass.getResource(path)) { "$path not found on classpath" }
    return StreamSource(url.toExternalForm())
}

private val msgHeadSchema by lazy {
    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
        arrayOf<Source>(
            xsdSource("/xsd/xmldsig-core-schema.xsd"),
            xsdSource("/xsd/kith-base64.xsd"),
            xsdSource("/xsd/kith.xsd"),
            xsdSource("/xsd/felleskomponent1.xsd"),
            xsdSource("/xsd/InnrapporteringTrekk-2010-02-04.xsd"),
            xsdSource("/xsd/MsgHead-v1_2.xsd"),
        ),
    )
}

fun String.validateMsgHeadSchema() {
    val validator =
        msgHeadSchema.newValidator().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        }
    validator.validate(StreamSource(this.byteInputStream()))
}

internal fun secureDocumentBuilderFactory(): DocumentBuilderFactory =
    DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        isXIncludeAware = false
        isExpandEntityReferences = false
    }

internal fun parseSecureDocument(xml: String): Document = secureDocumentBuilderFactory().newDocumentBuilder().parse(xml.byteInputStream())

private fun Document.firstElementByLocalName(localName: String): Element? {
    val nodes = getElementsByTagNameNS("*", localName)
    return if (nodes.length == 0) null else nodes.item(0) as Element
}
