package no.nav.trekkapi.innmelding

import no.nav.trekkapi.fellesformat.AuthData
import no.nav.trekkapi.fellesformat.InputTrekkopplysning
import no.nav.trekkapi.fellesformat.createEIFellesFormat_Trekkopplysning
import no.nav.trekkapi.fellesformat.unmarshal
import no.nav.trekkapi.log
import no.trygdeetaten.xml.eiff._1.EIFellesformat

fun buildDbId(orgnr: String, id: String): String {
    return "$orgnr-$id"
}

// Det må finnes en ID (antar ediLoggId) som har fprmat trekkapi-<orgnr>-<UUID>
// Den vil gjenkjennes av respons-router pga. prefix "trekkapi-" og rutes til respons-topic
// DB-id'en finnes da ved å fjerne prefikset.
const val TREKKAPI_PREFIX = "trekkapi-"
fun buildFagsystemId(orgnr: String, id: String): String {
    return TREKKAPI_PREFIX + buildDbId(orgnr, id)
}
fun getDbId(fagsystemId: String): String {
    if (!fagsystemId.startsWith(TREKKAPI_PREFIX)) {
        log.warn("Fagsystem-id $fagsystemId er ikke en trekkapi-id, responsen vil ikke bli registrert riktig")
        return ""
    }
    return fagsystemId.removePrefix(TREKKAPI_PREFIX)
}

class TrekkInnmeldingModel {
    // Holder rede på "formatene" i
    // objekter som skal sendes i retning fagsystem og responsene som kommer tilbake (Fellesformat)

    fun buildTrekkInnmelding_FellesFormat(orgnr: String, id: String, payload: String): EIFellesformat {
        val conversationId = buildFagsystemId(orgnr, id)
        val messageId = conversationId // NB: denne blir brukt som ediLoggId, som igjen brukes av respons-router
        val inputTrekkopplysning = InputTrekkopplysning(conversationId, messageId, payload)
        val authData = AuthData("", orgnr, "")
        return createEIFellesFormat_Trekkopplysning(inputTrekkopplysning, authData)
    }

    fun parseTrekkInnmeldingResponse_FellesFormat(message: ByteArray): EIFellesformat {
        return unmarshal(message.toString(Charsets.UTF_8), EIFellesformat::class.java)
    }

    fun getDbId(fellesFormatRespons: EIFellesformat): String {
        val fagsystemId = fellesFormatRespons.mottakenhetBlokk.ediLoggId
        return getDbId(fagsystemId)
    }

    fun avvist(fellesFormatRespons: EIFellesformat): Boolean {
        return "Avvisning" == ebAction(fellesFormatRespons)
    }

    fun akseptert(fellesFormatRespons: EIFellesformat): Boolean {
        return "Kvittering" == ebAction(fellesFormatRespons)
    }

    fun hentAvvisningsBeskrivelse(fellesFormatRespons: EIFellesformat): String {
        return apiRec_Error(fellesFormatRespons)
    }

    fun ebAction(fellesFormatRespons: EIFellesformat): String {
        return fellesFormatRespons.mottakenhetBlokk.ebAction
    }

    fun apiRec_Error(fellesFormatRespons: EIFellesformat): String {
        val errorElement = fellesFormatRespons.appRec.error
        return errorElement!!.get(0)!!.dn ?: ""
    }
}
