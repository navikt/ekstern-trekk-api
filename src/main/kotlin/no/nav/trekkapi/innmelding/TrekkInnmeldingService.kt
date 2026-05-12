package no.nav.trekkapi.innmelding

import no.nav.trekkapi.api.MessageStatus
import no.nav.trekkapi.configuration.TrekkopplysningMq
import no.nav.trekkapi.fellesformat.marshalTrekkopplysning
import no.nav.trekkapi.log
import no.nav.trekkapi.persistence.TrekkInnmeldingRepository
import no.nav.trekkapi.util.getEnvVar
import kotlin.uuid.Uuid

class TrekkInnmeldingService(
    trekkopplysningMq: TrekkopplysningMq,
    val innrapporteringRepository: TrekkInnmeldingRepository,
    val trekkInnmeldingModel: TrekkInnmeldingModel = TrekkInnmeldingModel(),
    val jmSclient: JmsClient = JmsClient(trekkopplysningMq),
    val queue: String = trekkopplysningMq.queue
) {
    suspend fun alreadyRegistered(orgnr: String, id: String): Boolean {
        return innrapporteringRepository.findNewestStatus(orgnr, id) != null
    }

    suspend fun getStatus(orgnr: String, id: String): MessageStatus? {
        return innrapporteringRepository.findNewestStatus(orgnr, id)
    }

    suspend fun register(orgnr: String, body: String): String {
        val id: String = Uuid.random().toString()
        register(orgnr, id, body)
        return id
    }

    suspend fun register(orgnr: String, id: String, body: String) {
        val fellesFormat = trekkInnmeldingModel.buildTrekkInnmelding_FellesFormat(orgnr, id, body)
        val messageBody = marshalTrekkopplysning(fellesFormat)

        val doSend = getEnvVar("USE_MQ", "false").toBoolean()
        if (doSend) {
            log.debug("Sending in trekkopplysning with body: " + messageBody)
            jmSclient.sendMessage(queue, messageBody)
        } else {
            log.debug("MQ turned OFF, would send in trekkopplysning with body: " + messageBody)
        }

        innrapporteringRepository.register(orgnr, id)
        // todo Hvis noe gikk galt eller timeout: exception
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }
}
