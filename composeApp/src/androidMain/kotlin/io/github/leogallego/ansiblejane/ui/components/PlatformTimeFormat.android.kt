package io.github.leogallego.ansiblejane.ui.components

import java.text.DateFormat
import java.text.SimpleDateFormat

actual fun isSystem24HourFormat(): Boolean {
    val pattern = (DateFormat.getTimeInstance(DateFormat.SHORT) as? SimpleDateFormat)?.toPattern() ?: return true
    return 'H' in pattern
}
