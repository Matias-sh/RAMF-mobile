package com.cocido.ramf.network

import com.cocido.ramf.models.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherStationService {

    // Nueva API local: /stations -> devuelve { data: [ WeatherStation ] }
    @GET("stations")
    suspend fun getWeatherStations(): Response<WeatherStationsResponse>

    // Nueva API: obtener estación específica por nombre
    @GET("stations/{stationName}")
    suspend fun getWeatherStation(
        @Path("stationName") stationName: String
    ): Response<WeatherStation>

    // Nueva API: obtener sensores de una estación
    @GET("stations/{stationName}/sensors")
    suspend fun getStationSensors(
        @Path("stationName") stationName: String
    ): Response<List<Any>> // TODO: definir modelo de sensor

    // Widget de temp máx/mín usando stationName
    @GET("stations-measurement/widget/{stationName}")
    suspend fun getTemperatureMaxMin(
        @Path("stationName") stationName: String
    ): Response<TemperatureMaxMin>

    // Widget con todos los datos principales
    @GET("stations-measurement/widget/{stationName}")
    suspend fun getWidgetData(
        @Path("stationName") stationName: String
    ): Response<WidgetData>

    // Datos en rango de tiempo usando stationName
    @GET("stations-measurement/data-time-range/{stationName}")
    suspend fun getWeatherDataTimeRange(
        @Path("stationName") stationName: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("timeRange") timeRange: String = "custom"
    ): Response<WrapperResponse>

    // Datos para gráficos
    @GET("stations-measurement/data-time-range-charts/{stationName}")
    suspend fun getWeatherDataForCharts(
        @Path("stationName") stationName: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("timeRange") timeRange: String = "custom"
    ): Response<ChartsResponse>

    // Datos desde/hasta (alternativo para filtros)
    @GET("stations-measurement/data-from-to/{stationName}")
    suspend fun getWeatherDataFromTo(
        @Path("stationName") stationName: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<List<WeatherData>>

    // Todos los datos de una estación
    @GET("stations-measurement/allByStationName/{stationName}")
    suspend fun getAllStationMeasurements(
        @Path("stationName") stationName: String
    ): Response<WrapperResponse>
}
