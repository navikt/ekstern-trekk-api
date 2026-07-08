package no.nav.trekkapi.api

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val message: String,
)
