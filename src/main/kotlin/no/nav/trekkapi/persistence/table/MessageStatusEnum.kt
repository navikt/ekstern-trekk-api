package no.nav.trekkapi.persistence.table

enum class MessageStatusEnum(val description: String) {
    BEING_PROCESSED("Melding mottatt og sendt til behandling"),
    ACCEPTED("Melding ferdig behandlet"),
    REJECTED("Melding behandlet, ikke akseptert") ;
}
