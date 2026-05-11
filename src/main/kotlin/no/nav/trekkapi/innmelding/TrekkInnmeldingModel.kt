package no.nav.trekkapi.innmelding

import no.nav.trekkapi.fellesformat.AuthData
import no.nav.trekkapi.fellesformat.InputTrekkopplysning
import no.nav.trekkapi.fellesformat.createEIFellesFormat_Trekkopplysning
import no.nav.trekkapi.fellesformat.unmarshal
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import java.time.Instant

class TrekkInnmeldingModel {
    // Holder rede på "formatene" i
    // objekter som skal sendes i retning fagsystem og responsene som kommer tilbake (Fellesformat)

    fun buildTrekkInnmelding_FellesFormat(orgnr: String, id: String, payload: String, timestamp: Instant? = Instant.now()): EIFellesformat {
        val messageId = id
        val conversationId = messageId
        val inputTrekkopplysning = InputTrekkopplysning(conversationId, messageId, payload)
        val authData = AuthData("", orgnr)
        return createEIFellesFormat_Trekkopplysning(inputTrekkopplysning, authData, timestamp!!)
    }

    fun parseTrekkInnmeldingResponse_FellesFormat(message: ByteArray): EIFellesformat {
        return unmarshal(message.toString(Charsets.UTF_8), EIFellesformat::class.java)
    }

    fun orgnrOgMeldingsId(fellesFormatRespons: EIFellesformat): Pair<String, String> {
        val orgnr = fellesFormatRespons.mottakenhetBlokk.orgNummer
        val meldingsId = fellesFormatRespons.mottakenhetBlokk.ediLoggId
        return Pair(orgnr, meldingsId)
    }

    fun isRejected(fellesFormatRespons: EIFellesformat): Boolean {
        return "Avvisning" == ebAction(fellesFormatRespons)
    }

    fun isAccepted(fellesFormatRespons: EIFellesformat): Boolean {
        return "Kvittering" == ebAction(fellesFormatRespons)
    }

    fun getRejectionDescription(fellesFormatRespons: EIFellesformat): String {
        return apiRec_Error(fellesFormatRespons).dn ?: ""
    }

    fun getRejectionCode(fellesFormatRespons: EIFellesformat): String? {
        return apiRec_Error(fellesFormatRespons).v
    }

    fun ebAction(fellesFormatRespons: EIFellesformat): String {
        return fellesFormatRespons.mottakenhetBlokk.ebAction
    }

    fun apiRec_Error(fellesFormatRespons: EIFellesformat) =
        fellesFormatRespons.appRec.error!![0]!!
}
