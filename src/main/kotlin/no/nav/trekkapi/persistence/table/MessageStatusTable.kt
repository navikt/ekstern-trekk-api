package no.nav.trekkapi.persistence.table

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.postgresql.util.PGobject

object MessageStatusTable : Table("message_status") {

    val messageId: Column<String> = varchar("message_id", 256)

    val orgNr: Column<String> = varchar("org_nr", 32)

    val processedAt: Column<java.time.Instant> = timestamp("processed_at")
        .defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentTimestamp)

    val latestStatus: Column<MessageStatusEnum> = messageStatusEnumeration("latest_status")

    val responseReceivedAt: Column<java.time.Instant?> = timestamp("response_at").nullable()

    val responseDescription: Column<String?> = varchar("response_description", 256).nullable()

    val responseCode: Column<String?> = varchar("response_code", 64).nullable()

    val idempotencyKey: Column<String> = varchar("idempotency_key", 36)

    override val primaryKey = PrimaryKey(messageId)
}

// Postgres ENUM opprettes med 'CREATE TYPE "message_status_type" AS ENUM', og samme verdier som MessageStatusEnum
fun Table.messageStatusEnumeration(name: String) = customEnumeration(
    name = name,
    sql = "message_status",
    fromDb = { value -> MessageStatusEnum.valueOf(value as String) },
    toDb = { PGEnum("message_status_type", it) }
)

// Helper for PostgreSQL native enums
class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}
