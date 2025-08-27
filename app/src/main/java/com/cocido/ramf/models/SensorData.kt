package com.cocido.ramf.models

import com.google.gson.annotations.SerializedName

data class Sensors(
    @SerializedName("hcairtemperature") val hcAirTemperature: SensorWithAvgMaxMin? = null,
    @SerializedName("hcrelativehumidity") val hcRelativeHumidity: SensorWithAvgMaxMin? = null,
    @SerializedName("solarradiation") val solarRadiation: SensorAvg? = null,
    @SerializedName("precipitation") val precipitation: SensorSum? = null,
    @SerializedName("usonicwinddir") val usonicWindDir: SensorLast? = null,
    @SerializedName("usonicwindspeed") val usonicWindSpeed: SensorAvg? = null,
    @SerializedName("dewpoint") val dewPoint: SensorAvg? = null,
    @SerializedName("airpressure") val airPressure: SensorAvg? = null,
    @SerializedName("windgust") val windGust: SensorMax? = null,
    @SerializedName("windorientation") val windOrientation: SensorResult? = null
)

data class SensorAvg(
    @SerializedName("avg") val avg: Double? = null
)

data class SensorSum(
    @SerializedName("sum") val sum: Double? = null
)

data class SensorMax(
    @SerializedName("max") val max: Double? = null
)

data class SensorLast(
    @SerializedName("last") val last: Double? = null
)

data class SensorResult(
    @SerializedName("result") val result: Double? = null
)

data class SensorWithAvgMaxMin(
    @SerializedName("avg") val avg: Double? = null,
    @SerializedName("max") val max: Double? = null,
    @SerializedName("min") val min: Double? = null
)
