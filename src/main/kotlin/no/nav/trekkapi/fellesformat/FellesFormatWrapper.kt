package no.nav.trekkapi.fellesformat

import no.kith.xmlstds.msghead._2006_05_24.MsgHead
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

fun createEIFellesFormat_Trekkopplysning(inputTrekkopplysning: InputTrekkopplysning, authData: AuthData, timestamp: Instant): EIFellesformat =
    fellesFormatFactory.createEIFellesformat().apply {
        mottakenhetBlokk = createFellesFormatMottakEnhetBlokk(authData, inputTrekkopplysning.conversationId, inputTrekkopplysning.messageId, timestamp)
        msgHead = unmarshal(inputTrekkopplysning.payload, MsgHead::class.java)
    }

const val TREKKOPPLYSNING_SERVICE = "Trekkopplysning"
const val TREKKOPPLYSNING_INPUT_ROLE = "Fordringshaver"
const val TREKKOPPLYSNING_INPUT_ACTION = "Innmelding"

internal fun createFellesFormatMottakEnhetBlokk(authData: AuthData, conversationId: String, messageId: String, timestamp: Instant): EIFellesformat.MottakenhetBlokk {
    return fellesFormatFactory.createEIFellesformatMottakenhetBlokk().apply {
        ebXMLSamtaleId = conversationId
        ebAction = TREKKOPPLYSNING_INPUT_ACTION
        ebService = TREKKOPPLYSNING_SERVICE
        ebRole = TREKKOPPLYSNING_INPUT_ROLE
        herIdentifikator = authData.herId
        orgNummer = authData.orgnummer
        avsender = authData.orgnummer
        mottattDatotid = timestamp.toXmlGregorianCalendar()
        ediLoggId = messageId
        meldingsType = "xml"
        partnerReferanse = authData.cpaId
        avsenderRef = ""
    }
}

fun Instant.toXmlGregorianCalendar() = DatatypeFactory.newInstance().newXMLGregorianCalendar(
    GregorianCalendar().apply { this.setTimeInMillis(this@toXmlGregorianCalendar.toEpochMilli()) }
)
