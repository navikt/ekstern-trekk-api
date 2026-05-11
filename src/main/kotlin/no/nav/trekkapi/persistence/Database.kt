package no.nav.trekkapi.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.trekkapi.util.getEnvVar
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

class Database(
    dbConfig: HikariConfig
) {
    val dataSource = when (dbConfig) {
        is HikariDataSource -> dbConfig
        else -> HikariDataSource(dbConfig)
    }
    val db = Database.connect(dataSource)
    fun migrate(migrationConfig: HikariConfig) {
        val migrationPath = when (getEnvVar("NAIS_CLUSTER_NAME", "local")) {
            "local", "test" -> "filesystem:src/main/resources/db/migration"
            else -> "filesystem:/app/db/migration"
        }
        Flyway.configure()
            .dataSource(migrationConfig.jdbcUrl, migrationConfig.username, migrationConfig.password)
            .locations(migrationPath)
            .callbackLocations(migrationPath)
            .lockRetryCount(50)
            .load()
            .migrate()
    }
}
