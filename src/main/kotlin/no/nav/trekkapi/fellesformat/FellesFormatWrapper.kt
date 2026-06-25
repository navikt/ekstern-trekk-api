package no.nav.trekkapi.fellesformat

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val EIFF_NAMESPACE = "http://www.trygdeetaten.no/xml/eiff/1/"

const val TREKKOPPLYSNING_SERVICE = "Trekkopplysning"
const val TREKKOPPLYSNING_INPUT_ROLE = "Fordringshaver"
const val TREKKOPPLYSNING_INPUT_ACTION = "Innmelding"
const val TREKKAPI_PARTHER_REF = "EKSTERN_TREKK_API"

// todo må evt. finne disse via auth/maskinporten. Vet ikke om HER-id er obligatorisk. Test ved å kalle fagsystem
data class AuthData(
    val herId: String,
    val orgnummer: String,
)

data class InputTrekkopplysning(
    val conversationId: String,
    val messageId: String,
    val payload: String,
)

private val mottattDatotidFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

// Bygger EI_fellesformat-konvolutten ved å pakke den innkommende payloaden (MsgHead) ordrett inn,
// slik at den ikke re-serialiseres og det gamle eMottak-formatet bevares nøyaktig.
fun createEIFellesFormatTrekkopplysning(
    inputTrekkopplysning: InputTrekkopplysning,
    authData: AuthData,
    timestamp: Instant,
): String {
    val payload = inputTrekkopplysning.payload.stripXmlProlog().trim()
    return buildString {
        append("<?xml version='1.0' encoding='UTF-8'?>\n")
        append("<EI_fellesformat xmlns=\"$EIFF_NAMESPACE\">\n")
        append(payload)
        append("\n")
        append(
            createFellesFormatMottakEnhetBlokk(
                authData,
                inputTrekkopplysning.conversationId,
                inputTrekkopplysning.messageId,
                timestamp,
            ),
        )
        append("</EI_fellesformat>")
    }
}

// MottakenhetBlokk-attributtene skrives i en helt spesifikk rekkefølge fordi mottakerne forventer
// XML-en "EKSAKT som kodet under". Spesielt SKAL ebAction, ebRole og ebService komme i denne rekkefølgen.
internal fun createFellesFormatMottakEnhetBlokk(
    authData: AuthData,
    conversationId: String,
    messageId: String,
    timestamp: Instant,
): String =
    buildString {
        append("<MottakenhetBlokk")
        appendXmlAttribute("ediLoggId", messageId)
        appendXmlAttribute("avsender", authData.orgnummer)
        appendXmlAttribute("ebXMLSamtaleId", conversationId)
        appendXmlAttribute("meldingsType", "xml")
        appendXmlAttribute("avsenderRef", "")
        appendXmlAttribute("mottattDatotid", timestamp.toMottattDatotid())
        appendXmlAttribute("orgNummer", authData.orgnummer)
        appendXmlAttribute("partnerReferanse", TREKKAPI_PARTHER_REF)
        appendXmlAttribute("herIdentifikator", authData.herId)
        appendXmlAttribute("ebAction", TREKKOPPLYSNING_INPUT_ACTION)
        appendXmlAttribute("ebRole", TREKKOPPLYSNING_INPUT_ROLE)
        appendXmlAttribute("ebService", TREKKOPPLYSNING_SERVICE)
        append("/>")
    }

private fun StringBuilder.appendXmlAttribute(
    name: String,
    value: String,
) {
    append(" ")
    append(name)
    append("=\"")
    append(value.xmlEscapeAttribute())
    append("\"")
}

private fun String.xmlEscapeAttribute(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

private val xmlPrologRegex = Regex("""^\uFEFF?\s*<\?xml.*?\?>""", RegexOption.DOT_MATCHES_ALL)

internal fun String.stripXmlProlog(): String = xmlPrologRegex.replace(this, "")

fun Instant.toMottattDatotid(): String = OffsetDateTime.ofInstant(this, ZoneId.systemDefault()).format(mottattDatotidFormatter)
