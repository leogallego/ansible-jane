package com.example.aapremote.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object DateFormatter {

    @Volatile
    var zoneOverride: ZoneId? = null

    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

    private val dateOnlyFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    private val zone: ZoneId
        get() = zoneOverride ?: ZoneId.systemDefault()

    fun formatDateTime(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val local = instant.atZone(zone)
            dateTimeFormatter.format(local)
        } catch (_: Exception) {
            isoTimestamp
        }
    }

    fun formatDate(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val local = instant.atZone(zone)
            dateOnlyFormatter.format(local)
        } catch (_: Exception) {
            isoTimestamp
        }
    }
}
