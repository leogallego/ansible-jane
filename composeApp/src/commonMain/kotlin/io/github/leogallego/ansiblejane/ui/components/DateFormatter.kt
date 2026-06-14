package io.github.leogallego.ansiblejane.ui.components

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateFormatter {

    @Volatile
    var zoneOverride: TimeZone? = null

    @Volatile
    var timeFormat: TimeFormat = TimeFormat.SYSTEM

    @Volatile
    var systemIs24Hour: Boolean? = null

    private val zone: TimeZone
        get() = zoneOverride ?: TimeZone.currentSystemDefault()

    fun formatDateTime(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val local = instant.toLocalDateTime(zone)
            val month = MONTH_ABBREVS[local.month.ordinal]
            val use24h = when (timeFormat) {
                TimeFormat.SYSTEM -> systemIs24Hour ?: isSystem24HourFormat()
                TimeFormat.HOURS_24 -> true
                TimeFormat.HOURS_12 -> false
            }
            if (use24h) {
                "$month ${local.day}, ${local.year} ${local.hour.pad()}:${local.minute.pad()}"
            } else {
                val (hour12, amPm) = to12Hour(local.hour)
                "$month ${local.day}, ${local.year} $hour12:${local.minute.pad()} $amPm"
            }
        } catch (_: Exception) {
            isoTimestamp
        }
    }

    fun formatRelative(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val now = Clock.System.now()
            val diff = now - instant
            val minutesAgo = diff.inWholeMinutes
            if (minutesAgo < 0) {
                val minutesUntil = -minutesAgo
                when {
                    minutesUntil < 1 -> "now"
                    minutesUntil < 60 -> "in ${minutesUntil}m"
                    minutesUntil < 1440 -> "in ${minutesUntil / 60}h"
                    minutesUntil < 10080 -> "in ${minutesUntil / 1440}d"
                    else -> formatDateTime(isoTimestamp)
                }
            } else {
                when {
                    minutesAgo < 1 -> "just now"
                    minutesAgo < 60 -> "${minutesAgo}m ago"
                    minutesAgo < 1440 -> "${minutesAgo / 60}h ago"
                    minutesAgo < 10080 -> "${minutesAgo / 1440}d ago"
                    else -> formatDateTime(isoTimestamp)
                }
            }
        } catch (_: Exception) {
            isoTimestamp
        }
    }

    fun formatDuration(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val now = Clock.System.now()
            val minutesAgo = (now - instant).inWholeMinutes.coerceAtLeast(0)
            when {
                minutesAgo < 1 -> "just now"
                minutesAgo < 60 -> "${minutesAgo}m"
                minutesAgo < 1440 -> "${minutesAgo / 60}h"
                minutesAgo < 10080 -> "${minutesAgo / 1440}d"
                else -> formatDateTime(isoTimestamp)
            }
        } catch (_: Exception) {
            isoTimestamp
        }
    }

    fun formatDate(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val local = instant.toLocalDateTime(zone)
            val month = MONTH_ABBREVS[local.month.ordinal]
            "$month ${local.day}, ${local.year}"
        } catch (_: Exception) {
            isoTimestamp
        }
    }

    private fun Int.pad(): String = toString().padStart(2, '0')

    private fun to12Hour(hour24: Int): Pair<Int, String> {
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return hour12 to amPm
    }

    private val MONTH_ABBREVS = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
}
