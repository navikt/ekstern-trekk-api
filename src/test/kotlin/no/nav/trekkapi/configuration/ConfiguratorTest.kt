package no.nav.trekkapi.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfiguratorTest {

    @Test
    fun `config loads without throwing`() {
        val config = config()
        assertNotNull(config)
    }

    @Test
    fun `config has expected server settings`() {
        val config = config()
        assertEquals(8080, config.server.port.value)
    }

    @Test
    fun `config has expected kafka settings`() {
        val config = config()
        assertEquals("ekstern-trekk-api", config.kafka.groupId)
    }

    @Test
    fun `config has expected kafkaResponseQueue settings`() {
        val config = config()
        assertEquals("team-emottak.trekkapi.response", config.kafkaResponseQueue.topic)
        assertTrue(config.kafkaResponseQueue.active)
    }

    @Test
    fun `config has expected database pool settings`() {
        val config = config()
        assertEquals(4, config.database.maxConnectionPoolSizeForUser.value)
        assertEquals(1, config.database.maxConnectionPoolSizeForAdmin.value)
        assertEquals(24L, config.database.distinctValuesRefreshRateInHours.value)
    }
}
