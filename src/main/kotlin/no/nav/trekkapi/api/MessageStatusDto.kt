package no.nav.trekkapi.api

import kotlinx.serialization.Serializable
import no.nav.trekkapi.persistence.table.MessageStatusEnum
import no.nav.trekkapi.util.InstantSerializer
import java.time.Instant

@Serializable
data class MessageStatusDto(
    val id: String,
    val status: MessageStatusEnum,
    @Serializable(with = InstantSerializer::class)
    val submittedAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
    val rejectionDescription: String? = null,
    val rejectionCode: String? = null,
    val responseXml: String? = null,
)

fun pending(
    id: String,
    submittedAt: Instant,
) = MessageStatusDto(
    id = id,
    status = MessageStatusEnum.PENDING,
    submittedAt = submittedAt,
    updatedAt = submittedAt,
)

fun accepted(
    id: String,
    submittedAt: Instant,
    receivedAt: Instant,
    responseXml: String? = null,
) = MessageStatusDto(
    id = id,
    status = MessageStatusEnum.ACCEPTED,
    submittedAt = submittedAt,
    updatedAt = receivedAt,
    responseXml = responseXml,
)

fun rejected(
    id: String,
    submittedAt: Instant,
    receivedAt: Instant,
    description: String,
    code: String? = null,
    responseXml: String? = null,
) = MessageStatusDto(
    id = id,
    status = MessageStatusEnum.REJECTED,
    submittedAt = submittedAt,
    updatedAt = receivedAt,
    rejectionDescription = description,
    rejectionCode = code,
    responseXml = responseXml,
)
