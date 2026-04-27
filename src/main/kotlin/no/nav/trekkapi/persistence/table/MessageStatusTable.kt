package no.nav.trekkapi.persistence.table

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.postgresql.util.PGobject

object MessageStatusTable : Table("message_status") {
    val messageId: Column<String> = varchar("message_id", 256)
    val processedAt: Column<java.time.Instant> = timestamp("processed_at")
        .defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp)
    val latestStatus: Column<MessageStatusEnum> = messageStatusEnumeration("latest_status")
    val responseReceivedAt: Column<java.time.Instant?> = timestamp("response_at").nullable()
    val responseDescription: Column<String?> = varchar("response_description", 256).nullable()

    override val primaryKey = PrimaryKey(messageId)
}

fun Table.messageStatusEnumeration(name: String) = customEnumeration(
    name = name,
    sql = "message_status",
    fromDb = { MessageStatusEnum.fromDbValue(it.toString()) },
    toDb = {
        PGobject().apply {
            type = "message_status"
            value = it.dbValue
        }
    }
)
