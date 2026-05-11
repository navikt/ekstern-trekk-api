package no.nav.trekkapi.persistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.trekkapi.innmelding.InnrapporteringStatus
import no.nav.trekkapi.innmelding.MessageStatus
import no.nav.trekkapi.innmelding.akseptert
import no.nav.trekkapi.innmelding.avvist
import no.nav.trekkapi.innmelding.underBehandling
import no.nav.trekkapi.persistence.table.MessageStatusEnum
import no.nav.trekkapi.persistence.table.MessageStatusTable
import no.nav.trekkapi.persistence.table.MessageStatusTable.latestStatus
import no.nav.trekkapi.persistence.table.MessageStatusTable.messageId
import no.nav.trekkapi.persistence.table.MessageStatusTable.orgNr
import no.nav.trekkapi.persistence.table.MessageStatusTable.processedAt
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

    suspend fun registerResponse(orgnr: String, id: String, akseptert: Boolean, beskrivelse: String? = null) {
        val status = if (akseptert) MessageStatusEnum.ACCEPTED else MessageStatusEnum.REJECTED
        update(orgnr, id, status, description = beskrivelse)
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
        return get(orgnr, id)
    }

    suspend fun insert(orgnr: String, id: String, now: Instant = nowOsloToInstant().truncatedTo(ChronoUnit.MICROS)): Boolean = withContext(Dispatchers.IO) {
        transaction(database.db) {
            MessageStatusTable.insertIgnore {
                it[orgNr] = orgnr
                it[messageId] = id
                it[processedAt] = now
                it[latestStatus] = MessageStatusEnum.BEING_PROCESSED
            }.insertedCount == 1
        }
    }

    suspend fun update(
        orgnr: String,
        id: String,
        status: MessageStatusEnum,
        datetime: Instant = nowOsloToInstant().truncatedTo(ChronoUnit.MICROS),
        description: String?
    ): Boolean = withContext(Dispatchers.IO) {
        transaction(database.db) {
            val updatedRows = MessageStatusTable.update({
                (messageId eq id) and (orgNr eq orgnr)
            }) {
                it[latestStatus] = status
                it[responseReceivedAt] = datetime
                it[responseDescription] = description
            }
            updatedRows == 1
        }
    }

    suspend fun get(orgnr: String, id: String): MessageStatus? = withContext(Dispatchers.IO) {
        transaction(database.db) {
            MessageStatusTable
                .select(orgNr, messageId, processedAt, latestStatus, responseReceivedAt, responseDescription)
                .where { (messageId eq id) and (orgNr eq orgnr) }
                .mapNotNull {
                    MessageStatus(
                        orgNr = it[orgNr],
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
