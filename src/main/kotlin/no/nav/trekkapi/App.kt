package no.nav.trekkapi

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.result
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import io.ktor.server.application.Application
import io.ktor.server.netty.Netty
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import no.nav.emottak.utils.coroutines.coroutineScope
import no.nav.trekkapi.configuration.config
import no.nav.trekkapi.innmelding.TrekkInnmeldingRepository
import no.nav.trekkapi.innmelding.TrekkInnmeldingService
import no.nav.trekkapi.plugin.configureAuthentication
import no.nav.trekkapi.plugin.configureContentNegotiation
import no.nav.trekkapi.plugin.configureMetrics
import no.nav.trekkapi.plugin.configureRoutes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.coroutineContext

val log: Logger = LoggerFactory.getLogger("no.nav.trekkapi.App")

fun main(args: Array<String>) = SuspendApp {
    result {
        resourceScope {
            runServer()
            awaitCancellation()
        }
    }
}

suspend fun ResourceScope.runServer() {
    val config = config()
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    // todo kanskje lokal DB, eller eventManager client (både for å lagre og lese)
//    val database = Database(eventDbConfig.value)
//    database.migrate(eventMigrationConfig.value)

    val trekkInnmeldingRepository = TrekkInnmeldingRepository()

    val trekkInnmeldingService = TrekkInnmeldingService(trekkInnmeldingRepository)

    val serverConfig = config.server
    server(
        factory = Netty,
        port = serverConfig.port.value,
        preWait = serverConfig.preWait,
        module = trekkapiModule(trekkInnmeldingService, prometheusMeterRegistry)
    )

    log.debug("Configuration: {}", config)
//    if (config.eventConsumer.active) {
    // todo kanskje kafka listener, eller poller som sjekker eventManager jevnlig
    // kafka listener vil enten lagre i lokal DB eller event manager
    log.info("Starting receiver")
    val eventReceiverScope = coroutineScope(coroutineContext + Dispatchers.IO)
    eventReceiverScope.launch {
//            startEventReceiver(
//                listOf(
//                    config.eventConsumer.eventTopic,
//                    config.eventConsumer.messageDetailsTopic
//                ),
//                eventService,
//                ebmsMessageDetailService
//            )
    }
//    }
}

fun trekkapiModule(
    trekkInnmeldingService: TrekkInnmeldingService,
    prometheusMeterRegistry: PrometheusMeterRegistry
): Application.() -> Unit {
    return {
        configureMetrics(prometheusMeterRegistry)
        configureContentNegotiation()
        configureAuthentication()
        configureRoutes(trekkInnmeldingService, prometheusMeterRegistry)
    }
}
