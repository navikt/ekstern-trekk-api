package no.nav.trekkapi.configuration

import no.nav.emottak.utils.config.Kafka
import no.nav.emottak.utils.config.Server
import no.nav.emottak.utils.config.toProperties
import no.nav.emottak.utils.environment.getEnvVar
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_TYPE_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG

data class Config(
    val environment: Environment,
    val database: Database,
    val kafka: Kafka,
    val kafkaResponseQueue: KafkaResponseQueue,
    val trekkopplysningMq: TrekkopplysningMq,
    val server: Server
)

data class Environment(
    val naisClusterName: NaisClusterName
)

data class Database(
    val jdbcUrl: String,
    val jdbcUser: String,
    val jdbcPassword: String,
    val maxConnectionPoolSizeForUser: MaxConnectionPoolSizeForUser,
    val maxConnectionPoolSizeForAdmin: MaxConnectionPoolSizeForAdmin,
    val distinctValuesRefreshRateInHours: DistinctValuesRefreshRateInHours
)

data class TrekkopplysningMq(
    val hostname: Host,
    val port: Int,
    val queueManager: String,
    val channel: String,
    val queue: String,
    val username: String,
    val password: String
)

data class KafkaResponseQueue(
    val active: Boolean,
    val topic: String,
    val initOffset: String
)

@JvmInline
value class Host(val value: String)

@JvmInline
value class NaisClusterName(val value: String)

@JvmInline
value class MaxConnectionPoolSizeForUser(val value: Int)

@JvmInline
value class MaxConnectionPoolSizeForAdmin(val value: Int)

@JvmInline
value class DistinctValuesRefreshRateInHours(val value: Long)

fun Kafka.toProperties() =
    toProperties()
        .apply {
            put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            if (getEnvVar("NAIS_CLUSTER_NAME", "local") == "local") {
                remove(SECURITY_PROTOCOL_CONFIG, securityProtocol.value)
                remove(SSL_KEYSTORE_TYPE_CONFIG, keystoreType.value)
                remove(SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation.value)
                remove(SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword.value)
                remove(SSL_TRUSTSTORE_TYPE_CONFIG, truststoreType.value)
                remove(SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation.value)
                remove(SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword.value)
            }
        }
