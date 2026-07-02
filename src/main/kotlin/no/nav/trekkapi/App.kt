package no.nav.trekkapi

import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.core.raise.result
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import io.ktor.server.application.Application
import io.ktor.server.netty.Netty
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import no.nav.trekkapi.configuration.config
import no.nav.trekkapi.innmelding.TrekkInnmeldingModel
import no.nav.trekkapi.innmelding.TrekkInnmeldingService
import no.nav.trekkapi.innmelding.startResponseReceiver
import no.nav.trekkapi.persistence.Database
import no.nav.trekkapi.persistence.TrekkInnmeldingRepository
import no.nav.trekkapi.persistence.messageStatusDbConfig
import no.nav.trekkapi.persistence.messageStatusMigrationConfig
import no.nav.trekkapi.plugin.configureAuthentication
import no.nav.trekkapi.plugin.configureContentNegotiation
import no.nav.trekkapi.plugin.configureMetrics
import no.nav.trekkapi.plugin.configureRoutes
import no.nav.trekkapi.plugin.configureStatusPages
import no.nav.trekkapi.util.coroutineScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.coroutineContext

val log: Logger = LoggerFactory.getLogger("no.nav.trekkapi.App")

fun main(args: Array<String>) =
    SuspendApp {
        log.info("--- Starting application")
        result {
            resourceScope {
                runServer()
                awaitCancellation()
            }
        }.onFailure { error ->
            log.error("Application startup failed", error)
            throw error
        }
    }

suspend fun ResourceScope.runServer() {
    log.info("--- Getting config")
    val config = config()
    log.info("--- Config loaded: $config")

    log.info("--- Starting database")
    val database = Database(messageStatusDbConfig.value)
    log.info("--- Calling migrate")
    database.migrate(messageStatusMigrationConfig.value)

    log.info("--- Starting services")

    server(
        factory = Netty,
        port = config.server.port.value,
        preWait = config.server.preWait,
        module =
            trekkapiModule(
                TrekkInnmeldingService(config.trekkopplysningMq, TrekkInnmeldingRepository(database)),
                PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            ),
    )

    log.debug("Configuration: {}", config)
    if (config.kafkaResponseQueue.active) {
        log.info("Starting response receiver")
        val eventReceiverScope = coroutineScope(coroutineContext + Dispatchers.IO)
        eventReceiverScope.launch {
            startResponseReceiver(
                config.kafkaResponseQueue.topic,
                config.kafka.groupId,
                TrekkInnmeldingModel(),
                TrekkInnmeldingRepository(database),
            )
        }
    }
}

fun trekkapiModule(
    trekkInnmeldingService: TrekkInnmeldingService,
    prometheusMeterRegistry: PrometheusMeterRegistry,
): Application.() -> Unit {
    log.info("Configure plugins and routes")
    return {
        configureMetrics(prometheusMeterRegistry)
        log.info("Configured prometheus metrics")
        configureContentNegotiation()
        log.info("Configured content negotiation (JSON)")
        configureAuthentication()
        log.info("Configured authentication")
        configureStatusPages()
        log.info("Configured status pages")
        configureRoutes(trekkInnmeldingService, prometheusMeterRegistry)
        log.info("Configured routes")
    }
}
