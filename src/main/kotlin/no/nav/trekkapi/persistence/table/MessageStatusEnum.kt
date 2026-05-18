package no.nav.trekkapi.persistence.table

import kotlinx.serialization.Serializable

@Serializable
enum class MessageStatusEnum(
    val description: String,
) {
    PENDING("Melding mottatt og sendt til behandling"),
    ACCEPTED("Melding ferdig behandlet"),
    REJECTED("Melding avvist, se beskrivelse"),
}
