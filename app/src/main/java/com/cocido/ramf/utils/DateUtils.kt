package com.cocido.ramf.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    fun formatDate(date: Date, format: String = "dd/MM/yyyy"): String {
        val formatter = SimpleDateFormat(format, Locale.getDefault())
        return formatter.format(date)
    }
}
