package no.nav.trekkapi.innmelding

import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.trekkapi.configuration.TrekkopplysningMq
import no.nav.trekkapi.log
import javax.jms.Session

// todo konskje få egen bruker ?
class JmsClient(
    config: TrekkopplysningMq,
    val factory: MQQueueConnectionFactory = MQQueueConnectionFactory(),
    val username: String = config.username,
    val password: String = config.password.value
) {

    /*
    If we only supply queuemanager and no channel, it seems the connection will be made in "bind/server" mode.
    This led to the error message "Failed to load the IBM MQ native JNI library: 'mqjbnd'".
    We therefore must explicitly set the connection mode to client.

    There is no Channel defined in Fasit for old eMottak, only the queuemanager (MQLS04 in Q1).
     */
    init {
        factory.hostName = config.hostname.value
        factory.port = config.port
        factory.queueManager = config.queueManager
        factory.channel = config.channel
        factory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT)
        log.debug("MQ User: $username")
    }

    // Her opprettes ny connection (og lukkes) for hver melding.
    // Kan cache/poole connections hvis dette viser seg å bli for mye overhead
    fun sendMessage(queue: String, messageText: String) {
        factory.createContext(username, password, Session.AUTO_ACKNOWLEDGE)?.use {
            it.createProducer().send(it.createQueue(queue), it.createTextMessage(messageText))
        }
    }

    // Har tydeligvis ikke lov til å opprette en Browser (write-only rettigheter?),
    // finner ingen måte å pinge køen på, må nøye oss med å verifisere at vi får opprettet connection.
    fun verifyConnection() {
        factory.createContext(username, password, Session.AUTO_ACKNOWLEDGE)
    }
}
