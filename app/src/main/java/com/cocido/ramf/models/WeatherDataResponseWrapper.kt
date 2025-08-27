package com.cocido.ramf.models

data class WeatherDataResponseWrapper(
    val data: List<WeatherData>,
    val meta: MetaData?,
    val links: Map<String, String>?
)
