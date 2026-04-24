package no.nav.trekkapi.innmelding

import java.time.LocalDateTime

class TrekkInnmeldingRepository {

    // Denne klassen vil enten bruke EventManager eller egen lokal DB som backend

    fun register(orgnr: String, id: String) {
        // Enten registrer event med riktig ID for søkene nedenfor,
        // eller lagre i lokal DB (id, innsendt tidspunkt, status UnderBehandling)
    }

    fun registerResponse(orgnr: String, id: String, akseptert: Boolean, beskrivelse: String? = null) {
        // Enten registrer event med riktig ID for søkene nedenfor og akseptert/avvist + beskrivelse i eventData,
        // eller lagre i lokal DB (id, kvittert tidspunkt, status akseoptert/avvist, evt beskrivelse)
    }

    fun findNewestStatus(orgnr: String, id: String): InnrapporteringStatus? {
        val status: TrekkStatus = findStatus(orgnr, id)
        when (status) {
            TrekkStatus.UnderBehandling -> {
                val innsendt: LocalDateTime = findInnsendt(orgnr, id)
                return underBehandling(innsendt)
            }
            TrekkStatus.Akseptert -> {
                val kvittert: LocalDateTime = findKvittert(orgnr, id)
                return akseptert(kvittert)
            }
            TrekkStatus.Avvist -> {
                val beskrivelse: String = findAvvistBeskrivelse(orgnr, id)
                return avvist(beskrivelse)
            }
        }
    }

    private fun findAvvistBeskrivelse(orgnr: String, id: String): String {
        // finn beskr i eventData for "svar mottatt" event, eller rett fra lokal DB
        return "Avvist av tjenesten"
    }

    private fun findInnsendt(orgnr: String, id: String): LocalDateTime {
        // finn tidspunkt for "sendt" event, eller rett fra lokal DB
        return LocalDateTime.now()
    }

    private fun findKvittert(orgnr: String, id: String): LocalDateTime {
        // finn tidspunkt for "svar mottatt" event, eller rett fra lokal DB
        return LocalDateTime.now()
    }

    private fun findStatus(orgnr: String, id: String): TrekkStatus {
        // finn nyeste event med gitt ID, eller status i lokal DB
        return TrekkStatus.UnderBehandling
    }
}
