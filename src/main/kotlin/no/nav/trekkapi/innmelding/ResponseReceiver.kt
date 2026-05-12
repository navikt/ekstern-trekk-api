package no.nav.trekkapi.innmelding

import io.github.nomisRev.kafka.receiver.AutoOffsetReset
import io.github.nomisRev.kafka.receiver.KafkaReceiver
import io.github.nomisRev.kafka.receiver.ReceiverSettings
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import no.nav.trekkapi.configuration.config
import no.nav.trekkapi.configuration.toProperties
import no.nav.trekkapi.persistence.TrekkInnmeldingRepository
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

private val log = LoggerFactory.getLogger("no.nav.trekkapi.ResponseReceiver")

suspend fun startResponseReceiver(
    topic: String,
    groupId: String,
    trekkInnmeldingModel: TrekkInnmeldingModel,
    trekkInnmeldingRepository: TrekkInnmeldingRepository
) {
    val config = config()
    log.info("Starting response receiver on topic $topic")
    val receiverSettings: ReceiverSettings<String?, ByteArray> =
        ReceiverSettings(
            bootstrapServers = config.kafka.bootstrapServers,
            keyDeserializer = StringDeserializer(),
            valueDeserializer = ByteArrayDeserializer(),
            groupId = groupId,
            autoOffsetReset = AutoOffsetReset.Latest,
            pollTimeout = 10.seconds,
            properties = config.kafka.toProperties()
        )

    KafkaReceiver(receiverSettings)
        .receive(topic)
        .map { record ->
            log.debug("Processing record: {}", record)
            val fellesFormat = trekkInnmeldingModel.parseTrekkInnmeldingResponse_FellesFormat(record.value())
            val (orgnummer, meldingsid) = trekkInnmeldingModel.orgnrOgMeldingsId(fellesFormat)
            val isAccepted = trekkInnmeldingModel.isAccepted(fellesFormat)
            if (isAccepted) {
                trekkInnmeldingRepository.registerResponse(orgnummer, meldingsid, true, null)
                log.info("Response på trekkopplysningsmelding med orgnr $orgnummer, meldingsId $meldingsid er lagret, status: akseptert")
            } else {
                val isRejected = trekkInnmeldingModel.isRejected(fellesFormat)
                if (isRejected) {
                    val beskrivelse = trekkInnmeldingModel.getRejectionDescription(fellesFormat)
                    val kode = trekkInnmeldingModel.getRejectionCode(fellesFormat)
                    trekkInnmeldingRepository.registerResponse(orgnummer, meldingsid, false, beskrivelse, kode)
                    log.info("Response på trekkopplysningsmelding med orgnr $orgnummer, meldingsId $meldingsid er lagret, status: avvist, beskrivelse: $beskrivelse")
                } else {
                    log.error("Ukjent status for trekkopplysningsmelding med orgnr $orgnummer, meldingsId $meldingsid")
                }
            }
            record.offset.acknowledge()
        }.collect()
}
