package no.nav.trekkapi.innmelding

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

// status kan være: "Akseptert", "Avvist", "Under behandling"
enum class TrekkStatus {
    UnderBehandling, Akseptert, Avvist
}
const val TREKK_STATUS_UNDERBEHANDLING = "Under behandling"
const val TREKK_STATUS_AKSEPTERT = "Akseptert"
const val TREKK_STATUS_AVVIST = "Avvist"

@Serializable
data class InnrapporteringStatus(val status: String, val description: String? = null)

fun underBehandling(innsendt: LocalDateTime) = InnrapporteringStatus(
    TREKK_STATUS_UNDERBEHANDLING,
    "Sendt inn $innsendt"
)
fun akseptert(kvitteringMottatt: LocalDateTime) = InnrapporteringStatus(
    TREKK_STATUS_AKSEPTERT,
    "Kvittering mottatt $kvitteringMottatt"
)
fun avvist(beskrivelse: String) = InnrapporteringStatus(
    TREKK_STATUS_AVVIST,
    beskrivelse
)

class TrekkInnmeldingService(val innrapporteringRepository: TrekkInnmeldingRepository, val trekkInnmeldingModel: TrekkInnmeldingModel = TrekkInnmeldingModel()) {
    fun alreadyRegistered(orgnr: String, id: String): Boolean {
        // Sjekk events eller innmelding i lokal DB, med orgnr-id
        return innrapporteringRepository.findNewestStatus(orgnr, id) != null
    }

    fun getStatus(orgnr: String, id: String): InnrapporteringStatus? {
        // Sjekk events eller innmelding i lokal DB, med orgnr-id
        // Hvis funnet, bruk nyeste event eller status i DB som nyeste status, retuner den
        // Hvis ikke funnet: null
        return innrapporteringRepository.findNewestStatus(orgnr, id)
    }

    fun register(orgnr: String, body: String): String {
        val id: String = "" // todo lag ny id
        register(orgnr, id, body)
        return id
    }

    /*
    Innmelding, alternativ 1:
ekstern-trekk-api lager en SendInRequest, som legges på inn-topicen til ebms-send-in.
ebms-send-in er uendret, prosesserer denne helt likt som den gjør for meldinger via epost.

Alternativ 2:
ekstern-trekk-api lager Fellesformat, som legges på MQ-kø til fagsystemet.
variant A: ebms-send-in uendret
variant B: ebms-send-in fjerner kode for å lage Fellesformat og legge på MQ-kø,
får et internt API-endepunkt i ekstern-trekk-api hvor den kan poste nødvendige ID-er og payloaden

Uansett alternativ: ID-er i Fellesformatet må identifisere meldinger som hhv. epost-trekkopplysninger og http-trekkopplysninger.

// todo skal det uansett lagres noe i eventmgr, eller er det kun for emottak-prosesser ?
     */
    fun register(orgnr: String, id: String, body: String) {
        // Lag objektet som skal videresendes, bruk (orgnr + id) som unik ID inni objektet
        val sendInRequest = trekkInnmeldingModel.buildTrekkInnmelding_SendInRequest(orgnr, id, body)
        val fellesFormat = trekkInnmeldingModel.buildTrekkInnmelding_FellesFormat(orgnr, id, body)

        // Send objektet til fagsystem (topic, MQ-kø)
        // todo publish, som i ebms-async eller ebms-send-in

        // Lagre event eller innmelding i lokal DB, med orgnr-id
        innrapporteringRepository.register(orgnr, id)

        // Hvis noe gikk galt eller timeout: exception
    }
}
