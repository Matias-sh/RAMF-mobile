package com.cocido.ramf.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cocido.ramf.databinding.FragmentWeatherInfoBinding
import com.cocido.ramf.models.WeatherData
import com.cocido.ramf.models.WidgetData
import com.cocido.ramf.viewmodels.WeatherStationViewModel

class WeatherInfoFragment : Fragment() {

    private lateinit var binding: FragmentWeatherInfoBinding
    private lateinit var viewModel: WeatherStationViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWeatherInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[WeatherStationViewModel::class.java]

        // Escuchamos los datos del widget para información principal
        viewModel.widgetData.observe(viewLifecycleOwner) { widgetData ->
            updateWeatherDataFromWidget(widgetData)
        }

        // Escuchamos también los datos del último día como fallback
        viewModel.weatherDataLast.observe(viewLifecycleOwner) { weatherDataList ->
            val latest = weatherDataList.firstOrNull()
            updateWeatherDataFromSensors(latest)
        }
    }

    private fun updateWeatherDataFromSensors(data: WeatherData?) {
        val sensors = data?.sensors

        // Solo actualizar si no tenemos datos del widget ya cargados
        if (binding.humidityTextView.text == "--- %" || binding.humidityTextView.text.isEmpty()) {
            binding.humidityTextView.text = sensors?.hcRelativeHumidity?.avg?.let { 
                String.format("%.2f %%", it) 
            } ?: "--- %"
        }
        
        if (binding.dewPointTextView.text == "--- °C" || binding.dewPointTextView.text.isEmpty()) {
            binding.dewPointTextView.text = sensors?.dewPoint?.avg?.let { 
                String.format("%.1f °C", it) 
            } ?: "--- °C"
        }
        
        if (binding.airPressureTextView.text == "--- kPa" || binding.airPressureTextView.text.isEmpty()) {
            binding.airPressureTextView.text = sensors?.airPressure?.avg?.let { 
                String.format("%.4f kPa", it) // Mantener en kPa como muestra la web
            } ?: "--- kPa"
        }
        
        if (binding.solarRadiationTextView.text == "--- W/m²" || binding.solarRadiationTextView.text.isEmpty()) {
            binding.solarRadiationTextView.text = sensors?.solarRadiation?.avg?.let { 
                "${it.toInt()} W/m²" 
            } ?: "--- W/m²"
        }
        
        if (binding.windDirectionTextView.text == "---" || binding.windDirectionTextView.text.isEmpty()) {
            binding.windDirectionTextView.text = sensors?.usonicWindDir?.last?.let { 
                formatWindDirection(it) 
            } ?: "---"
        }
        
        if (binding.windSpeedTextView.text == "--- m/s" || binding.windSpeedTextView.text.isEmpty()) {
            binding.windSpeedTextView.text = sensors?.usonicWindSpeed?.avg?.let { 
                String.format("%.1f km/h", it / 3.6) // Convertir km/h a m/s
            } ?: "--- m/s"
        }
        
        // Ráfaga de viento desde sensores (prioritario sobre aproximación del widget)
        binding.windGustTextView.text = sensors?.windGust?.max?.let { 
            String.format("%.1f m/s", it) 
        } ?: "--- m/s"
        
        // Solo datos de precipitación 24h desde sensores si no vienen del widget
        if (binding.rainLast24hTextView.text == "--- mm" || binding.rainLast24hTextView.text.isEmpty()) {
            binding.rainLast24hTextView.text = sensors?.precipitation?.sum?.let { 
                if (it == 0.0) "0.0 mm" else String.format("%.1f mm", it) 
            } ?: "--- mm"
        }
    }

    private fun updateWeatherDataFromWidget(widgetData: WidgetData?) {
        widgetData?.let { widget ->
            android.util.Log.d("WeatherInfoFragment", "Widget data received: rainLastHour=${widget.rainLastHour}, rain24h=${widget.rain24h}, rain48h=${widget.rain48h}, rain7d=${widget.rain7d}")
            // Actualizar datos principales desde el widget con formatos exactos de la web
            binding.humidityTextView.text = String.format("%.2f %%", widget.relativeHumidity)
            binding.dewPointTextView.text = String.format("%.1f °C", widget.dewPoint)
            binding.airPressureTextView.text = String.format("%.4f kPa", widget.airPressure)
            binding.solarRadiationTextView.text = "${widget.solarRadiation} W/m²"
            binding.windSpeedTextView.text = String.format("%.1f m/s", widget.windSpeed)
            
            // Ráfaga de viento se actualiza desde sensores, no desde widget
            
            // Datos de lluvia con formato correcto - verificar que los valores no sean null o negativos
            binding.rainLast1hTextView.text = if (widget.rainLastHour >= 0) {
                if (widget.rainLastHour == 0.0) "0.0 mm" else String.format("%.1f mm", widget.rainLastHour)
            } else "--- mm"
            
            binding.rainLast24hTextView.text = if (widget.rain24h >= 0) {
                if (widget.rain24h == 0.0) "0.0 mm" else String.format("%.1f mm", widget.rain24h)
            } else "--- mm"
            
            binding.rainLast48hTextView.text = if (widget.rain48h >= 0) {
                if (widget.rain48h == 0.0) {
                    "0.0 mm"
                } else if (widget.rain48h < 10) {
                    String.format("%.1f mm", widget.rain48h)
                } else {
                    String.format("%.0f mm", widget.rain48h)
                }
            } else "--- mm"
            
            binding.rainLast7dTextView.text = if (widget.rain7d >= 0) {
                if (widget.rain7d == 0.0) {
                    "0.0 mm"
                } else if (widget.rain7d < 10) {
                    String.format("%.1f mm", widget.rain7d)
                } else {
                    String.format("%.0f mm", widget.rain7d)
                }
            } else "--- mm"
            
            // Dirección del viento simplificada como en la web
            binding.windDirectionTextView.text = simplifyWindDirection(widget.windDirection)
        }
    }
    
    private fun formatWindDirection(degrees: Double): String {
        return when {
            degrees >= 337.5 || degrees < 22.5 -> "Norte, N"
            degrees < 67.5 -> "Noreste, NE" 
            degrees < 112.5 -> "Este, E"
            degrees < 157.5 -> "Sureste, SE"
            degrees < 202.5 -> "Sur, S"
            degrees < 247.5 -> "Suroeste, SO"
            degrees < 292.5 -> "Oeste, O"
            degrees < 337.5 -> "Noroeste, NO"
            else -> "Noroeste, NO"
        }
    }
    
    private fun simplifyWindDirection(windDirection: String): String {
        // Simplificar direcciones del viento para coincidir exactamente con la web oficial
        return when {
            windDirection.contains("Noroeste", ignoreCase = true) -> "Noroeste, NE" // Como muestra la web oficial
            windDirection.contains("Noreste", ignoreCase = true) -> "Noreste, NE"
            windDirection.contains("Suroeste", ignoreCase = true) -> "Suroeste, SO"
            windDirection.contains("Sureste", ignoreCase = true) -> "Sureste, SE"
            windDirection.contains("Norte", ignoreCase = true) -> "Norte, N"
            windDirection.contains("Sur", ignoreCase = true) -> "Sur, S"
            windDirection.contains("Este", ignoreCase = true) -> "Este, E"
            windDirection.contains("Oeste", ignoreCase = true) -> "Oeste, O"
            else -> windDirection
        }
    }
}
