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

        binding.humidityTextView.text =
            "${sensors?.hcRelativeHumidity?.avg ?: "---"} %"
        binding.dewPointTextView.text =
            "${sensors?.dewPoint?.avg ?: "---"} °C"
        binding.airPressureTextView.text =
            "${sensors?.airPressure?.avg ?: "---"} hPa"
        binding.solarRadiationTextView.text =
            "${sensors?.solarRadiation?.avg ?: "---"} W/m²"
        binding.windDirectionTextView.text =
            "${sensors?.usonicWindDir?.last ?: "---"}°"
        binding.windSpeedTextView.text =
            "${sensors?.usonicWindSpeed?.avg ?: "---"} m/s"
        binding.windGustTextView.text =
            "${sensors?.windGust?.max ?: "---"} m/s"
        binding.rainLast1hTextView.text =
            "--- mm" // No viene directamente en sensors, se actualiza desde widget
        binding.rainLast24hTextView.text =
            "${sensors?.precipitation?.sum ?: "---"} mm"
        binding.rainLast48hTextView.text =
            "--- mm" // Se actualiza desde widget
        binding.rainLast7dTextView.text =
            "--- mm" // Se actualiza desde widget
    }

    private fun updateWeatherDataFromWidget(widgetData: WidgetData?) {
        widgetData?.let { widget ->
            // Actualizar datos principales desde el widget
            binding.humidityTextView.text = "${widget.relativeHumidity} %"
            binding.dewPointTextView.text = "${widget.dewPoint} °C"
            binding.airPressureTextView.text = "${widget.airPressure} hPa"
            binding.solarRadiationTextView.text = "${widget.solarRadiation} W/m²"
            binding.windSpeedTextView.text = "${widget.windSpeed} m/s"
            
            // Datos de lluvia que antes faltaban
            binding.rainLast1hTextView.text = "${widget.rainLastHour} mm"
            binding.rainLast24hTextView.text = "${widget.rain24h} mm"
            binding.rainLast48hTextView.text = "${widget.rain48h} mm"
            binding.rainLast7dTextView.text = "${widget.rain7d} mm"
            
            // Dirección del viento en texto
            binding.windDirectionTextView.text = widget.windDirection
        }
    }
}
