package no.nav.trekkapi.innmelding

import no.nav.trekkapi.api.MessageStatus
import no.nav.trekkapi.configuration.TrekkopplysningMq
import no.nav.trekkapi.fellesformat.marshalTrekkopplysning
import no.nav.trekkapi.log
import no.nav.trekkapi.persistence.TrekkInnmeldingRepository
import kotlin.uuid.Uuid

class TrekkInnmeldingService(
    trekkopplysningMq: TrekkopplysningMq,
    val innrapporteringRepository: TrekkInnmeldingRepository,
    val trekkInnmeldingModel: TrekkInnmeldingModel = TrekkInnmeldingModel(),
    val jmSclient: JmsClient = JmsClient(trekkopplysningMq),
    val queue: String = trekkopplysningMq.queue,
) {
    suspend fun alreadyRegistered(
        orgnr: String,
        id: String,
    ): Boolean = innrapporteringRepository.findNewestStatus(orgnr, id) != null

    suspend fun getStatus(
        orgnr: String,
        id: String,
    ): MessageStatus? = innrapporteringRepository.findNewestStatus(orgnr, id)

    suspend fun register(
        orgnr: String,
        body: String,
    ): String {
        val id: String = Uuid.random().toString()
        register(orgnr, id, body)
        return id
    }

    suspend fun register(
        orgnr: String,
        id: String,
        body: String,
    ) {
        val fellesformat = trekkInnmeldingModel.buildTrekkInnmeldingAsFellesFormat(orgnr, id, body)
//        val fellesformatXmlBuilder = FellesformatXmlBuilder()
//        val messageBody = fellesformatXmlBuilder.buildXml(fellesformat.mottakenhetBlokk, body.toByteArray())
        val messageBody = marshalTrekkopplysning(fellesformat)

        log.debug("Sending in trekkopplysning with body: $messageBody")
        jmSclient.sendMessage(queue, messageBody)

        innrapporteringRepository.register(orgnr, id)
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }
}
