package no.nav.trekkapi.api

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String
)
