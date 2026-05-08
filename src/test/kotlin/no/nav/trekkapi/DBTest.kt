package no.nav.trekkapi

import com.zaxxer.hikari.HikariConfig
import org.testcontainers.postgresql.PostgreSQLContainer

fun trekkApiPostgres(): PostgreSQLContainer =
    PostgreSQLContainer("postgres:15").apply {
        withUsername("ekstern-trekk-api-db-admin")
        withReuse(true)
        withLabel("app-navn", "ekstern-trekk-api")
        start()
        println(
            "Testdatabasen er startet opp, portnummer: $firstMappedPort, jdbcUrl: jdbc:postgresql://localhost:$firstMappedPort/test, credentials: test og test"
        )
    }

fun PostgreSQLContainer.testConfiguration(): HikariConfig {
    return HikariConfig().apply {
        jdbcUrl = this@testConfiguration.jdbcUrl
        username = this@testConfiguration.username
        password = this@testConfiguration.password
        maximumPoolSize = 5
        minimumIdle = 1
        idleTimeout = 500001
        connectionTimeout = 10000
        maxLifetime = 600001
        initializationFailTimeout = 5000
    }
}
