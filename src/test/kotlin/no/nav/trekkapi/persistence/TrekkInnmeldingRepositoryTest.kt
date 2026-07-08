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
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// For å kunne opprette DB bare 1 gang for hele testklassen
// Alternativet er å flytte Before/AfterAll funksjonene til et companion object
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrekkInnmeldingRepositoryTest {
    lateinit var dbContainer: PostgreSQLContainer
    lateinit var db: Database
    val payload = this::class.java.getResource("/trekkopplysning_innmelding.xml")?.readText() ?: ""

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
    fun `Verify register() and findNewestStatus()`() =
        runBlocking {
            val repo = TrekkInnmeldingRepository(db)
            val orgnr = "123451111"
            val id = "theIdOfTheInsertedRecord"
            suspendTransaction {
                val registered = repo.register(orgnr, id, payload)
                assertTrue(registered)
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
                    assertNotNull(rs.getString("request_xml"))
                    assertEquals(payload, rs.getString("request_xml"))
                }
                rollback()
            }
        }

    @Test
    fun `register() should return false when duplicate`() =
        runBlocking {
            val repo = TrekkInnmeldingRepository(db)
            val orgnr = "123451111"
            val id = "theIdOfTheInsertedRecord"
            suspendTransaction {
                var registered = repo.register(orgnr, id, payload)
                assertTrue(registered)
                registered = repo.register(orgnr, id, payload)
                assertFalse(registered)
                exec("SELECT count(*) FROM message_status") { rs ->
                    rs.next()
                    assertEquals(1, rs.getInt(1))
                }
                rollback()
            }
        }

    @Test
    fun `Verify registerResponse() accepted and findNewestStatus()`() =
        runBlocking {
            val repo = TrekkInnmeldingRepository(db)
            val orgnr = "123456789"
            val id = "theIdOfTheInsertedRecord"
            suspendTransaction {
                var registered = repo.register(orgnr, id, payload)
                assertTrue(registered)
                registered = repo.registerResponse(orgnr, id, true)
                assertTrue(registered)
                val status = repo.findNewestStatus(orgnr, id)
                assertEquals(MessageStatusEnum.ACCEPTED, status!!.status)
                assertEquals(id, status.id)
                assertNotNull(status.submittedAt)
                assertNotNull(status.updatedAt)
                assertNull(status.responseXml)
                exec("SELECT * FROM message_status") { rs ->
                    rs.next()
                    assertEquals("123456789", rs.getString("org_nr"))
                    assertEquals("theIdOfTheInsertedRecord", rs.getString("message_id"))
                    assertNotNull(rs.getTimestamp("processed_at"))
                    assertEquals("ACCEPTED", rs.getString("latest_status"))
                    assertNotNull(rs.getTimestamp("response_at"))
                    assertNull(rs.getString("response_description"))
                    assertNull(rs.getString("response_xml"))
                    assertNotNull(rs.getString("request_xml"))
                    assertEquals(payload, rs.getString("request_xml"))
                }
                rollback()
            }
        }

    @Test
    fun `Verify registerResponse() accepted with xml and findNewestStatus()`() =
        runBlocking {
            val repo = TrekkInnmeldingRepository(db)
            val orgnr = "123456789"
            val id = "theIdOfTheInsertedRecord"
            val fagmeldingXml =
                """<MsgHead xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24"><MsgInfo><Type V="INNRAPPORTERING_TREKK_RETUR"/></MsgInfo></MsgHead>"""
            val expectedBase64 = Base64.getEncoder().encodeToString(fagmeldingXml.toByteArray())
            suspendTransaction {
                var registered = repo.register(orgnr, id, payload)
                assertTrue(registered)
                registered = repo.registerResponse(orgnr, id, true, xml = fagmeldingXml)
                assertTrue(registered)
                val status = repo.findNewestStatus(orgnr, id)
                assertEquals(MessageStatusEnum.ACCEPTED, status!!.status)
                assertEquals(expectedBase64, status.responseXml)
                exec("SELECT response_xml FROM message_status") { rs ->
                    rs.next()
                    assertEquals(fagmeldingXml, rs.getString("response_xml"))
                }
                rollback()
            }
        }

    @Test
    fun `registerResponse() should return false when unknown ID`() =
        runBlocking {
            val repo = TrekkInnmeldingRepository(db)
            val orgnr = "123456789"
            val id = "theIdOfTheInsertedRecord"
            val fagmeldingXml =
                """<MsgHead xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24"><MsgInfo><Type V="INNRAPPORTERING_TREKK_RETUR"/></MsgInfo></MsgHead>"""
            suspendTransaction {
                var registered = repo.register(orgnr, id, payload)
                assertTrue(registered)
                registered = repo.registerResponse("111222333", id, true, xml = fagmeldingXml)
                assertFalse(registered)
                registered = repo.registerResponse(orgnr, "an-unknown-message-id", true, xml = fagmeldingXml)
                assertFalse(registered)
                rollback()
            }
        }

    @Test
    fun `Verify registerResponse() rejected and findNewestStatus()`() =
        runBlocking {
            val repo = TrekkInnmeldingRepository(db)
            val orgnr = "123456789"
            val id = "theIdOfTheInsertedRecord"
            suspendTransaction {
                var registered = repo.register(orgnr, id, payload)
                assertTrue(registered)
                registered = repo.registerResponse("123456789", "theIdOfTheInsertedRecord", false, "Avvist av test", "TEST_CODE")
                assertTrue(registered)
                val status = repo.findNewestStatus(orgnr, id)
                assertEquals(MessageStatusEnum.REJECTED, status!!.status)
                assertEquals(id, status.id)
                assertNotNull(status.submittedAt)
                assertNotNull(status.updatedAt)
                assertEquals("Avvist av test", status.rejectionDescription)
                assertEquals("TEST_CODE", status.rejectionCode)
                assertNull(status.responseXml)
                exec("SELECT * FROM message_status") { rs ->
                    rs.next()
                    assertEquals("123456789", rs.getString("org_nr"))
                    assertEquals("theIdOfTheInsertedRecord", rs.getString("message_id"))
                    assertNotNull(rs.getTimestamp("processed_at"))
                    assertEquals("REJECTED", rs.getString("latest_status"))
                    assertNotNull(rs.getTimestamp("response_at"))
                    assertEquals("Avvist av test", rs.getString("response_description"))
                    assertEquals("TEST_CODE", rs.getString("response_code"))
                    assertNull(rs.getString("response_xml"))
                }
                rollback()
            }
        }

    @Test
    fun `Verify registerResponse() rejected with xml and findNewestStatus()`() =
        runBlocking {
            val repo = TrekkInnmeldingRepository(db)
            val orgnr = "123456789"
            val id = "theIdOfTheInsertedRecord"
            val fagmeldingXml =
                """<AppRec xmlns="http://www.kith.no/xmlstds/apprec/2004-11-21"><Status V="2" DN="Avvist"/><Error V="B720007F" DN="Avvist av test"/></AppRec>"""
            val expectedBase64 = Base64.getEncoder().encodeToString(fagmeldingXml.toByteArray())
            suspendTransaction {
                var registered = repo.register(orgnr, id, payload)
                assertTrue(registered)
                registered = repo.registerResponse(orgnr, id, false, "Avvist av test", "TEST_CODE", fagmeldingXml)
                assertTrue(registered)
                val status = repo.findNewestStatus(orgnr, id)
                assertEquals(MessageStatusEnum.REJECTED, status!!.status)
                assertEquals("Avvist av test", status.rejectionDescription)
                assertEquals("TEST_CODE", status.rejectionCode)
                assertEquals(expectedBase64, status.responseXml)
                exec("SELECT response_xml FROM message_status") { rs ->
                    rs.next()
                    assertEquals(fagmeldingXml, rs.getString("response_xml"))
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
