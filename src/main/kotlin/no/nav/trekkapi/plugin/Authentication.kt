package no.nav.trekkapi.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import no.nav.security.token.support.v3.tokenValidationSupport
import no.nav.trekkapi.auth.MASKINPORTEN_AUTH_INMMELDING
import no.nav.trekkapi.auth.getInnmeldingConfig
import no.nav.trekkapi.auth.getRequiredClaims

fun Application.configureAuthentication() {
    install(Authentication) {
//        tokenValidationSupport(AZURE_AD_AUTH, AuthConfig.getTokenSupportConfig())
        tokenValidationSupport(MASKINPORTEN_AUTH_INMMELDING, getInnmeldingConfig(), getRequiredClaims())
    }
}
