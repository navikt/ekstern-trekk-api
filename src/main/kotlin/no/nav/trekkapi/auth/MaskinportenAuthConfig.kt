package no.nav.trekkapi.auth

import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingContext
import no.nav.security.token.support.v3.IssuerConfig
import no.nav.security.token.support.v3.RequiredClaims
import no.nav.security.token.support.v3.TokenSupportConfig
import no.nav.security.token.support.v3.TokenValidationContextPrincipal
import no.nav.trekkapi.util.getEnvVar
import no.nav.trekkapi.log

// Lag en config for hver tjenestetype, hvis det blir flere tjenester (sjekker forskjellig audience for hver)
const val MASKINPORTEN_AUTH_INMMELDING = "MASKINPORTEN_INNMELDING"

const val SCOPE_INNMELDING = "nav:utbetaling/trekkopplysning/innmelding"

// todo må testes, tanken er at vi får et token som inneholder audience "nav:utbetaling/trekkopplysning/innmelding" for de som har tilgang
// settes opp i nais.yaml: orgnr X har tilgang til scope Y https://docs.nais.io/auth/maskinporten/how-to/secure/
// maskinporten må akseptere bruker X. Og så må vi få ut orgnr fra authdetails
fun getInnmeldingConfig(): TokenSupportConfig = TokenSupportConfig(
    IssuerConfig(
        name = MASKINPORTEN_AUTH_INMMELDING,
        // "https://test.maskinporten.no/.well-known/oauth-authorization-server"
        discoveryUrl = getEnvVar("MASKINPORTEN_WELL_KNOWN_URL", "http://localhost:3344/AZURE_AD/.well-known/openid-configuration"),
        acceptedAudience = listOf(SCOPE_INNMELDING),
        optionalClaims = listOf("aud", "sub")
    )
)

fun getRequiredClaims(): RequiredClaims = RequiredClaims(issuer = MASKINPORTEN_AUTH_INMMELDING, claimMap = arrayOf("consumer", "scope"))

suspend fun RoutingContext.orgNrFromTokenValidationContext(): String? {
    val principal = call.principal<TokenValidationContextPrincipal>()
    log.debug("### Principal: $principal")
    val context = principal?.context
    log.debug("### Context: $context")
    val claims = context?.getClaims(MASKINPORTEN_AUTH_INMMELDING)
    log.debug("### Claims for $MASKINPORTEN_AUTH_INMMELDING: $claims")
    val consumer = claims?.get("consumer") as Map<*, *>
    log.debug("### Claim 'consumer': $consumer")
    val sub = claims?.get("sub") as Map<*, *>
    log.debug("### Claim 'sub': $sub")
    val orgnr = consumer.extractOrgnummer()
    log.debug("### Extracted orgnr: $orgnr")
    return orgnr
}

// Se format under
private fun Map<*, *>.extractOrgnummer(): String? = (get("ID") as? String)?.split(":")?.get(1)

/*
Se kode i https://github.com/navikt/sokos-oppgjorsrapporter/blob/0e30b20243949c60b7d714cfa05b8f3065b8548e/src/main/kotlin/no/nav/sokos/oppgjorsrapporter/auth/TokenValidationUtils.kt

Om maskinporten:
https://docs.digdir.no/docs/Maskinporten/maskinporten_protocol_token#the-access-token

"consumer" : {
   "authority" : "iso6523-actorid-upis",
   "ID" : "0192:991825827"
 }
 */

//            additionalValidation = { it.gyldigScope(getEnvVar("EKSPONERT_MASKINPORTEN_SCOPE")) && it.gyldigSystembrukerOgConsumer() },
//            resourceRetriever =
//                DefaultResourceRetriever(
//                    DEFAULT_HTTP_CONNECT_TIMEOUT,
//                    DEFAULT_HTTP_READ_TIMEOUT,
//                    DEFAULT_HTTP_SIZE_LIMIT
//                ),
// )
