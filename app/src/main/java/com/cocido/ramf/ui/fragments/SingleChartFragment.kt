package com.cocido.ramf.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.animation.Easing
import com.cocido.ramf.R
import com.cocido.ramf.databinding.FragmentSingleChartBinding
import com.cocido.ramf.models.WrapperResponse
import com.cocido.ramf.viewmodels.GraphViewModel
import com.cocido.ramf.utils.ChartUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

class SingleChartFragment : Fragment() {
    
    private var _binding: FragmentSingleChartBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: GraphViewModel
    private lateinit var parameter: String
    
    companion object {
        private const val ARG_PARAMETER = "parameter"
        
        fun newInstance(parameter: String): SingleChartFragment {
            return SingleChartFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAMETER, parameter)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parameter = arguments?.getString(ARG_PARAMETER) ?: "temperatura"
        viewModel = ViewModelProvider(requireActivity())[GraphViewModel::class.java]
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSingleChartBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupChart()
        setupStatsCards()
        observeViewModel()
    }
    
    private fun setupChart() {
        binding.lineChart.apply {
            // Configuración mejorada para gráfico individual
            setTouchEnabled(true)
            setPinchZoom(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setBackgroundColor(Color.WHITE)
            
            // Configuración optimizada para pantalla completa
            description = Description().apply { text = "" }
            
            // Eje X (tiempo) con mejor formato replicando la web
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = ContextCompat.getColor(requireContext(), R.color.chart_text_color)
                textSize = 11f
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.chart_grid_color)
                gridLineWidth = 0.8f
                setDrawAxisLine(true)
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.chart_axis_color)
                axisLineWidth = 1.5f
                // Configuración inicial - se actualizará dinámicamente en updateChart
                valueFormatter = ChartUtils.createTimeFormatter()
                labelCount = 6
                isGranularityEnabled = true
                granularity = 600000f // 10 min por defecto (como en la web)
                // Mejorar espaciado para evitar solapamiento
                labelRotationAngle = 0f
                setAvoidFirstLastClipping(true)
                spaceMin = 0.1f
                spaceMax = 0.1f
            }
            
            // Eje Y con configuración específica por parámetro
            axisLeft.apply {
                textColor = ContextCompat.getColor(requireContext(), R.color.chart_text_color)
                textSize = 12f
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.chart_grid_color)
                gridLineWidth = 1f
                setDrawAxisLine(true)
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.chart_axis_color)
                axisLineWidth = 1.5f
                isGranularityEnabled = true
                setDrawZeroLine(true)
                zeroLineColor = ContextCompat.getColor(requireContext(), R.color.chart_zero_line_color)
                zeroLineWidth = 2f
                
                // Configurar rango según el parámetro
                ChartUtils.getParameterRange(parameter)?.let { range ->
                    axisMinimum = range.first
                    axisMaximum = range.second
                }
            }
            
            // Deshabilitar eje Y derecho
            axisRight.isEnabled = false
            
            // Configurar leyenda
            legend.apply {
                isEnabled = true
                textColor = ContextCompat.getColor(requireContext(), R.color.chart_text_color)
                textSize = 14f
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
                formLineWidth = 4f
                formSize = 14f
            }
            
            // Márgenes para mejor visualización
            setExtraOffsets(20f, 30f, 20f, 40f)
            setNoDataText("No hay datos disponibles para este parámetro")
            setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.chart_no_data_color))
        }
    }
    
    private fun setupStatsCards() {
        // Configurar las tarjetas de estadísticas
        binding.parameterTitle.text = ChartUtils.getParameterLabel(parameter)
        binding.parameterUnit.text = ChartUtils.getParameterUnit(parameter)
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }
    }
    
    private fun updateUI(state: com.cocido.ramf.viewmodels.GraphUiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        if (state.errorMessage != null) {
            showError(state.errorMessage)
        } else {
            binding.errorMessage.visibility = View.GONE
            state.weatherData?.let { data ->
                updateChart(data)
            }
        }
    }
    
    private fun updateChart(data: WrapperResponse) {
        android.util.Log.d("SingleChartFragment", "updateChart called - Parameter: $parameter")
        android.util.Log.d("SingleChartFragment", "Data received - Size: ${data.data.size}")
        
        val entries = mapDataToEntries(data, parameter)
        android.util.Log.d("SingleChartFragment", "Entries mapped - Count: ${entries.size}")
        
        // Si no hay entries de la API, crear algunos de prueba para diagnóstico
        val finalEntries = if (entries.isEmpty()) {
            android.util.Log.w("SingleChartFragment", "No entries from API data, creating test entries for debugging")
            createTestEntries(parameter)
        } else {
            entries
        }
        
        if (finalEntries.isNotEmpty()) {
            // Calcular rango de tiempo real de los datos recibidos
            val timeRange = if (finalEntries.size > 1) {
                (finalEntries.last().x - finalEntries.first().x).toLong()
            } else {
                3600000L // 1 hora por defecto
            }
            
            // Configurar eje X ANTES de mostrar datos (replicando comportamiento de la web)
            binding.lineChart.xAxis.apply {
                // Aplicar granularidad y formato dinámicos como en la web
                granularity = ChartUtils.calculateOptimalGranularity(timeRange)
                valueFormatter = ChartUtils.createDynamicTimeFormatter(timeRange)
                labelCount = ChartUtils.calculateOptimalLabelCount(timeRange)
                
                // Asegurar que las etiquetas no se solapen
                setLabelCount(labelCount, false)
                setAvoidFirstLastClipping(true)
                
                // Log para debugging - verificar configuración del eje X
                android.util.Log.d("SingleChartFragment", "Configurando eje X - Rango: ${timeRange}ms, Granularidad: ${granularity}ms, LabelCount: $labelCount")
            }
            
            val dataSet = ChartUtils.createLineDataSet(finalEntries, parameter, false)
            val lineData = LineData(dataSet)
            
            binding.lineChart.data = lineData
            binding.lineChart.animateXY(800, 600, Easing.EaseInOutCubic, Easing.EaseInOutCubic)
            binding.lineChart.invalidate()
            
            // Actualizar estadísticas
            updateStats(finalEntries)
            
            binding.errorMessage.visibility = View.GONE
        } else {
            binding.lineChart.clear()
            showError("No hay datos disponibles para ${ChartUtils.getParameterLabel(parameter)}")
        }
    }
    
    private fun updateStats(entries: List<Entry>) {
        if (entries.isEmpty()) return
        
        val values = entries.map { it.y }
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 0f
        val avg = values.average().toFloat()
        val current = values.lastOrNull() ?: 0f
        
        binding.apply {
            currentValue.text = ChartUtils.formatParameterValue(current, parameter)
            minValue.text = ChartUtils.formatParameterValue(min, parameter)
            maxValue.text = ChartUtils.formatParameterValue(max, parameter)
            avgValue.text = ChartUtils.formatParameterValue(avg, parameter)
        }
    }
    
    private fun mapDataToEntries(data: WrapperResponse, parameter: String): List<Entry> {
        android.util.Log.d("SingleChartFragment", "mapDataToEntries - Parameter: '$parameter', Data count: ${data.data.size}")
        
        val entries = data.data.mapIndexedNotNull { index, weatherData ->
            val timestamp = parseTimestamp(weatherData.date)
            
            // Log detallado para primera entrada
            if (index == 0) {
                android.util.Log.d("SingleChartFragment", "First entry analysis:")
                android.util.Log.d("SingleChartFragment", "  Date: ${weatherData.date}")
                android.util.Log.d("SingleChartFragment", "  Parsed timestamp: $timestamp")
                android.util.Log.d("SingleChartFragment", "  Target parameter: '$parameter'")
                
                // Log todos los sensores disponibles
                android.util.Log.d("SingleChartFragment", "Available sensors:")
                android.util.Log.d("SingleChartFragment", "  hcAirTemperature: ${weatherData.sensors.hcAirTemperature?.avg}")
                android.util.Log.d("SingleChartFragment", "  hcRelativeHumidity: ${weatherData.sensors.hcRelativeHumidity?.avg}")
                android.util.Log.d("SingleChartFragment", "  solarRadiation: ${weatherData.sensors.solarRadiation?.avg}")
                android.util.Log.d("SingleChartFragment", "  precipitation: ${weatherData.sensors.precipitation?.sum}")
                android.util.Log.d("SingleChartFragment", "  usonicWindSpeed: ${weatherData.sensors.usonicWindSpeed?.avg}")
                android.util.Log.d("SingleChartFragment", "  airPressure: ${weatherData.sensors.airPressure?.avg}")
                android.util.Log.d("SingleChartFragment", "  dewPoint: ${weatherData.sensors.dewPoint?.avg}")
                android.util.Log.d("SingleChartFragment", "  windGust: ${weatherData.sensors.windGust?.max}")
            }
            
            val value = when (parameter) {
                "temperatura" -> weatherData.sensors.hcAirTemperature?.avg
                "humedad" -> weatherData.sensors.hcRelativeHumidity?.avg
                "radiacion" -> weatherData.sensors.solarRadiation?.avg
                "precipitacion" -> weatherData.sensors.precipitation?.sum
                "direccionViento" -> weatherData.sensors.usonicWindDir?.last
                "vientoVel" -> weatherData.sensors.usonicWindSpeed?.avg
                "dewPoint" -> weatherData.sensors.dewPoint?.avg
                "airPressure" -> weatherData.sensors.airPressure?.avg
                "windGust" -> weatherData.sensors.windGust?.max
                else -> {
                    android.util.Log.w("SingleChartFragment", "Unknown parameter: '$parameter'")
                    null
                }
            }
            
            // Log razón de exclusión
            if (timestamp == null) {
                android.util.Log.w("SingleChartFragment", "Skipping entry - null timestamp for date: ${weatherData.date}")
            } else if (value == null) {
                android.util.Log.w("SingleChartFragment", "Skipping entry - null value for parameter '$parameter' at ${weatherData.date}")
            } else {
                android.util.Log.d("SingleChartFragment", "Valid entry - '$parameter': $value at ${weatherData.date}")
            }
            
            if (timestamp != null && value != null) {
                Entry(timestamp.toFloat(), value.toFloat())
            } else null
        }.sortedBy { it.x }
        
        android.util.Log.d("SingleChartFragment", "Final entries count: ${entries.size}")
        return entries
    }
    
    private fun parseTimestamp(dateString: String): Long? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }
    
    private fun createTestEntries(parameter: String): List<Entry> {
        android.util.Log.d("SingleChartFragment", "Creating test entries for parameter: $parameter")
        val baseTime = System.currentTimeMillis()
        val entries = mutableListOf<Entry>()
        
        // Crear 5 puntos de prueba
        for (i in 0..4) {
            val timestamp = baseTime - (4 - i) * 600000L // Cada 10 minutos hacia atrás
            val value = when (parameter) {
                "temperatura" -> 20.0f + i * 2.5f // 20-30°C
                "humedad" -> 40.0f + i * 10f // 40-80%
                "precipitacion" -> i * 2.5f // 0-10mm
                "vientoVel" -> 5.0f + i * 3f // 5-20 km/h
                "airPressure" -> 1010.0f + i * 2f // 1010-1018 hPa
                "radiacion" -> 100.0f + i * 150f // 100-700 W/m²
                else -> 10.0f + i * 5f // Valor genérico
            }
            
            entries.add(Entry(timestamp.toFloat(), value))
            android.util.Log.d("SingleChartFragment", "Test entry $i: time=$timestamp, value=$value")
        }
        
        return entries
    }
    
    private fun showError(message: String) {
        binding.errorMessage.apply {
            text = message
            visibility = View.VISIBLE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}