package no.nav.trekkapi.fellesformat

import jakarta.xml.bind.JAXBContext.newInstance
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.nav.innrapporteringtrekk._2010_02_04.Identifisering
import no.kith.xmlstds.nav.innrapporteringtrekk._2010_02_04.InnrapporteringTrekk
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

val fellesformatXmlMarshaller =
    XmlMarshaller(
        newInstance(
            no.trygdeetaten.xml.eiff._1.ObjectFactory::class.java,
            no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
        ),
    )

val msgheadXmlMarshaller =
    XmlMarshaller(
        newInstance(
            no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
        ),
    )

// Egen, isolert kontekst kun brukt til å tolke InnrapporteringTrekk-innholdet i en respons-MsgHead.
// Holdes bevisst adskilt fra msgheadXmlMarshaller/fellesformatXmlMarshaller over, siden disse også
// brukes til å parse/validere innkommende meldinger som ikke alltid er strengt navnerom-kvalifisert.
val innrapporteringTrekkXmlMarshaller =
    XmlMarshaller(
        newInstance(
            no.kith.xmlstds.nav.innrapporteringtrekk._2010_02_04.ObjectFactory::class.java,
        ),
    )

fun String.unmarshalFellesformat(): EIFellesformat = fellesformatXmlMarshaller.unmarshal(this, EIFellesformat::class.java)

fun String.unmarshalMsgHead(): MsgHead = msgheadXmlMarshaller.unmarshal(this, MsgHead::class.java)

// Innholdet i Document/RefDoc/Content er deklarert som xsd:any (lax=true) i MsgHead-skjemaet. Siden
// msgheadXmlMarshaller sin kontekst ikke kjenner InnrapporteringTrekk-pakken, kommer dette innholdet
// tilbake som rå DOM-elementer, som vi her tolker eksplisitt via en egen JAXB-kontekst.
fun MsgHead.identifisering(): Identifisering? =
    document
        .mapNotNull { it.refDoc?.content }
        .flatMap { it.any }
        .filterIsInstance<Element>()
        .firstOrNull { it.localName == "InnrapporteringTrekk" }
        ?.let { innrapporteringTrekkXmlMarshaller.toDomainObject(it) as? InnrapporteringTrekk }
        ?.identifisering

fun marshalTrekkopplysning(fellesFormat: EIFellesformat): String {
    val writer = StringWriter()
    val xmlStreamWriter = TrekkopplysningWriter(XMLOutputFactory.newFactory().createXMLStreamWriter(writer))
    fellesformatXmlMarshaller.marshal(fellesFormat, xmlStreamWriter)
    return writer.toString()
}

// Denne XML-writeren overstyrer normal serialisering for å få XML a la gamle eMottak:
// Det brukes IKKE namespace-prefikser, hvert namespace deklareres som default NS inni topp-elementet det hører til
// Virker som mottakerne må ha det EKSAKT som kodet under
// I tillegg SKAL service-action-role attributtene i MottakenhetBlokk komme i helt spesifikk rekkefølge.
class TrekkopplysningWriter(
    writer: XMLStreamWriter,
) : DelegatingXMLStreamWriter(writer) {
    // I element hvor attributtene skal komme spesialsortert, cacher vi dem til slutt-tagen skal skrives
    var deferAttributeWritingToElementEnd: Boolean = false
    val cachedAttributesWithValues: MutableMap<String, String> = mutableMapOf()

    // Attributtene som skal sorteres, i ønsket rekkefølge
    val attributesToSort = listOf("ebAction", "ebRole", "ebService")

    override fun writeStartElement(
        namespaceURI: String?,
        localName: String?,
        prefix: String?,
    ) {
        if (localName == "EI_fellesformat") {
            super.writeStartElement("", "EI_fellesformat", "")
            super.writeDefaultNamespace("http://www.trygdeetaten.no/xml/eiff/1/")
        } else if (localName == "MsgHead") {
            super.writeStartElement("", "MsgHead", "")
            super.writeDefaultNamespace("http://www.kith.no/xmlstds/msghead/2006-05-24")
        } else if (localName == "Signature") {
            super.writeStartElement("", "Signature", "")
            super.writeDefaultNamespace("http://www.w3.org/2000/09/xmldsig#")
        } else if (localName == "InnrapporteringTrekk") {
            super.writeStartElement("", "InnrapporteringTrekk", "")
            super.writeDefaultNamespace("http://www.kith.no/xmlstds/nav/innrapporteringtrekk/2010-02-04")
            super.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
            // Ser ut til at den under kommer uansett
//            super.writeAttribute("xsi:schemaLocation", "http://www.kith.no/xmlstds/nav/innrapporteringtrekk/2010-02-04 InnrapporteringTrekk-2010-02-04.xsd")
        } else {
            super.writeStartElement("", localName, "")
        }

        if (localName == "MottakenhetBlokk") {
            deferAttributeWritingToElementEnd = true
        } else {
            deferAttributeWritingToElementEnd = false
        }
    }

    override fun writeNamespace(
        prefix: String?,
        uri: String?,
    ) {
        // Ønsker ikke andre deklarasjoner enn de som eksplisitt er gjort i writeStartElement
        // super.writeNamespace(prefix, uri)
    }

    override fun writeAttribute(
        local: String,
        value: String,
    ) {
        if (deferAttributeWritingToElementEnd && local in attributesToSort) {
            cachedAttributesWithValues.put(local, value)
        } else {
            super.writeAttribute(local, value)
        }
    }

    override fun writeEndElement() {
        if (!cachedAttributesWithValues.isEmpty()) {
            for (attributeName in attributesToSort) {
                writeAttributeIfValueIsCached(attributeName)
            }
            cachedAttributesWithValues.clear()
        }
        super.writeEndElement()
    }

    private fun writeAttributeIfValueIsCached(attributeName: String) {
        if (cachedAttributesWithValues[attributeName] != null) {
            super.writeAttribute(attributeName, cachedAttributesWithValues[attributeName]!!)
        }
    }
}
