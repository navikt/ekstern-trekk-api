package no.nav.trekkapi.configuration

import com.sksamuel.hoplite.Masked
import no.nav.trekkapi.util.getEnvVar
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_KEYSTORE_TYPE_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG
import org.apache.kafka.common.config.SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG
import java.util.Properties
import kotlin.time.Duration

data class Config(
    val environment: Environment,
    val database: Database,
    val kafka: Kafka,
    val kafkaResponseQueue: KafkaResponseQueue,
    val trekkopplysningMq: TrekkopplysningMq,
    val server: Server,
)

data class Environment(
    val naisClusterName: NaisClusterName,
) {
    fun isProduction() = naisClusterName.value == "prod-gcp"
}

data class Server(
    val port: Port,
    val preWait: Duration,
)

data class Database(
    val jdbcUrl: String,
    val jdbcUser: String,
    val jdbcPassword: String,
    val maxConnectionPoolSizeForUser: MaxConnectionPoolSizeForUser,
    val maxConnectionPoolSizeForAdmin: MaxConnectionPoolSizeForAdmin,
    val distinctValuesRefreshRateInHours: DistinctValuesRefreshRateInHours,
)

data class TrekkopplysningMq(
    val hostname: Host,
    val port: Int,
    val queueManager: String,
    val channel: String,
    val queue: String,
    val username: String,
    val password: Masked,
)

data class KafkaResponseQueue(
    val active: Boolean,
    val topic: String,
    val initOffset: String,
)

data class Kafka(
    val bootstrapServers: String,
    val securityProtocol: SecurityProtocol,
    val keystoreType: KeystoreType,
    val keystoreLocation: KeystoreLocation,
    val keystorePassword: Masked,
    val truststoreType: TruststoreType,
    val truststoreLocation: TruststoreLocation,
    val truststorePassword: Masked,
    val groupId: String,
    val maxPollRecords: Int = 50,
)

@JvmInline
value class SecurityProtocol(
    val value: String,
)

@JvmInline
value class KeystoreType(
    val value: String,
)

@JvmInline
value class KeystoreLocation(
    val value: String,
)

@JvmInline
value class TruststoreType(
    val value: String,
)

@JvmInline
value class TruststoreLocation(
    val value: String,
)

@JvmInline
value class Host(
    val value: String,
)

@JvmInline
value class Port(
    val value: Int,
)

@JvmInline
value class NaisClusterName(
    val value: String,
)

@JvmInline
value class MaxConnectionPoolSizeForUser(
    val value: Int,
)

@JvmInline
value class MaxConnectionPoolSizeForAdmin(
    val value: Int,
)

@JvmInline
value class DistinctValuesRefreshRateInHours(
    val value: Long,
)

fun Kafka.toProperties() =
    Properties()
        .apply {
            put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(MAX_POLL_RECORDS_CONFIG, maxPollRecords.toString())
            if (getEnvVar("NAIS_CLUSTER_NAME", "local") != "local") {
                put(SECURITY_PROTOCOL_CONFIG, securityProtocol.value)
                put(SSL_KEYSTORE_TYPE_CONFIG, keystoreType.value)
                put(SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword.value)
                put(SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation.value)
                put(SSL_TRUSTSTORE_TYPE_CONFIG, truststoreType.value)
                put(SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation.value)
                put(SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword.value)
            }
        }
