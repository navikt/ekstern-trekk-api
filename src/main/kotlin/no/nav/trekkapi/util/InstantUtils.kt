package no.nav.trekkapi.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

fun nowOsloToInstant(): Instant = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).toInstant()
