package no.nav.trekkapi.persistence

import com.zaxxer.hikari.HikariConfig
import no.nav.trekkapi.configuration.Config
import no.nav.trekkapi.configuration.config

const val MESSAGE_STATUS_DB_NAME = "ekstern-trekk-api-db"

val messageStatusDbConfig = lazy { configure("user") }

// Dette er kopiert fra event-manager, tror ikke vi trenger egen admin-user. Slå sammen hvis OK
// val messageStatusMigrationConfig = lazy { configure("admin") }
val messageStatusMigrationConfig = lazy { configure("user") }

fun configure(role: String): HikariConfig {
    val config: Config = config()
    val maxPoolSizeForUser = config.database.maxConnectionPoolSizeForUser.value
    val maxPoolSizeForAdmin = config.database.maxConnectionPoolSizeForAdmin.value

    val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = config.database.jdbcUrl
            driverClassName = "org.postgresql.Driver"
            this.username = config.database.jdbcUser
            this.password = config.database.jdbcPassword
            this.maximumPoolSize = maxPoolSizeForUser
            if (role == "admin") {
                this.maximumPoolSize = maxPoolSizeForAdmin
            }
        }

    return hikariConfig
}
