package no.nav.trekkapi.innmelding

import no.nav.trekkapi.fellesformat.AuthData
import no.nav.trekkapi.fellesformat.InputTrekkopplysning
import no.nav.trekkapi.fellesformat.createEIFellesFormatTrekkopplysning
import no.nav.trekkapi.fellesformat.unmarshal
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import java.time.Instant

class TrekkInnmeldingModel {
    // Holder rede på "formatene" i
    // objekter som skal sendes i retning fagsystem og responsene som kommer tilbake (Fellesformat)

    fun buildTrekkInnmeldingAsFellesFormat(
        orgnr: String,
        id: String,
        payload: String,
        timestamp: Instant = Instant.now(),
    ): EIFellesformat =
        createEIFellesFormatTrekkopplysning(
            InputTrekkopplysning(id, id, payload),
            AuthData("", orgnr),
            timestamp,
        )

    fun parseTrekkInnmeldingResponseAsFellesFormat(message: ByteArray): EIFellesformat =
        unmarshal(message.toString(Charsets.UTF_8), EIFellesformat::class.java)

    fun orgnrOgMeldingsId(fellesFormatRespons: EIFellesformat): Pair<String, String> =
        Pair(fellesFormatRespons.mottakenhetBlokk.orgNummer, fellesFormatRespons.mottakenhetBlokk.ediLoggId)

    fun isRejected(fellesFormatRespons: EIFellesformat): Boolean = "Avvisning" == ebAction(fellesFormatRespons)

    fun isAccepted(fellesFormatRespons: EIFellesformat): Boolean = "Kvittering" == ebAction(fellesFormatRespons)

    fun getRejectionDescription(fellesFormatRespons: EIFellesformat): String = appRecError(fellesFormatRespons).dn ?: ""

    fun getRejectionCode(fellesFormatRespons: EIFellesformat): String? = appRecError(fellesFormatRespons).v

    fun ebAction(fellesFormatRespons: EIFellesformat): String = fellesFormatRespons.mottakenhetBlokk.ebAction

    fun appRecError(fellesFormatRespons: EIFellesformat) = fellesFormatRespons.appRec.error!![0]!!
}
