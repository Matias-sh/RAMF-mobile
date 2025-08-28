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
        val entries = mapDataToEntries(data, parameter)
        
        if (entries.isNotEmpty()) {
            // Calcular rango de tiempo real de los datos recibidos
            val timeRange = if (entries.size > 1) {
                (entries.last().x - entries.first().x).toLong()
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
            
            val dataSet = ChartUtils.createLineDataSet(entries, parameter, false)
            val lineData = LineData(dataSet)
            
            binding.lineChart.data = lineData
            binding.lineChart.animateXY(800, 600, Easing.EaseInOutCubic, Easing.EaseInOutCubic)
            binding.lineChart.invalidate()
            
            // Actualizar estadísticas
            updateStats(entries)
            
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
        return data.data.mapNotNull { weatherData ->
            val timestamp = parseTimestamp(weatherData.date)
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
                else -> null
            }
            
            if (timestamp != null && value != null) {
                Entry(timestamp.toFloat(), value.toFloat())
            } else null
        }.sortedBy { it.x }
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