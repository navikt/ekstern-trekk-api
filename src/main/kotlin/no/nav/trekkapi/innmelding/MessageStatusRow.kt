package no.nav.trekkapi.innmelding

import no.nav.trekkapi.persistence.table.MessageStatusEnum
import java.time.Instant

data class MessageStatusRow(
    val orgNr: String,
    val messageId: String,
    val processedAt: Instant,
    val latestStatus: MessageStatusEnum,
    val responseReceivedAt: Instant?,
    val responseDescription: String?,
    val responseCode: String?,
)
