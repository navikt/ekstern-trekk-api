package no.nav.trekkapi.persistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.trekkapi.api.MessageStatusDto
import no.nav.trekkapi.api.accepted
import no.nav.trekkapi.api.pending
import no.nav.trekkapi.api.rejected
import no.nav.trekkapi.fellesformat.identifisering
import no.nav.trekkapi.fellesformat.unmarshalMsgHead
import no.nav.trekkapi.innmelding.MessageStatusRow
import no.nav.trekkapi.log
import no.nav.trekkapi.persistence.table.MessageStatusEnum
import no.nav.trekkapi.persistence.table.MessageStatusTable
import no.nav.trekkapi.persistence.table.MessageStatusTable.debitorId
import no.nav.trekkapi.persistence.table.MessageStatusTable.kreditorTrekkId
import no.nav.trekkapi.persistence.table.MessageStatusTable.latestStatus
import no.nav.trekkapi.persistence.table.MessageStatusTable.messageId
import no.nav.trekkapi.persistence.table.MessageStatusTable.navTrekkId
import no.nav.trekkapi.persistence.table.MessageStatusTable.orgNr
import no.nav.trekkapi.persistence.table.MessageStatusTable.processedAt
import no.nav.trekkapi.persistence.table.MessageStatusTable.responseCode
import no.nav.trekkapi.persistence.table.MessageStatusTable.responseDescription
import no.nav.trekkapi.persistence.table.MessageStatusTable.responseReceivedAt
import no.nav.trekkapi.util.nowOsloToInstant
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.temporal.ChronoUnit

class TrekkInnmeldingRepository(
    private val database: Database,
) {
    suspend fun register(
        orgnr: String,
        id: String,
    ): Boolean = insert(orgnr, id)

    suspend fun registerResponse(
        orgnr: String,
        id: String,
        akseptert: Boolean,
        beskrivelse: String? = null,
        kode: String? = null,
        xml: String? = null,
    ): Boolean {
        val status = if (akseptert) MessageStatusEnum.ACCEPTED else MessageStatusEnum.REJECTED
        val identifisering = if (akseptert && xml != null) extractIdentifisering(xml) else null
        return update(
            orgnr,
            id,
            status,
            description = beskrivelse,
            code = kode,
            debitorId = identifisering?.debitorId,
            navTrekkId = identifisering?.navTrekkId,
            kreditorTrekkId = identifisering?.kreditorTrekkId,
        )
    }

    suspend fun findNewestStatus(
        orgnr: String,
        id: String,
    ): MessageStatusDto? {
        val row: MessageStatusRow = findStatus(orgnr, id) ?: return null
        return when (row.latestStatus) {
            MessageStatusEnum.PENDING -> pending(row.messageId, row.processedAt)
            MessageStatusEnum.ACCEPTED ->
                accepted(
                    row.messageId,
                    row.processedAt,
                    row.responseReceivedAt!!,
                    debitorId = row.debitorId,
                    navTrekkId = row.navTrekkId,
                    kreditorTrekkId = row.kreditorTrekkId,
                )
            MessageStatusEnum.REJECTED ->
                rejected(
                    row.messageId,
                    row.processedAt,
                    row.responseReceivedAt!!,
                    row.responseDescription!!,
                    row.responseCode,
                )
        }
    }

    // For accepted messages, the XML contains a MsgHead document; only its identifiers are retained.
    private fun extractIdentifisering(responseXml: String): ResponseIdentifisering? =
        runCatching {
            responseXml.unmarshalMsgHead().identifisering()?.let {
                ResponseIdentifisering(it.debitorId?.id, it.navTrekkId, it.kreditorTrekkId)
            }
        }.onFailure { log.warn("Kunne ikke hente Identifisering fra svarmeldingen") }
            .getOrNull()

    private data class ResponseIdentifisering(
        val debitorId: String?,
        val navTrekkId: String?,
        val kreditorTrekkId: String?,
    )

    private suspend fun findStatus(
        orgnr: String,
        id: String,
    ): MessageStatusRow? = get(orgnr, id)

    suspend fun insert(
        orgnr: String,
        id: String,
        now: Instant = nowOsloToInstant().truncatedTo(ChronoUnit.MICROS),
    ): Boolean =
        withContext(Dispatchers.IO) {
            transaction(database.db) {
                MessageStatusTable
                    .insertIgnore {
                        it[orgNr] = orgnr
                        it[messageId] = id
                        it[processedAt] = now
                        it[latestStatus] = MessageStatusEnum.PENDING
                    }.insertedCount == 1
            }
        }

    suspend fun update(
        orgnr: String,
        id: String,
        status: MessageStatusEnum,
        datetime: Instant = nowOsloToInstant().truncatedTo(ChronoUnit.MICROS),
        description: String?,
        code: String? = null,
        debitorId: String? = null,
        navTrekkId: String? = null,
        kreditorTrekkId: String? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            transaction(database.db) {
                val updatedRows =
                    MessageStatusTable.update({
                        (messageId eq id) and (orgNr eq orgnr)
                    }) {
                        it[latestStatus] = status
                        it[responseReceivedAt] = datetime
                        it[responseDescription] = description
                        it[responseCode] = code
                        it[MessageStatusTable.debitorId] = debitorId
                        it[MessageStatusTable.navTrekkId] = navTrekkId
                        it[MessageStatusTable.kreditorTrekkId] = kreditorTrekkId
                    }
                updatedRows == 1
            }
        }

    suspend fun get(
        orgnr: String,
        id: String,
    ): MessageStatusRow? =
        withContext(Dispatchers.IO) {
            transaction(database.db) {
                MessageStatusTable
                    .select(
                        orgNr,
                        messageId,
                        processedAt,
                        latestStatus,
                        responseReceivedAt,
                        responseDescription,
                        responseCode,
                        debitorId,
                        navTrekkId,
                        kreditorTrekkId,
                    ).where { (messageId eq id) and (orgNr eq orgnr) }
                    .mapNotNull {
                        MessageStatusRow(
                            orgNr = it[orgNr],
                            messageId = it[messageId],
                            processedAt = it[processedAt],
                            latestStatus = it[latestStatus],
                            responseReceivedAt = it[responseReceivedAt],
                            responseDescription = it[responseDescription],
                            responseCode = it[responseCode],
                            debitorId = it[debitorId],
                            navTrekkId = it[navTrekkId],
                            kreditorTrekkId = it[kreditorTrekkId],
                        )
                    }.singleOrNull()
            }
        }
}
