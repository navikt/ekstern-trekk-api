package no.nav.trekkapi.persistence

import com.zaxxer.hikari.HikariConfig
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// For å kunne opprette DB bare 1 gang for hele testklassen
// Alternativet er å flytte Before/AfterAll funksjonenw til et companion object
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrekkInnmeldingRepositoryTest {

    lateinit var dbContainer: PostgreSQLContainer
    lateinit var db: Database

    @BeforeAll
    fun setup() {
        dbContainer = buildDatabaseContainer()
        dbContainer.start()
        val migrationDb = Database(dbContainer.testConfiguration())
        migrationDb.migrate(migrationDb.dataSource)
        db = Database(dbContainer.testConfiguration(user = "user"))
    }

    @AfterAll
    fun teardown() {
        db.dataSource.close()
        dbContainer.stop()
    }

    @Test
    fun `Verify register() and findNewestStatus()`() = runBlocking {
        val repo = TrekkInnmeldingRepository(db)
        val orgnr = "123451111"
        val id = "theIdOfTheInsertedRecord"
        newSuspendedTransaction {
            repo.register(orgnr, id)
            val status = repo.findNewestStatus(orgnr, id)
            assertEquals("Melding mottatt og sendt til behandling", status!!.status)
            assertTrue(status.description!!.startsWith("Sendt inn"))
            exec("SELECT count(*) FROM message_status") { rs ->
                rs.next()
                assertEquals(1, rs.getInt(1))
            }
            exec("SELECT * FROM message_status") { rs ->
                rs.next()
                assertEquals("123451111-theIdOfTheInsertedRecord", rs.getString("message_id"))
                assertNotNull(rs.getTimestamp("processed_at"))
                assertEquals("BEING_PROCESSED", rs.getString("latest_status"))
                assertNull(rs.getTimestamp("response_at"))
                assertNull(rs.getString("response_description"))
            }
            rollback()
        }
    }

    @Test
    fun `Verify registerAcceptedResponse() and findNewestStatus()`() = runBlocking {
        val repo = TrekkInnmeldingRepository(db)
        val orgnr = "123456789"
        val id = "theIdOfTheInsertedRecord"
        newSuspendedTransaction {
            repo.register(orgnr, id)
            repo.registerResponse("123456789-theIdOfTheInsertedRecord", true)
            val status = repo.findNewestStatus(orgnr, id)
            assertEquals("Melding ferdig behandlet", status!!.status)
            assertTrue(status.description!!.startsWith("Kvittering mottatt"))
            exec("SELECT * FROM message_status") { rs ->
                rs.next()
                assertEquals("123456789-theIdOfTheInsertedRecord", rs.getString("message_id"))
                assertNotNull(rs.getTimestamp("processed_at"))
                assertEquals("ACCEPTED", rs.getString("latest_status"))
                assertNotNull(rs.getTimestamp("response_at"))
                assertNull(rs.getString("response_description"))
            }
            rollback()
        }
    }

    @Test
    fun `Verify registerRejectedResponse() and findNewestStatus()`() = runBlocking {
        val repo = TrekkInnmeldingRepository(db)
        val orgnr = "123456789"
        val id = "theIdOfTheInsertedRecord"
        newSuspendedTransaction {
            repo.register(orgnr, id)
            repo.registerResponse("123456789-theIdOfTheInsertedRecord", false, "Avvist av test")
            val status = repo.findNewestStatus(orgnr, id)
            assertEquals("Melding behandlet, ikke akseptert", status!!.status)
            assertEquals("Avvist av test", status.description!!)
            exec("SELECT * FROM message_status") { rs ->
                rs.next()
                assertEquals("123456789-theIdOfTheInsertedRecord", rs.getString("message_id"))
                assertNotNull(rs.getTimestamp("processed_at"))
                assertEquals("REJECTED", rs.getString("latest_status"))
                assertNotNull(rs.getTimestamp("response_at"))
                assertEquals("Avvist av test", rs.getString("response_description"))
            }
            rollback()
        }
    }
}

fun PostgreSQLContainer.testConfiguration(user: String = "admin"): HikariConfig {
    val (username, password) = when (user) {
        "admin" -> this@testConfiguration.username to this@testConfiguration.password
        "user" -> "$MESSAGE_STATUS_DB_NAME-user" to "app_pass"
        else -> error("Unsupported user: $user")
    }
    return HikariConfig().apply {
        jdbcUrl = this@testConfiguration.jdbcUrl
        this.username = username
        this.password = password
        maximumPoolSize = 5
        minimumIdle = 1
        idleTimeout = 500001
        connectionTimeout = 10000
        maxLifetime = 600001
        initializationFailTimeout = 5000
    }
}

fun buildDatabaseContainer(): PostgreSQLContainer {
    return PostgreSQLContainer("postgres:15").apply {
        withInitScript("init_roles.sql")
        withUsername("$MESSAGE_STATUS_DB_NAME-admin")
        withReuse(true)
        withLabel("app-name", "ekstern-trekk-api")
        start()
    }
}
