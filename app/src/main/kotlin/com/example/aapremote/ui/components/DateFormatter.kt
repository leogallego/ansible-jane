package com.example.aapremote.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class TimeFormat {
    SYSTEM,
    HOURS_24,
    HOURS_12;

    val displayName: String
        get() = when (this) {
            SYSTEM -> "System default"
            HOURS_24 -> "24-hour"
            HOURS_12 -> "12-hour (AM/PM)"
        }
}

object DateFormatter {

    @Volatile
    var zoneOverride: ZoneId? = null

    @Volatile
    var timeFormat: TimeFormat = TimeFormat.SYSTEM

    private val dateOnlyFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    private val zone: ZoneId
        get() = zoneOverride ?: ZoneId.systemDefault()

    private val dateTimeFormatter: DateTimeFormatter
        get() = when (timeFormat) {
            TimeFormat.SYSTEM ->
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            TimeFormat.HOURS_24 ->
                DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.getDefault())
            TimeFormat.HOURS_12 ->
                DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault())
        }

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
