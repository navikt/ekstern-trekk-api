package no.nav.trekkapi

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import java.io.InputStream
import java.sql.DriverManager
import java.util.Properties
import kotlin.use
import kotlin.uuid.Uuid

// When run: call the function you want, add more if needed. Will connect to kafka/postgres as started by RunLocalContainers
fun main() {
    val client = LocalTestClient()

    // Simulert innmelding kan gjøres ved å GET'e http://localhost:8080/test/putinnrapportering
    // Etter det kan det verifiseres at DB inneholder ny meldingsstatus for orgnr 123456789, og ny generert ID
    // Simulert respons kan gjøres ved putMessageOnResponseTopic, med orgnr 123456789 og den nye IDen
    // Etter det kan det verifiseres at DB inneholder oppdatert meldingsstatus orgnr 123456789, og den nye IDen
    // Meldingsstatus kan også hentes ved å GET'e http://localhost:8080/test/innrapportering/{id}

//    client.putMessageOnResponseTopic("9e72380a-5524-4a0e-b629-e43c1a219091")
//    client.listAllMessages("team-emottak.trekkapi.respons")
//    client.readDb("select count(*) from message_status")
    client.readDb("select * from message_status")
}

class LocalTestClient {

    val kafkaBrokerUrl = getRunningKafkaBrokerUrl()
    val kafkaProperties = Properties().apply {
        put("bootstrap.servers", kafkaBrokerUrl)
        put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    }

    val kafkaProducer = KafkaProducer<String, String>(kafkaProperties)

    fun putMessageOnResponseTopic(id: String = "id") {
        val topic = "team-emottak.trekkapi.respons"
        val key = Uuid.random().toString()
        val value = readClasspathFile("trekkopplysning_respons_avvist_duplikat.xml")!!
        val edited = value.replace("69abb69f-b491-4d34-aeb1-10c02c7b98b6", id)
        val headers = emptyList<RecordHeader>()
        sendMessage(topic, key, edited, headers)
    }

    fun sendMessage(topic: String, key: String, value: String, headers: List<Header> = emptyList()) {
        val record = ProducerRecord(topic, null, key, value, headers)
        kafkaProducer.send(record) { metadata, exception ->
            if (exception == null) {
                println("Message with key ${record.key()} sent successfully to topic: ${metadata.topic()}, partition: ${metadata.partition()}, offset: ${metadata.offset()}")
            } else {
                System.err.println("Error sending message: ${exception.message}")
            }
        }
        kafkaProducer.flush() // Ensure all buffered records are sent
    }

    // This will "hang" until some timeout when there are no messages on the topic. Can be killed then.
    fun listAllMessages(topic: String) {
        val kafkaConsumer = KafkaConsumer<String, String>(kafkaProperties)

        kafkaConsumer.partitionsFor(topic).forEach { partition ->
            kafkaConsumer.assign(listOf(TopicPartition(partition.topic(), partition.partition())))
            kafkaConsumer.seekToBeginning(listOf(TopicPartition(partition.topic(), partition.partition())))
            kafkaConsumer.poll(5000).forEach { record ->
                println("Record---------------------------------------------- offset: ${record.offset()}")
                println("Key: ${record.key()}, Value: ${record.value()}")
            }
        }
        kafkaConsumer.close()
    }

    fun readDb(query: String) {
        val dbconfig = getRunningPostgresConfiguration()
        DriverManager.getConnection(dbconfig.jdbcUrl, dbconfig.username, dbconfig.password).use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(query)
                val cols = resultSet.metaData.columnCount
                iteration@ for (i in 1..cols) {
                    print(resultSet.metaData.getColumnName(i) + "  ")
                }
                println()
                resultSet.use {
                    while (resultSet.next()) {
                        iteration@ for (i in 1..cols) {
                            print(max50(resultSet.getString(i)) + "  ")
                        }
                        println()
                    }
                }
            }
        }
    }

    private fun max50(s: String?): String {
        if (s == null) return ""
        return if (s.length > 50) s.substring(0, 50) + "..." else s
    }

    fun readClasspathFile(fileName: String): String? {
        val inputStream: InputStream? = this::class.java.getResourceAsStream("/$fileName")
        return inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }
}
