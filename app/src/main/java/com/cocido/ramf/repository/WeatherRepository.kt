package com.cocido.ramf.repository

import com.cocido.ramf.models.WrapperResponse
import com.cocido.ramf.models.WeatherStation
import com.cocido.ramf.models.WeatherStationsResponse
import com.cocido.ramf.models.WidgetData
import com.cocido.ramf.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class WeatherRepository {
    
    private val weatherService = RetrofitClient.weatherStationService
    
    suspend fun getWeatherStations(): Result<List<WeatherStation>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = weatherService.getWeatherStations()
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.data)
                } else {
                    Result.Error(Exception("Error ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }
    
    suspend fun getWeatherData(
        stationName: String,
        from: String,
        to: String
    ): Result<WrapperResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Nueva API local: usa stationName en lugar de stationId
                val response = weatherService.getWeatherDataTimeRange(
                    stationName = stationName,
                    from = from,
                    to = to
                )
                
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error(Exception("Error ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }
    
    suspend fun getWeatherDataWithRetry(
        stationName: String,
        from: String,
        to: String,
        maxRetries: Int = 3
    ): Result<WrapperResponse> {
        repeat(maxRetries) { attempt ->
            when (val result = getWeatherData(stationName, from, to)) {
                is Result.Success -> return result
                is Result.Error -> {
                    if (attempt == maxRetries - 1) return result
                    kotlinx.coroutines.delay((1000 * (attempt + 1)).toLong()) // Backoff exponencial
                }
                else -> {}
            }
        }
        return Result.Error(Exception("Máximo número de reintentos alcanzado"))
    }
    
    suspend fun getWeatherDataForCharts(
        stationName: String,
        from: String,
        to: String
    ): Result<WrapperResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = weatherService.getWeatherDataForCharts(
                    stationName = stationName,
                    from = from,
                    to = to
                )
                
                if (response.isSuccessful && response.body() != null) {
                    // La API devuelve List<WeatherData> directamente, lo envolvemos en WrapperResponse
                    val wrappedResponse = WrapperResponse(data = response.body()!!)
                    Result.Success(wrappedResponse)
                } else {
                    Result.Error(Exception("Error ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }
    
    suspend fun getWidgetData(stationName: String): Result<WidgetData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = weatherService.getWidgetData(stationName)
                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error(Exception("Error ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }
}