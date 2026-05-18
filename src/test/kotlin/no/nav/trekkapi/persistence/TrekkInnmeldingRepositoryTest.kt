package no.nav.trekkapi.persistence

import com.zaxxer.hikari.HikariConfig
import kotlinx.coroutines.runBlocking
import no.nav.trekkapi.persistence.table.MessageStatusEnum
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        val idempotencyKey = "550e8400-e29b-41d4-a716-446655440000"
        suspendTransaction {
            repo.register(orgnr, id, idempotencyKey)
            val status = repo.findNewestStatus(orgnr, id)
            assertEquals(MessageStatusEnum.PENDING, status!!.status)
            assertEquals(id, status.id)
            assertNotNull(status.submittedAt)
            assertEquals(status.submittedAt, status.updatedAt)
            exec("SELECT count(*) FROM message_status") { rs ->
                rs.next()
                assertEquals(1, rs.getInt(1))
            }
            exec("SELECT * FROM message_status") { rs ->
                rs.next()
                assertEquals("123451111", rs.getString("org_nr"))
                assertEquals("theIdOfTheInsertedRecord", rs.getString("message_id"))
                assertNotNull(rs.getTimestamp("processed_at"))
                assertEquals("PENDING", rs.getString("latest_status"))
                assertNull(rs.getTimestamp("response_at"))
                assertNull(rs.getString("response_description"))
                assertEquals(idempotencyKey, rs.getString("idempotency_key"))
            }
            rollback()
        }
    }

    @Test
    fun `Verify findByIdempotencyKey() returns existing message id`() = runBlocking {
        val repo = TrekkInnmeldingRepository(db)
        val orgnr = "123451111"
        val id = "theIdOfTheInsertedRecord"
        val idempotencyKey = "550e8400-e29b-41d4-a716-446655440001"
        newSuspendedTransaction {
            repo.register(orgnr, id, idempotencyKey)
            assertEquals(id, repo.findByIdempotencyKey(orgnr, idempotencyKey))
            assertNull(repo.findByIdempotencyKey(orgnr, "00000000-0000-0000-0000-000000000000"))
            assertNull(repo.findByIdempotencyKey("999999999", idempotencyKey))
            rollback()
        }
    }

    @Test
    fun `Verify registerAcceptedResponse() and findNewestStatus()`() =
        runBlocking {
            val repo = TrekkInnmeldingRepository(db)
            val orgnr = "123456789"
            val id = "theIdOfTheInsertedRecord"
            suspendTransaction {
                repo.register(orgnr, id, "550e8400-e29b-41d4-a716-446655440002")
                repo.registerResponse("123456789", "theIdOfTheInsertedRecord", true)
                val status = repo.findNewestStatus(orgnr, id)
                assertEquals(MessageStatusEnum.ACCEPTED, status!!.status)
                assertEquals(id, status.id)
                assertNotNull(status.submittedAt)
                assertNotNull(status.updatedAt)
                exec("SELECT * FROM message_status") { rs ->
                    rs.next()
                    assertEquals("123456789", rs.getString("org_nr"))
                    assertEquals("theIdOfTheInsertedRecord", rs.getString("message_id"))
                    assertNotNull(rs.getTimestamp("processed_at"))
                    assertEquals("ACCEPTED", rs.getString("latest_status"))
                    assertNotNull(rs.getTimestamp("response_at"))
                    assertNull(rs.getString("response_description"))
                }
                rollback()
            }
        }

    @Test
    fun `Verify registerRejectedResponse() and findNewestStatus()`() =
        runBlocking {
            val repo = TrekkInnmeldingRepository(db)
            val orgnr = "123456789"
            val id = "theIdOfTheInsertedRecord"
            suspendTransaction {
                repo.register(orgnr, id, "550e8400-e29b-41d4-a716-446655440003")
                repo.registerResponse("123456789", "theIdOfTheInsertedRecord", false, "Avvist av test", "TEST_CODE")
                val status = repo.findNewestStatus(orgnr, id)
                assertEquals(MessageStatusEnum.REJECTED, status!!.status)
                assertEquals(id, status.id)
                assertNotNull(status.submittedAt)
                assertNotNull(status.updatedAt)
                assertEquals("Avvist av test", status.rejectionDescription)
                assertEquals("TEST_CODE", status.rejectionCode)
                exec("SELECT * FROM message_status") { rs ->
                    rs.next()
                    assertEquals("123456789", rs.getString("org_nr"))
                    assertEquals("theIdOfTheInsertedRecord", rs.getString("message_id"))
                    assertNotNull(rs.getTimestamp("processed_at"))
                    assertEquals("REJECTED", rs.getString("latest_status"))
                    assertNotNull(rs.getTimestamp("response_at"))
                    assertEquals("Avvist av test", rs.getString("response_description"))
                    assertEquals("TEST_CODE", rs.getString("response_code"))
                }
                rollback()
            }
        }
}

fun PostgreSQLContainer.testConfiguration(user: String = "admin"): HikariConfig {
    val (username, password) =
        when (user) {
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

fun buildDatabaseContainer(): PostgreSQLContainer =
    PostgreSQLContainer("postgres:15").apply {
        withInitScript("init_roles.sql")
        withUsername("$MESSAGE_STATUS_DB_NAME-admin")
        withReuse(true)
        withLabel("app-name", "ekstern-trekk-api")
        start()
    }
