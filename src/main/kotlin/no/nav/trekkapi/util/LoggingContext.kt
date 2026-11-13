package no.nav.trekkapi.util

import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

private val loggingContextKey = AttributeKey<Map<String, String>>("requestLoggingContext")

suspend fun <T> ApplicationCall.withLoggingContext(
    orgnr: String? = null,
    messageId: String? = null,
    block: suspend () -> T,
): T {
    val fields = attributes.getOrNull(loggingContextKey).orEmpty().toMutableMap()
    orgnr?.let { fields["orgnr"] = it }
    messageId?.let { fields["messageId"] = it }
    // Keep the fields on the call so StatusPages can restore them after an exception unwinds the coroutine context.
    attributes.put(loggingContextKey, fields)
    return withMdcFields(fields, block)
}

suspend fun <T> withLoggingContext(
    orgnr: String? = null,
    messageId: String? = null,
    block: suspend () -> T,
): T =
    withMdcFields(
        buildMap {
            orgnr?.let { put("orgnr", it) }
            messageId?.let { put("messageId", it) }
        },
        block,
    )

private suspend fun <T> withMdcFields(
    fields: Map<String, String>,
    block: suspend () -> T,
): T =
    withContext(MDCContext(MDC.getCopyOfContextMap().orEmpty() + fields)) {
        block()
    }
