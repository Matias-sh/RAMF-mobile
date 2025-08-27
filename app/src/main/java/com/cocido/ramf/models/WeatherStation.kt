package com.cocido.ramf.models

import com.google.gson.annotations.SerializedName

data class WeatherStation(
    @SerializedName("stationName") val id: String,
    @SerializedName("customName") val name: String?,
    @SerializedName("rights") val rights: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("lastCommunication") val lastCommunication: String?,
    @SerializedName("db") val db: DbDates?,
    @SerializedName("position") val position: Position?,
    @SerializedName("config") val config: StationConfig?
)

data class DbDates(
    @SerializedName("maxDate") val maxDate: String?,
    @SerializedName("minDate") val minDate: String?
)

data class Position(
    @SerializedName("coordinates") val coordinates: List<Double>?
)

data class StationConfig(
    @SerializedName("timezoneOffset") val timezoneOffset: Int?
)
