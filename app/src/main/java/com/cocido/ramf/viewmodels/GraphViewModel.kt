package com.cocido.ramf.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cocido.ramf.models.WeatherStation
import com.cocido.ramf.models.WrapperResponse
import com.cocido.ramf.repository.Result
import com.cocido.ramf.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GraphUiState(
    val isLoading: Boolean = false,
    val weatherData: WrapperResponse? = null,
    val stations: List<WeatherStation> = emptyList(),
    val currentStationId: String = "00210E7D",
    val selectedParameters: Set<String> = setOf("temperatura"),
    val errorMessage: String? = null,
    val dateRangeLabel: String = "24h"
)

class GraphViewModel(private val repository: WeatherRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()
    
    init {
        loadStations()
    }
    
    private fun loadStations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = repository.getWeatherStations()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        stations = result.data,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar estaciones: ${result.exception.localizedMessage}"
                    )
                }
                else -> {}
            }
        }
    }
    
    fun loadWeatherData(from: String, to: String) {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
            
            when (val result = repository.getWeatherDataForCharts(
                stationName = currentState.currentStationId,
                from = from,
                to = to
            )) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        weatherData = result.data,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = getErrorMessage(result.exception)
                    )
                }
                else -> {}
            }
        }
    }
    
    fun selectStation(stationId: String) {
        val currentState = _uiState.value
        if (currentState.currentStationId != stationId) {
            _uiState.value = currentState.copy(currentStationId = stationId)
            // Recargar datos con la nueva estación
            if (currentState.weatherData != null) {
                // Usar las mismas fechas que se usaron anteriormente
                loadWeatherData(
                    from = generateDateRange().first,
                    to = generateDateRange().second
                )
            }
        }
    }
    
    fun updateSelectedParameters(parameters: Set<String>) {
        _uiState.value = _uiState.value.copy(selectedParameters = parameters)
    }
    
    fun updateDateRangeLabel(label: String) {
        _uiState.value = _uiState.value.copy(dateRangeLabel = label)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun retry() {
        val (from, to) = generateDateRange()
        loadWeatherData(from, to)
    }
    
    private fun getErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("timeout", true) == true -> "Tiempo de espera agotado"
            exception.message?.contains("network", true) == true -> "Error de red. Verifica tu conexión"
            exception.message?.contains("connection", true) == true -> "Sin conexión a internet"
            exception.message?.contains("404") == true -> "Estación no encontrada"
            exception.message?.contains("500") == true -> "Error del servidor"
            else -> "Error inesperado: ${exception.localizedMessage ?: "Desconocido"}"
        }
    }
    
    private fun generateDateRange(): Pair<String, String> {
        // Por simplicidad, usar las últimas 24 horas
        // En una implementación completa, esto vendría del estado
        val calendar = java.util.Calendar.getInstance()
        val isoFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        
        val endDate = calendar.time
        val to = isoFormatter.format(endDate)
        
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val startDate = calendar.time
        val from = isoFormatter.format(startDate)
        
        return Pair(from, to)
    }
}

class GraphViewModelFactory(
    private val repository: WeatherRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GraphViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GraphViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}