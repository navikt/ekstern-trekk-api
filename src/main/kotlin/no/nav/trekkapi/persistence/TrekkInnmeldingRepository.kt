package no.nav.trekkapi.persistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.emottak.utils.common.nowOsloToInstant
import no.nav.trekkapi.innmelding.InnrapporteringStatus
import no.nav.trekkapi.innmelding.MessageStatus
import no.nav.trekkapi.innmelding.akseptert
import no.nav.trekkapi.innmelding.avvist
import no.nav.trekkapi.innmelding.buildDbId
import no.nav.trekkapi.innmelding.underBehandling
import no.nav.trekkapi.persistence.table.MessageStatusEnum
import no.nav.trekkapi.persistence.table.MessageStatusTable
import no.nav.trekkapi.persistence.table.MessageStatusTable.latestStatus
import no.nav.trekkapi.persistence.table.MessageStatusTable.messageId
import no.nav.trekkapi.persistence.table.MessageStatusTable.processedAt
import no.nav.trekkapi.persistence.table.MessageStatusTable.responseDescription
import no.nav.trekkapi.persistence.table.MessageStatusTable.responseReceivedAt
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.temporal.ChronoUnit

// todo loag test for denne
class TrekkInnmeldingRepository(private val database: Database) {

    suspend fun register(orgnr: String, id: String) {
        val dbId = buildDbId(orgnr, id)
        insert(dbId)
    }

    suspend fun registerResponse(dbId: String, akseptert: Boolean, beskrivelse: String? = null) {
        val status = if (akseptert) MessageStatusEnum.ACCEPTED else MessageStatusEnum.REJECTED
        update(dbId, status, description = beskrivelse)
    }

    suspend fun findNewestStatus(orgnr: String, id: String): InnrapporteringStatus? {
        val messageStatus: MessageStatus? = findStatus(orgnr, id)
        if (messageStatus == null) return null
        when (messageStatus.latestStatus) {
            MessageStatusEnum.BEING_PROCESSED -> {
                return underBehandling(messageStatus.processedAt)
            }
            MessageStatusEnum.ACCEPTED -> {
                return akseptert(messageStatus.responseReceivedAt!!)
            }
            MessageStatusEnum.REJECTED -> {
                return avvist(messageStatus.responseDescription!!)
            }
        }
    }

    private suspend fun findStatus(orgnr: String, id: String): MessageStatus? {
        val dbId = buildDbId(orgnr, id)
        return get(dbId)
    }

    suspend fun insert(id: String, now: Instant = nowOsloToInstant().truncatedTo(ChronoUnit.MICROS)): Boolean = withContext(Dispatchers.IO) {
        transaction(database.db) {
            MessageStatusTable.insertIgnore {
                it[messageId] = id
                it[processedAt] = now
                it[latestStatus] = MessageStatusEnum.BEING_PROCESSED
            }.insertedCount == 1
        }
    }

    suspend fun update(
        id: String,
        status: MessageStatusEnum,
        datetime: Instant = nowOsloToInstant().truncatedTo(ChronoUnit.MICROS),
        description: String?
    ): Boolean = withContext(Dispatchers.IO) {
        transaction(database.db) {
            val updatedRows = MessageStatusTable.update({
                messageId eq id
            }) {
                it[latestStatus] = status
                it[responseReceivedAt] = datetime
                it[responseDescription] = description
            }
            updatedRows == 1
        }
    }

    suspend fun get(id: String): MessageStatus? = withContext(Dispatchers.IO) {
        transaction(database.db) {
            MessageStatusTable
                .select(messageId, processedAt, latestStatus, responseReceivedAt, responseDescription)
                .where { messageId eq id }
                .mapNotNull {
                    MessageStatus(
                        messageId = it[messageId],
                        processedAt = it[processedAt],
                        latestStatus = it[latestStatus],
                        responseReceivedAt = it[responseReceivedAt],
                        responseDescription = it[responseDescription]
                    )
                }
                .singleOrNull()
        }
    }
}
