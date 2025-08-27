package com.cocido.ramf.models

import com.google.gson.annotations.SerializedName

data class TemperatureMaxMin(
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("temperature") val temperature: Double?,
    @SerializedName("maxTemperature") val maxTemperature: Double?,
    @SerializedName("minTemperature") val minTemperature: Double?,
    @SerializedName("relativeHumidity") val relativeHumidity: Double?,
    @SerializedName("pressure") val pressure: Double?,
    @SerializedName("windSpeed") val windSpeed: Double?,
    @SerializedName("windDirection") val windDirection: String?,
    @SerializedName("solarRadiation") val solarRadiation: Double?,
    @SerializedName("uvIndex") val uvIndex: Double?,
    @SerializedName("dewPoint") val dewPoint: Double?,
    @SerializedName("heatIndex") val heatIndex: Double?,
    @SerializedName("windChill") val windChill: Double?,
    @SerializedName("visibility") val visibility: Double?,
    @SerializedName("precipitationRate") val precipitationRate: Double?,
    @SerializedName("precipitationAccumulation") val precipitationAccumulation: Double?
) {
    val min: Double? get() = minTemperature
    val max: Double? get() = maxTemperature
}
