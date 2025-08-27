package com.cocido.ramf.models

data class WeatherStationResponseWrapper(
    val data: List<WeatherStation>,
    val meta: MetaData?,
    val links: Map<String, String>?
)
