package no.nav.trekkapi.persistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.trekkapi.api.MessageStatus
import no.nav.trekkapi.api.accepted
import no.nav.trekkapi.api.pending
import no.nav.trekkapi.api.rejected
import no.nav.trekkapi.innmelding.MessageStatusRow
import no.nav.trekkapi.persistence.table.MessageStatusEnum
import no.nav.trekkapi.persistence.table.MessageStatusTable
import no.nav.trekkapi.persistence.table.MessageStatusTable.latestStatus
import no.nav.trekkapi.persistence.table.MessageStatusTable.messageId
import no.nav.trekkapi.persistence.table.MessageStatusTable.orgNr
import no.nav.trekkapi.persistence.table.MessageStatusTable.processedAt
import no.nav.trekkapi.persistence.table.MessageStatusTable.responseCode
import no.nav.trekkapi.persistence.table.MessageStatusTable.responseDescription
import no.nav.trekkapi.persistence.table.MessageStatusTable.responseReceivedAt
import no.nav.trekkapi.util.nowOsloToInstant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.temporal.ChronoUnit

class TrekkInnmeldingRepository(private val database: Database) {

    suspend fun register(orgnr: String, id: String) {
        insert(orgnr, id)
    }

    suspend fun registerResponse(orgnr: String, id: String, akseptert: Boolean, beskrivelse: String? = null, kode: String? = null) {
        val status = if (akseptert) MessageStatusEnum.ACCEPTED else MessageStatusEnum.REJECTED
        update(orgnr, id, status, description = beskrivelse, code = kode)
    }

    suspend fun findNewestStatus(orgnr: String, id: String): MessageStatus? {
        val row: MessageStatusRow = findStatus(orgnr, id) ?: return null
        return when (row.latestStatus) {
            MessageStatusEnum.PENDING -> pending(row.messageId, row.processedAt)
            MessageStatusEnum.ACCEPTED -> accepted(row.messageId, row.processedAt, row.responseReceivedAt!!)
            MessageStatusEnum.REJECTED -> rejected(row.messageId, row.processedAt, row.responseReceivedAt!!, row.responseDescription!!, row.responseCode)
        }
    }

    private suspend fun findStatus(orgnr: String, id: String): MessageStatusRow? {
        return get(orgnr, id)
    }

    suspend fun insert(orgnr: String, id: String, now: Instant = nowOsloToInstant().truncatedTo(ChronoUnit.MICROS)): Boolean = withContext(Dispatchers.IO) {
        transaction(database.db) {
            MessageStatusTable.insertIgnore {
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
        code: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        transaction(database.db) {
            val updatedRows = MessageStatusTable.update({
                (messageId eq id) and (orgNr eq orgnr)
            }) {
                it[latestStatus] = status
                it[responseReceivedAt] = datetime
                it[responseDescription] = description
                it[responseCode] = code
            }
            updatedRows == 1
        }
    }

    suspend fun get(orgnr: String, id: String): MessageStatusRow? = withContext(Dispatchers.IO) {
        transaction(database.db) {
            MessageStatusTable
                .select(orgNr, messageId, processedAt, latestStatus, responseReceivedAt, responseDescription, responseCode)
                .where { (messageId eq id) and (orgNr eq orgnr) }
                .mapNotNull {
                    MessageStatusRow(
                        orgNr = it[orgNr],
                        messageId = it[messageId],
                        processedAt = it[processedAt],
                        latestStatus = it[latestStatus],
                        responseReceivedAt = it[responseReceivedAt],
                        responseDescription = it[responseDescription],
                        responseCode = it[responseCode]
                    )
                }
                .singleOrNull()
        }
    }
}
