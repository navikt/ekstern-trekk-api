package no.nav.trekkapi.innmelding

import kotlinx.serialization.Serializable
import no.nav.trekkapi.persistence.TrekkInnmeldingRepository
import no.nav.trekkapi.persistence.table.MessageStatusEnum
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class InnrapporteringStatus(val status: String, val description: String? = null)

fun underBehandling(innsendt: Instant) = InnrapporteringStatus(
    MessageStatusEnum.BEING_PROCESSED.description,
    "Sendt inn $innsendt"
)
fun akseptert(kvitteringMottatt: Instant) = InnrapporteringStatus(
    MessageStatusEnum.ACCEPTED.description,
    "Kvittering mottatt $kvitteringMottatt"
)
fun avvist(beskrivelse: String) = InnrapporteringStatus(
    MessageStatusEnum.REJECTED.description,
    beskrivelse
)

class TrekkInnmeldingService(val innrapporteringRepository: TrekkInnmeldingRepository, val trekkInnmeldingModel: TrekkInnmeldingModel = TrekkInnmeldingModel()) {
    suspend fun alreadyRegistered(orgnr: String, id: String): Boolean {
        return innrapporteringRepository.findNewestStatus(orgnr, id) != null
    }

    suspend fun getStatus(orgnr: String, id: String): InnrapporteringStatus? {
        return innrapporteringRepository.findNewestStatus(orgnr, id)
    }

    suspend fun register(orgnr: String, body: String): String {
        val id: String = Uuid.random().toString()
        register(orgnr, id, body)
        return id
    }

// todo skal det uansett lagres noe i eventmgr, eller er det kun for emottak-prosesser ?
    suspend fun register(orgnr: String, id: String, body: String) {
        // Lag objektet som skal videresendes, bruk (orgnr + id) som unik ID inni objektet
        val fellesFormat = trekkInnmeldingModel.buildTrekkInnmelding_FellesFormat(orgnr, id, body)

        // Send objektet til fagsystem (topic, MQ-kø)
        // todo publish, som i ebms-async eller ebms-send-in

        innrapporteringRepository.register(orgnr, id)

        // Hvis noe gikk galt eller timeout: exception
    }
}
