package com.cocido.ramf.ui.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import java.text.SimpleDateFormat
import java.util.*
import com.cocido.ramf.R
import com.cocido.ramf.databinding.ActivityFullScreenChartsBinding
import com.cocido.ramf.ui.adapters.ChartsViewPagerAdapter
import com.cocido.ramf.viewmodels.GraphViewModel
import com.cocido.ramf.viewmodels.GraphViewModelFactory
import com.cocido.ramf.repository.WeatherRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class FullScreenChartsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFullScreenChartsBinding
    private lateinit var chartsAdapter: ChartsViewPagerAdapter
    
    private val viewModel: GraphViewModel by viewModels {
        GraphViewModelFactory(WeatherRepository())
    }
    
    companion object {
        const val EXTRA_STATION_ID = "station_id"
        const val EXTRA_STATION_NAME = "station_name"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenChartsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViewPager()
        setupFilters()
        observeViewModel()
        loadInitialData()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = intent.getStringExtra(EXTRA_STATION_NAME) ?: "Gráficos"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupViewPager() {
        chartsAdapter = ChartsViewPagerAdapter(this)
        binding.viewPager.adapter = chartsAdapter
        
        // Configurar TabLayout con los parámetros meteorológicos
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Temperatura"
                1 -> "Humedad"
                2 -> "Precipitación"
                3 -> "Viento"
                4 -> "Presión"
                5 -> "Radiación"
                else -> "Parámetro ${position + 1}"
            }
        }.attach()
        
        // Configurar orientación horizontal para swipe entre gráficos
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // Actualizar UI según el estado
                    updateLoadingState(state.isLoading)
                    
                    if (state.errorMessage != null) {
                        showError(state.errorMessage)
                    }
                    
                    // Los fragmentos individuales se encargarán de mostrar sus datos
                }
            }
        }
    }
    
    private fun setupFilters() {
        // Variables para fechas
        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        // Configurar header expandible
        setupExpandableFilters()
        
        // Configurar selectores de fecha
        binding.fromDateEditText.setOnClickListener {
            showDateTimePicker { selectedDate ->
                binding.fromDateEditText.setText(dateFormatter.format(selectedDate))
            }
        }
        
        binding.toDateEditText.setOnClickListener {
            showDateTimePicker { selectedDate ->
                binding.toDateEditText.setText(dateFormatter.format(selectedDate))
            }
        }
        
        // Configurar chips de filtros rápidos
        binding.chipGroupTimeRange.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedChipId = checkedIds[0]
                val (from, to) = when (selectedChipId) {
                    R.id.chip1Hour -> generateTimeRange(1, Calendar.HOUR_OF_DAY)
                    R.id.chip6Hours -> generateTimeRange(6, Calendar.HOUR_OF_DAY)
                    R.id.chip1Day -> generateTimeRange(1, Calendar.DAY_OF_YEAR)
                    R.id.chip1Week -> generateTimeRange(7, Calendar.DAY_OF_YEAR)
                    R.id.chip1Month -> generateTimeRange(30, Calendar.DAY_OF_YEAR)
                    else -> generateTimeRange(1, Calendar.HOUR_OF_DAY)
                }
                
                // Actualizar campos de fecha
                binding.fromDateEditText.setText(dateFormatter.format(Date(from)))
                binding.toDateEditText.setText(dateFormatter.format(Date(to)))
                
                // Cargar datos automáticamente
                applyTimeFilter(from, to)
            }
        }
        
        // Configurar botón buscar
        binding.searchButton.setOnClickListener {
            applyCustomDateFilter()
        }
        
        // Establecer filtro inicial de 1 hora
        val (from, to) = generateTimeRange(1, Calendar.HOUR_OF_DAY)
        binding.fromDateEditText.setText(dateFormatter.format(Date(from)))
        binding.toDateEditText.setText(dateFormatter.format(Date(to)))
    }
    
    private fun showDateTimePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                
                // Después de seleccionar fecha, mostrar picker de hora
                android.app.TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onDateSelected(calendar.time)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun generateTimeRange(amount: Int, field: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(field, -amount)
        val startTime = calendar.timeInMillis
        return Pair(startTime, endTime)
    }
    
    private fun applyTimeFilter(fromMillis: Long, toMillis: Long) {
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        val fromIso = isoFormatter.format(Date(fromMillis))
        val toIso = isoFormatter.format(Date(toMillis))
        
        Log.d("FullScreenCharts", "Aplicando filtro: $fromIso - $toIso")
        viewModel.loadWeatherData(fromIso, toIso)
    }
    
    private fun applyCustomDateFilter() {
        val fromText = binding.fromDateEditText.text.toString()
        val toText = binding.toDateEditText.text.toString()
        
        if (fromText.isNotEmpty() && toText.isNotEmpty()) {
            try {
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val fromDate = dateFormatter.parse(fromText)
                val toDate = dateFormatter.parse(toText)
                
                if (fromDate != null && toDate != null) {
                    applyTimeFilter(fromDate.time, toDate.time)
                }
            } catch (e: Exception) {
                showError("Error en formato de fecha. Use: dd/MM/yyyy HH:mm")
            }
        } else {
            showError("Por favor seleccione fechas de inicio y fin")
        }
    }
    
    private fun loadInitialData() {
        val stationId = intent.getStringExtra(EXTRA_STATION_ID)
        if (stationId != null) {
            viewModel.selectStation(stationId)
            // Cargar datos para las últimas 24 horas por defecto
            val (from, to) = generateLast24HoursRange()
            viewModel.loadWeatherData(from, to)
        }
    }
    
    private fun generateLast24HoursRange(): Pair<String, String> {
        val calendar = java.util.Calendar.getInstance()
        val isoFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        
        val endDate = calendar.time
        calendar.add(java.util.Calendar.HOUR_OF_DAY, -24)
        val startDate = calendar.time
        
        return Pair(isoFormatter.format(startDate), isoFormatter.format(endDate))
    }
    
    private fun updateLoadingState(isLoading: Boolean) {
        // Implementar loading state si es necesario
    }
    
    private fun setupExpandableFilters() {
        val filtersHeader = binding.filtersHeader
        val filtersContent = binding.filtersContent
        val expandIcon = binding.expandIcon
        
        filtersHeader.setOnClickListener {
            val isVisible = filtersContent.visibility == View.VISIBLE
            
            if (isVisible) {
                // Colapsar
                filtersContent.visibility = View.GONE
                rotateIcon(expandIcon, 180f, 0f)
            } else {
                // Expandir
                filtersContent.visibility = View.VISIBLE
                rotateIcon(expandIcon, 0f, 180f)
            }
        }
    }
    
    private fun rotateIcon(imageView: ImageView, fromDegrees: Float, toDegrees: Float) {
        val rotateAnimation = RotateAnimation(
            fromDegrees, toDegrees,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 200
            fillAfter = true
        }
        imageView.startAnimation(rotateAnimation)
    }
    
    private fun showError(message: String) {
        // Mostrar error usando Snackbar o similar
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show()
    }
}