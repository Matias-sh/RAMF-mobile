package com.cocido.ramf.viewmodels

import androidx.lifecycle.*
import com.cocido.ramf.models.*
import com.cocido.ramf.network.RetrofitClient
import kotlinx.coroutines.launch

class WeatherStationViewModel : ViewModel() {

    private val _weatherStations = MutableLiveData<List<WeatherStation>>()
    val weatherStations: LiveData<List<WeatherStation>> = _weatherStations

    private val _selectedStationData = MutableLiveData<WeatherStation?>()
    val selectedStationData: LiveData<WeatherStation?> = _selectedStationData

    private val _temperatureMaxMin = MutableLiveData<TemperatureMaxMin>()
    val temperatureMaxMin: LiveData<TemperatureMaxMin> = _temperatureMaxMin

    private val _weatherDataLast = MutableLiveData<List<WeatherData>>()
    val weatherDataLast: LiveData<List<WeatherData>> = _weatherDataLast

    // Alias para no romper observadores existentes en MainActivity
    val weatherDataLastDay: LiveData<List<WeatherData>> get() = _weatherDataLast

    private val _widgetData = MutableLiveData<WidgetData>()
    val widgetData: LiveData<WidgetData> = _widgetData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchWeatherStations() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.weatherStationService.getWeatherStations()
                if (response.isSuccessful) {
                    _weatherStations.postValue(response.body()?.data ?: emptyList())
                    // Si querés seleccionar por defecto la primera:
                    response.body()?.data?.firstOrNull()?.let { _selectedStationData.postValue(it) }
                } else {
                    _error.postValue("Error al obtener estaciones: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.postValue("Error al obtener estaciones: ${e.message}")
            }
        }
    }

    /**
     * Busca la estación por stationName en la lista ya cargada
     */
    fun fetchStationData(stationName: String) {
        val list = _weatherStations.value.orEmpty()
        val station = list.find { it.id == stationName }
        if (station != null) {
            _selectedStationData.postValue(station)
        } else {
            _error.postValue("Estación no encontrada: $stationName")
        }
    }

    fun fetchTemperatureMaxMin(stationName: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.weatherStationService.getTemperatureMaxMin(stationName)
                if (response.isSuccessful) {
                    response.body()?.let { tempMaxMin ->
                        _temperatureMaxMin.postValue(tempMaxMin)
                    }
                } else if (response.code() != 404) {
                    // Solo mostrar error si no es 404 (estación no encontrada es común)
                    _error.postValue("Error al obtener temperatura máx/mín: ${response.code()}")
                }
            } catch (e: Exception) {
                // Solo mostrar errores de conexión, no de estaciones no encontradas
                if (e.message?.contains("404", ignoreCase = true) != true) {
                    _error.postValue("Error al obtener temperatura máx/mín: ${e.message}")
                }
            }
        }
    }

    fun fetchWeatherDataLastDay(stationName: String) {
        viewModelScope.launch {
            try {
                // Fecha de hoy y ayer dinámicamente
                val calendar = java.util.Calendar.getInstance()
                val today = java.text.SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'", java.util.Locale.US).format(calendar.time)
                
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'", java.util.Locale.US).format(calendar.time)
                
                val from = yesterday
                val to   = today

                // Nueva API: usa stationName en lugar de stationId
                val response = RetrofitClient.weatherStationService.getWeatherDataTimeRange(
                    stationName = stationName,
                    from = from,
                    to = to
                )
                if (response.isSuccessful) {
                    _weatherDataLast.postValue(response.body()?.data ?: emptyList())
                } else {
                    _error.postValue("Error al obtener datos: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _error.postValue("Error al obtener datos: ${e.message}")
            }
        }
    }

    fun fetchWidgetData(stationName: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.weatherStationService.getWidgetData(stationName)
                if (response.isSuccessful) {
                    response.body()?.let { widgetData ->
                        _widgetData.postValue(widgetData)
                    }
                } else if (response.code() != 404) {
                    // Solo mostrar error si no es 404 (estación no encontrada es común)
                    _error.postValue("Error al obtener datos del widget: ${response.code()}")
                }
            } catch (e: Exception) {
                // Solo mostrar errores de conexión, no de estaciones no encontradas
                if (e.message?.contains("404", ignoreCase = true) != true) {
                    _error.postValue("Error al obtener datos del widget: ${e.message}")
                }
            }
        }
    }
}
