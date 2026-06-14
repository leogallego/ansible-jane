package io.github.leogallego.ansiblejane.ui.components

import java.text.DateFormat
import java.text.SimpleDateFormat

private val cached: Boolean by lazy {
    val pattern = (DateFormat.getTimeInstance(DateFormat.SHORT) as? SimpleDateFormat)?.toPattern() ?: return@lazy true
    'H' in pattern
}

actual fun isSystem24HourFormat(): Boolean = cached
