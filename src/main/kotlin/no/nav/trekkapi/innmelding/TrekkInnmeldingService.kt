package no.nav.trekkapi.innmelding

import no.nav.trekkapi.api.MessageStatusDto
import no.nav.trekkapi.configuration.TrekkopplysningMq
import no.nav.trekkapi.fellesformat.marshalTrekkopplysning
import no.nav.trekkapi.log
import no.nav.trekkapi.persistence.TrekkInnmeldingRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

    suspend fun listLast(length: Int): String {
        val rows = innrapporteringRepository.getLast(length)
        val htmlPrologue =
            "<html><body><table border=\"1\">" +
                "    <tr>" +
                "        <th>Message ID</th>" +
                "        <th>Orgnr</th>" +
                "        <th>Processed at</th>" +
                "        <th>Processing status</th>" +
                "        <th>Rejection code</th>" +
                "        <th>Rejection reason</th>" +
                "        <th>Response received at</th>" +
                "    </tr>"
        val htmlEpilogue = "</table></body></html>"
        var tableHtml = ""
        for (row in rows) {
            tableHtml = tableHtml + "<tr><td>${row.messageId}</td>" +
                "<td>${row.orgNr}</td>" +
                "<td>${formatTs(row.processedAt)}</td>" +
                "<td>${row.latestStatus}</td>" +
                "<td>${blankIfNull(row.responseCode)}</td>" +
                "<td>${blankIfNull(row.responseDescription)}</td>" +
                "<td>${formatTs(row.responseReceivedAt)}</td></tr>"
        }
        return htmlPrologue + tableHtml + htmlEpilogue
    }

    private fun formatTs(processedAt: Instant?): String {
        if (processedAt == null) return ""
        val dt = LocalDateTime.ofInstant(processedAt, ZoneId.systemDefault())
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    private fun blankIfNull(s: String?): String {
        if (s == null) return ""
        return s
    }

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

        if (!innrapporteringRepository.register(orgnr, id, messageBody)) {
            log.warn("Inserted count from DB for trekkopplysning (messageId: '$id', orgnr: '$orgnr') was not 1 as expected")
        }
    }

    fun verifyConnection() {
        jmSclient.verifyConnection()
    }
}
