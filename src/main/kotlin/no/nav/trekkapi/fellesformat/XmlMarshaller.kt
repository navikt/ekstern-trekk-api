package no.nav.trekkapi.fellesformat

import jakarta.xml.bind.JAXBContext
import org.w3c.dom.Node
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamWriter

class XmlMarshaller(
    jaxbContext: JAXBContext,
) {
    private val marshaller = jaxbContext.createMarshaller()
    private val unmarshaller =
        jaxbContext.createUnmarshaller().apply {
            eventHandler =
                jakarta.xml.bind.ValidationEventHandler { event ->
                    throw event.linkedException ?: RuntimeException(event.message)
                }
        }
    private val marshallingMonitor = Any()
    private val unmarshallingMonitor = Any()

    fun marshal(objekt: Any): String {
        val writer = StringWriter()
        synchronized(marshallingMonitor) {
            marshaller.marshal(objekt, writer)
        }
        return writer.toString()
    }

    fun marshal(
        objekt: Any,
        xmlStreamWriter: XMLStreamWriter,
    ) {
        synchronized(marshallingMonitor) {
            marshaller.marshal(objekt, xmlStreamWriter)
        }
    }

    fun toDomainObject(any: Any): Any =
        synchronized(unmarshallingMonitor) {
            unmarshaller.unmarshal(any as Node)
        }

    fun marshalToByteArray(objekt: Any): ByteArray =
        ByteArrayOutputStream().use {
            synchronized(marshallingMonitor) {
                marshaller.marshal(objekt, it)
            }
            it.toByteArray()
        }

    fun <T> unmarshal(
        xml: String,
        clazz: Class<T>,
    ): T {
        val factory =
            XMLInputFactory.newInstance().apply {
                setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
                setProperty(XMLInputFactory.SUPPORT_DTD, false)
            }
        val reader = factory.createXMLStreamReader(xml.reader())
        return synchronized(unmarshallingMonitor) {
            unmarshaller.unmarshal(reader, clazz).value
        }
    }
}
