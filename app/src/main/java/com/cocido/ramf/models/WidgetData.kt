package com.cocido.ramf.models

data class WidgetData(
    val timestamp: String,
    val temperature: Double,
    val maxTemperature: Double,
    val minTemperature: Double,
    val relativeHumidity: Double,
    val dewPoint: Double,
    val airPressure: Double,
    val solarRadiation: Int,
    val windSpeed: Double,
    val windDirection: String,
    val rainLastHour: Double,
    val rainDay: Double,
    val rain24h: Double,
    val rain48h: Double,
    val rain7d: Double
)