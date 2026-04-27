package no.nav.trekkapi.persistence.table

enum class MessageStatusEnum(val dbValue: String, val description: String) {
    BEING_PROCESSED("Under behandling", "Melding mottatt og sendt til prosessering"),
    ACCEPTED("Akseptert", "Melding ferdig behandlet"),
    REJECTED("Avvist", "Melding behandlet, ikke akseptert") ;

    companion object {
        fun fromDbValue(value: String): MessageStatusEnum {
            return entries.find { it.dbValue == value }
                ?: throw IllegalArgumentException("Unknown message status: $value")
        }
    }

    override fun toString(): String {
        return dbValue
    }
}
