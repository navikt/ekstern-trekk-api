package no.nav.trekkapi.innmelding

import io.ktor.utils.io.core.toByteArray
import no.nav.trekkapi.api.MessageStatusDto
import no.nav.trekkapi.configuration.TrekkopplysningMq
import no.nav.trekkapi.fellesformat.FellesformatXmlBuilder
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
    ): MessageStatusDto? = innrapporteringRepository.findNewestStatus(orgnr, id)

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
        val fellesformatXmlBuilder = FellesformatXmlBuilder()
        val messageBody = fellesformatXmlBuilder.buildXml(fellesformat.mottakenhetBlokk, body.toByteArray())

        log.debug("Sending in trekkopplysning with body: $messageBody")
        jmSclient.sendMessage(queue, messageBody)

        if (!innrapporteringRepository.register(orgnr, id, messageBody)) {
            log.warn("Inserted count from DB for trekkopplysning (messageId: '$id', orgnr: '$orgnr') was not 1 as expected")
        }
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }
}
