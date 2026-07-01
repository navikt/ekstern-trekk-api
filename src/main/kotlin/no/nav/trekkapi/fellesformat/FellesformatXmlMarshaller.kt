package no.nav.trekkapi.fellesformat

import jakarta.xml.bind.JAXBContext.newInstance
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.trygdeetaten.xml.eiff._1.EIFellesformat

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

fun String.unmarshalFellesformat(): EIFellesformat = fellesformatXmlMarshaller.unmarshal(this, EIFellesformat::class.java)

fun String.unmarshalMsgHead(): MsgHead = msgheadXmlMarshaller.unmarshal(this, MsgHead::class.java)
