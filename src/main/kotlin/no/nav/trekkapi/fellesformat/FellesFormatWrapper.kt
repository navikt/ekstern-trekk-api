package no.nav.trekkapi.fellesformat

import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.trekkapi.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import no.trygdeetaten.xml.eiff._1.ObjectFactory
import java.time.Instant
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

private val fellesFormatFactory = ObjectFactory()

// todo må evt. finne disse via auth/maskinporten. Vet ikke om HER-id og CPA-id er obligatoriske. Test ved å kalle fagsystem
data class AuthData(val herId: String, val orgnummer: String, val cpaId: String)

// input er payload + messageId. Vi kan evt generere en convId, eller bruke messageId som convId
data class InputTrekkopplysning(val conversationId: String, val messageId: String, val payload: String)

// payload skal følge MsgHead-skjema, dvs inneholde 1 (todo flere?) Document
fun createEIFellesFormat_Trekkopplysning(inputTrekkopplysning: InputTrekkopplysning, authData: AuthData): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk(authData, inputTrekkopplysning.conversationId, inputTrekkopplysning.messageId)
        msgHead = unmarshal(inputTrekkopplysning.payload, MsgHead::class.java)
        val doc = msgHead.document
        if (doc.isEmpty()) {
            log.info("No documents in msgHead")
        } else {
            log.info("Docs: " + doc.size)
            val firstDoc = doc.first()
            val firstRefDoc = firstDoc.refDoc
            val firstContent = firstRefDoc.content.any
            if (firstContent.isEmpty()) {
                log.info("No content in refDoc")
            } else {
                log.info("Content: " + firstContent.size)
                val firstContentAny = firstContent.first()
                log.info("Content: " + firstContentAny.toString())
            }
        }
    }

const val TREKKOPPLYSNING_SERVICE = "Trekkopplysning"
const val TREKKOPPLYSNING_INPUT_ROLE = "Fordringshaver"
const val TREKKOPPLYSNING_INPUT_ACTION = "Innmelding"

internal fun createFellesFormatMottakEnhetBlokk(authData: AuthData, conversationId: String, messageId: String): EIFellesformat.MottakenhetBlokk {
    return fellesFormatFactory.createEIFellesformatMottakenhetBlokk().apply {
        ebXMLSamtaleId = conversationId
        ebAction = TREKKOPPLYSNING_INPUT_ACTION
        ebService = TREKKOPPLYSNING_SERVICE
        ebRole = TREKKOPPLYSNING_INPUT_ROLE
        herIdentifikator = authData.herId
        orgNummer = authData.orgnummer
        avsender = authData.orgnummer
        mottattDatotid = Instant.now().toXmlGregorianCalendar()
        ediLoggId = messageId
        meldingsType = "xml"
        partnerReferanse = authData.cpaId
        avsenderRef = ""
    }
}

fun Instant.toXmlGregorianCalendar() = DatatypeFactory.newInstance().newXMLGregorianCalendar(
    GregorianCalendar().apply { this.setTimeInMillis(this@toXmlGregorianCalendar.toEpochMilli()) }
)
