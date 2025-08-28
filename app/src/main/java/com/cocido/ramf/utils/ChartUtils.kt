package com.cocido.ramf.utils

import android.graphics.Color
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

object ChartUtils {
    
    val PARAMETER_COLORS = mapOf(
        "temperatura" to Color.rgb(255, 87, 34),      // Orange
        "humedad" to Color.rgb(33, 150, 243),         // Blue
        "radiacion" to Color.rgb(255, 193, 7),        // Yellow/Amber
        "precipitacion" to Color.rgb(76, 175, 80),    // Green
        "direccionViento" to Color.rgb(156, 39, 176), // Purple
        "vientoVel" to Color.rgb(96, 125, 139),       // Blue Grey
        "solarDuration" to Color.rgb(255, 152, 0),    // Orange (lighter)
        "dewPoint" to Color.rgb(0, 150, 136),         // Teal
        "airPressure" to Color.rgb(121, 85, 72),      // Brown
        "windGust" to Color.rgb(158, 158, 158)        // Grey
    )
    
    val PARAMETER_UNITS = mapOf(
        "temperatura" to "°C",
        "humedad" to "%",
        "radiacion" to "W/m²",
        "precipitacion" to "mm",
        "direccionViento" to "°",
        "vientoVel" to "m/s",
        "solarDuration" to "h",
        "dewPoint" to "°C",
        "airPressure" to "hPa",
        "windGust" to "m/s"
    )
    
    val PARAMETER_RANGES = mapOf(
        "temperatura" to Pair(-30f, 60f),      // Rango típico para estación meteorológica
        "humedad" to Pair(0f, 100f),          // Porcentaje de humedad
        "radiacion" to Pair(0f, 1500f),       // W/m² radiación solar máxima teórica
        "precipitacion" to Pair(0f, 200f),    // mm/hora máximo extremo
        "direccionViento" to Pair(0f, 360f),  // Grados brújula
        "vientoVel" to Pair(0f, 50f),         // m/s velocidad viento máxima típica
        "dewPoint" to Pair(-40f, 40f),        // °C punto de rocío
        "airPressure" to Pair(950f, 1050f),   // hPa presión atmosférica típica
        "windGust" to Pair(0f, 80f)           // m/s ráfagas máximas extremas
    )
    
    val PARAMETER_DECIMAL_PLACES = mapOf(
        "temperatura" to 1,
        "humedad" to 0,           // % sin decimales
        "radiacion" to 0,         // W/m² sin decimales
        "precipitacion" to 2,     // mm con 2 decimales para precisión
        "direccionViento" to 0,   // grados sin decimales
        "vientoVel" to 1,
        "dewPoint" to 1,
        "airPressure" to 1,
        "windGust" to 1
    )
    
    val PARAMETER_LABELS = mapOf(
        "temperatura" to "Temperatura del aire",
        "humedad" to "Humedad relativa",
        "radiacion" to "Radiación solar",
        "precipitacion" to "Precipitación",
        "direccionViento" to "Dirección del viento",
        "vientoVel" to "Velocidad del viento",
        "solarDuration" to "Duración solar",
        "dewPoint" to "Punto de rocío",
        "airPressure" to "Presión atmosférica",
        "windGust" to "Ráfagas de viento"
    )
    
    fun createLineDataSet(
        entries: List<Entry>,
        parameter: String,
        isMultiParameter: Boolean = false
    ): LineDataSet {
        val color = PARAMETER_COLORS[parameter] ?: Color.rgb(33, 150, 243)
        val label = getParameterLabelWithUnit(parameter)
        
        return LineDataSet(entries, label).apply {
            // Color configuration mejorada
            this.color = color
            lineWidth = if (isMultiParameter) 2.5f else 3f
            
            // Circle configuration para mejor visualización
            setDrawCircles(entries.size <= 50) // Solo mostrar círculos si hay pocos datos
            setCircleColor(color)
            circleRadius = 3f
            setDrawCircleHole(true)
            circleHoleRadius = 1.5f
            circleHoleColor = Color.WHITE
            
            // Fill configuration con gradiente sutil
            setDrawFilled(true)
            fillAlpha = 30
            fillColor = color
            
            // Value configuration
            setDrawValues(false)
            valueTextColor = Color.DKGRAY
            valueTextSize = 9f
            
            // Line style más suave
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            
            // Highlight configuration mejorada
            isHighlightEnabled = true
            setDrawHorizontalHighlightIndicator(true)
            setDrawVerticalHighlightIndicator(true)
            highlightLineWidth = 1.5f
            highLightColor = adjustColorBrightness(color, 0.8f)
            
            // Configuración adicional para suavizar líneas
            setDrawIcons(false)
            
            // Configuración de transparencia para líneas superpuestas
            if (isMultiParameter) {
                fillAlpha = 20
            }
        }
    }
    
    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = Math.round(Color.red(color) * factor).coerceIn(0, 255)
        val g = Math.round(Color.green(color) * factor).coerceIn(0, 255)
        val b = Math.round(Color.blue(color) * factor).coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
    
    fun getParameterColor(parameter: String): Int {
        return PARAMETER_COLORS[parameter] ?: Color.rgb(33, 150, 243)
    }
    
    fun getParameterUnit(parameter: String): String {
        return PARAMETER_UNITS[parameter] ?: ""
    }
    
    fun getParameterLabel(parameter: String): String {
        return PARAMETER_LABELS[parameter] ?: parameter
    }
    
    fun getParameterLabelWithUnit(parameter: String): String {
        val baseLabel = PARAMETER_LABELS[parameter] ?: parameter
        val unit = getParameterUnit(parameter)
        return if (unit.isNotEmpty()) "$baseLabel ($unit)" else baseLabel
    }
    
    fun createTimeFormatter(): ValueFormatter {
        return object : ValueFormatter() {
            private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            override fun getFormattedValue(value: Float): String {
                return try {
                    timeFormatter.format(Date(value.toLong()))
                } catch (e: Exception) {
                    ""
                }
            }
        }
    }
    
    fun createDynamicTimeFormatter(timeRangeMillis: Long): ValueFormatter {
        return object : ValueFormatter() {
            private val formatter = when {
                timeRangeMillis <= 259200000L -> SimpleDateFormat("HH:mm", Locale.getDefault())    // <= 3d: formato HH:mm como en la web
                else -> SimpleDateFormat("dd/MM", Locale.getDefault())                            // > 3d: formato dd/MM
            }
            
            override fun getFormattedValue(value: Float): String {
                return try {
                    formatter.format(Date(value.toLong()))
                } catch (e: Exception) {
                    ""
                }
            }
        }
    }
    
    fun createDateFormatter(): ValueFormatter {
        return object : ValueFormatter() {
            private val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())
            
            override fun getFormattedValue(value: Float): String {
                return try {
                    dateFormatter.format(Date(value.toLong()))
                } catch (e: Exception) {
                    ""
                }
            }
        }
    }
    
    fun optimizeEntries(entries: List<Entry>, maxEntries: Int = Constants.MAX_CHART_ENTRIES): List<Entry> {
        if (entries.size <= maxEntries) return entries
        
        val step = entries.size / maxEntries
        return entries.filterIndexed { index, _ -> index % step == 0 }
    }
    
    fun validateEntries(entries: List<Entry>): List<Entry> {
        return entries.filter { entry ->
            entry.x.isFinite() && entry.y.isFinite() && !entry.y.isNaN()
        }.sortedBy { it.x }
    }
    
    fun getParameterRange(parameter: String): Pair<Float, Float>? {
        return PARAMETER_RANGES[parameter]
    }
    
    fun getParameterDecimalPlaces(parameter: String): Int {
        return PARAMETER_DECIMAL_PLACES[parameter] ?: 1
    }
    
    fun formatParameterValue(value: Float, parameter: String): String {
        val decimals = getParameterDecimalPlaces(parameter)
        val unit = getParameterUnit(parameter)
        return String.format("%.${decimals}f %s", value, unit)
    }
    
    fun calculateOptimalGranularity(timeRangeMillis: Long): Float {
        return when {
            timeRangeMillis <= 3600000L -> 600000f      // <= 1h: 10 min (como en la web)
            timeRangeMillis <= 21600000L -> 1800000f    // <= 6h: 30 min (mejor espaciado)
            timeRangeMillis <= 86400000L -> 7200000f    // <= 1d: 2h (como en la web)
            timeRangeMillis <= 604800000L -> 43200000f  // <= 1w: 12h
            else -> 172800000f                          // > 1w: 2d
        }
    }
    
    fun calculateOptimalLabelCount(timeRangeMillis: Long): Int {
        return when {
            timeRangeMillis <= 3600000L -> 6           // <= 1h: 6 etiquetas (cada 10min, como en la web)
            timeRangeMillis <= 21600000L -> 12         // <= 6h: 12 etiquetas (cada 30min)
            timeRangeMillis <= 86400000L -> 12         // <= 1d: 12 etiquetas (cada 2h, como en la web)
            timeRangeMillis <= 604800000L -> 14        // <= 1w: 14 etiquetas (cada 12h)
            else -> 15                                 // > 1w: 15 etiquetas (cada 2d)
        }
    }
}