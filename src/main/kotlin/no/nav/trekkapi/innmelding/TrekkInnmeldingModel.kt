package no.nav.trekkapi.innmelding

import no.nav.emottak.utils.common.model.Addressing
import no.nav.emottak.utils.common.model.EbmsProcessing
import no.nav.emottak.utils.common.model.Party
import no.nav.emottak.utils.common.model.PartyId
import no.nav.emottak.utils.common.model.SendInRequest

class TrekkInnmeldingModel {
    // Holder rede på "formatene" i
    // objekter som skal sendes i retning fagsystem (SendInRequest eller Fellesformat)
    // og responsene som kommer tilbake (Fellesformat)

    fun buildTrekkInnmelding_SendInRequest(orgnr: String, id: String, payload: String): SendInRequest {
        // todo fyll ut IDer etc
        return SendInRequest(
            "messageId",
            "conversationId",
            "payloadId",
            "".toByteArray(),
            Addressing(
                Party(listOf(PartyId("HER", "123")), "role"),
                Party(listOf(PartyId("HER", "456")), "role"),
                "service",
                "action"
            ),
            "cpaId",
            EbmsProcessing(),
            null,
            "requestId",
            null
        )
    }

    fun buildTrekkInnmelding_FellesFormat(orgnr: String, id: String, payload: String): String {
        // todo fyll ut Fellesformat a la send-in, NB må få inn XSD-imports
        return ""
    }

    fun avvist(fellesFormatRespons: String): Boolean {
        return "Avvisning" == ebAction(fellesFormatRespons)
    }

    fun akseptert(fellesFormatRespons: String): Boolean {
        return "Kvittering" == ebAction(fellesFormatRespons)
    }

    fun hentAvvisningsBeskrivelse(fellesFormatRespons: String): String {
        return apiRec_Error(fellesFormatRespons)
    }

    fun ebAction(fellesFormatRespons: String): String {
        return ""
    }

    fun apiRec_Error(fellesFormatRespons: String): String {
        return ""
    }
}
