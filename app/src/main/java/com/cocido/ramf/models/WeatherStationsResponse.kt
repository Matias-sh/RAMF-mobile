package com.cocido.ramf.models

data class WeatherStationsResponse(
    val data: List<WeatherStation>,
    val meta: MetaData?,
    val links: Map<String, String>?
)

data class MetaData(
    val itemsPerPage: Int,
    val totalItems: Int,
    val currentPage: Int,
    val totalPages: Int
)
