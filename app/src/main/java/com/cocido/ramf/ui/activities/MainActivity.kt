package com.cocido.ramf.ui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.button.MaterialButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.cocido.ramf.R
import com.cocido.ramf.models.Sensors
import com.cocido.ramf.models.User
import com.cocido.ramf.models.WeatherData
import com.cocido.ramf.models.WeatherStation
import com.cocido.ramf.models.WidgetData
import com.cocido.ramf.viewmodels.WeatherStationViewModel
import com.cocido.ramf.ui.activities.FullScreenChartsActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import java.util.Locale
import java.util.TimeZone
import java.util.Date
import java.text.SimpleDateFormat
import com.cocido.ramf.utils.AuthManager

class MainActivity : AppCompatActivity() {

    private lateinit var stationSpinner: MaterialAutoCompleteTextView
    private lateinit var sharedPref: SharedPreferences
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var tempTextView: TextView
    private lateinit var lastComTextView: TextView
    private lateinit var tempMinTextView: TextView
    private lateinit var tempMaxTextView: TextView

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var navHeaderProfileImage: ImageView
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderRoleUser: TextView

    private val viewModel: WeatherStationViewModel by viewModels()
    private var weatherStations: List<WeatherStation> = listOf()
    private var selectedStationPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AuthManager.initialize(this)
        sharedPref = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        if (!AuthManager.isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.nav_view)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()

        stationSpinner = findViewById(R.id.stationSpinner)
        swipeRefreshLayout = findViewById(R.id.swiperefresh)
        tempTextView = findViewById(R.id.tempTextView)
        lastComTextView = findViewById(R.id.lastComTextView)
        tempMinTextView = findViewById(R.id.tempMinTextView)
        tempMaxTextView = findViewById(R.id.tempMaxTextView)

        // Configurar fragment de datos meteorológicos
        supportFragmentManager.beginTransaction()
            .replace(R.id.weatherDataFragment, com.cocido.ramf.ui.fragments.WeatherInfoFragment())
            .commit()

        // Los gráficos ahora se abren desde el menú lateral

        val headerView = navigationView.getHeaderView(0)
        navHeaderProfileImage = headerView.findViewById(R.id.navHeaderProfileImage)
        navHeaderName = headerView.findViewById(R.id.navHeaderUsername)
        navHeaderRoleUser = headerView.findViewById(R.id.navHeaderDetailsUser)

        val user = getUserDataFromSharedPreferences()
        user?.let {
            navHeaderName.text = "${it.firstName} ${it.lastName}"
            navHeaderRoleUser.text = it.role
            Glide.with(this).load(it.avatar).circleCrop().into(navHeaderProfileImage)
        }

        findViewById<ImageButton>(R.id.menuButton).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Ya estamos en la pantalla principal, cerrar drawer
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_charts -> {
                    openChartsActivity()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_soil_moisture -> {
                    // TODO: Implementar Soil Moisture
                    Toast.makeText(this, "Soil Moisture - En desarrollo", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_map_view -> {
                    openMapActivity()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_station_overview -> {
                    // TODO: Implementar Station Overview
                    Toast.makeText(this, "Station Overview - En desarrollo", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_device_management -> {
                    // TODO: Implementar Device Management
                    Toast.makeText(this, "Device Management - En desarrollo", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, UserProfileActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_settings -> {
                    // TODO: Implementar configuración
                    Toast.makeText(this, "Configuración - En desarrollo", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        swipeRefreshLayout.setOnRefreshListener { fetchWeatherData() }

        setupObservers()
        fetchWeatherData()
    }
    
    private fun openChartsActivity() {
        if (weatherStations.isNotEmpty()) {
            val currentStation = weatherStations[selectedStationPosition]
            val intent = Intent(this, FullScreenChartsActivity::class.java).apply {
                putExtra(FullScreenChartsActivity.EXTRA_STATION_ID, currentStation.id)
                putExtra(FullScreenChartsActivity.EXTRA_STATION_NAME, currentStation.name)
                // Abrimos con temperatura por defecto
                putExtra("selected_parameter", "temperatura")
            }
            startActivity(intent)
        }
    }

    private fun openMapActivity() {
        // Volver a usar MapActivity ahora que la API Key funciona
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
    }

    private fun setupObservers() {
        viewModel.weatherStations.observe(this) { stations ->
            Log.d("MainActivity", "weatherStations observer called with ${stations.size} stations")
            if (stations.isNotEmpty()) {
                weatherStations = stations
                setupStationSpinner(weatherStations)
                
                // Buscar Formosa (Polo Científico) como estación por defecto
                val defaultStation = stations.find { station ->
                    station.name?.contains("Formosa", ignoreCase = true) == true &&
                    station.name?.contains("Polo", ignoreCase = true) == true
                } ?: stations.find { station ->
                    station.name?.contains("Formosa", ignoreCase = true) == true
                } ?: stations[0] // Fallback a la primera estación si no se encuentra Formosa
                
                val defaultStationIndex = stations.indexOf(defaultStation)
                selectedStationPosition = defaultStationIndex
                val stationName = defaultStation.id
                
                Log.d("MainActivity", "Loading data for default station: ${defaultStation.name} (${stationName})")
                
                // Solo cargar datos si tenemos un ID de estación válido
                if (stationName.isNotBlank()) {
                    viewModel.fetchStationData(stationName)
                    viewModel.fetchTemperatureMaxMin(stationName)
                    viewModel.fetchWeatherDataLastDay(stationName)
                    viewModel.fetchWidgetData(stationName)
                } else {
                    Log.e("MainActivity", "Station ID is blank for ${defaultStation.name}")
                }
                
                stationSpinner.setText(defaultStation.name ?: "Desconocida", false)
            } else {
                Log.e("MainActivity", "No weather stations received")
            }
        }

        viewModel.selectedStationData.observe(this) { station ->
            updateMainUI(station)
            // El WeatherInfoFragment ya observa al ViewModel y se actualiza solo.
        }

        viewModel.temperatureMaxMin.observe(this) { tempMaxMin ->
            tempMinTextView.text = "Temp. mínima: ${tempMaxMin.min?.let { String.format("%.2f", it) } ?: "--"} °C"
            tempMaxTextView.text = "Temp. máxima: ${tempMaxMin.max?.let { String.format("%.2f", it) } ?: "--"} °C"
        }

        // Observamos los datos del widget para temperatura actual
        viewModel.widgetData.observe(this) { widgetData ->
            widgetData?.let { widget ->
                // Temperatura actual desde el widget (formato igual a la web: 2 decimales)
                tempTextView.text = String.format(Locale.getDefault(), "%.2f °C", widget.temperature)
                
                // Determinar condición del cielo con los datos del widget
                val condition = determineSkyConditionFromWidget(widget, isDaytime = true)
                updateBackgroundAndIcon(condition, isDaytime = true)
            }
        }

        // Fallback: Usamos esta lista para refrescar temperatura actual y el ícono de estado del cielo
        viewModel.weatherDataLastDay.observe(this) { weatherDataList ->
            val latest = weatherDataList.firstOrNull()
            latest?.let { wd ->
                // Solo usar como fallback si no tenemos datos del widget
                if (tempTextView.text.isNullOrBlank() || tempTextView.text == "-- °C") {
                    wd.sensors.hcAirTemperature?.avg?.let { t ->
                        tempTextView.text = String.format(Locale.getDefault(), "%.2f °C", t)
                    }
                    // Determinar condición del cielo con sensores (si hay radiación/humedad)
                    val condition = determineSkyConditionFromSensors(wd.sensors, isDaytime = true)
                    updateBackgroundAndIcon(condition, isDaytime = true)
                }
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Error: $errorMessage")
        }
    }

    private fun fetchWeatherData() {
        Log.d("MainActivity", "fetchWeatherData called")
        swipeRefreshLayout.isRefreshing = true
        
        // Solo actualizar datos de la estación actual, no resetear a la primera
        if (weatherStations.isNotEmpty() && selectedStationPosition >= 0) {
            val currentStation = weatherStations[selectedStationPosition]
            val stationName = currentStation.id
            Log.d("MainActivity", "Refreshing data for current station: ${currentStation.name} (${stationName})")
            
            // Actualizar datos de la estación actual
            viewModel.fetchTemperatureMaxMin(stationName)
            viewModel.fetchWeatherDataLastDay(stationName)
            viewModel.fetchWidgetData(stationName)
        } else {
            // Fallback: cargar estaciones solo si no tenemos ninguna
            viewModel.fetchWeatherStations()
        }
        
        swipeRefreshLayout.isRefreshing = false
    }

    private fun setupStationSpinner(stations: List<WeatherStation>) {
        val stationNames = stations.map { it.name ?: "Desconocida" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, stationNames)
        stationSpinner.setAdapter(adapter)

        stationSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedStationPosition = position
            val stationName = stations[position].id // En la nueva API, id contiene el stationName
            Log.d("MainActivity", "Selected station: ${stations[position].name} (${stationName})")
            viewModel.fetchStationData(stationName)
            viewModel.fetchTemperatureMaxMin(stationName)
            viewModel.fetchWeatherDataLastDay(stationName)
            viewModel.fetchWidgetData(stationName)
        }
    }

    private fun updateMainUI(station: WeatherStation?) {
        // La nueva API no trae 'meta' en la estación; usamos lastCommunication y dejamos temp hasta que lleguen datos
        lastComTextView.text = formatLastCommunication(station?.lastCommunication)
        if (tempTextView.text.isNullOrBlank()) tempTextView.text = "-- °C"
    }

    private fun formatLastCommunication(isoDate: String?): String {
        if (isoDate == null) return "--/--/----"
        
        return try {
            // Parsear fecha ISO: "2025-08-18T11:31:25.000Z"
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoDate)
            
            // Formatear para mostrar: "18/08/25 09:31"
            val outputFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault() // Hora local
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // Si no se puede parsear, mostrar la fecha original recortada
            isoDate.take(10) // Solo la parte de fecha
        }
    }

    private fun getUserDataFromSharedPreferences(): User? {
        return AuthManager.getCurrentUser()
    }

    private fun logout() {
        AuthManager.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun determineSkyConditionFromSensors(sensors: Sensors, isDaytime: Boolean): String {
        val rh = sensors.hcRelativeHumidity?.avg ?: 0.0
        val sr = sensors.solarRadiation?.avg ?: 0.0
        return if (isDaytime) {
            when {
                sr > 500 -> "Despejado"
                sr in 100.0..500.0 -> "Parcialmente Nublado"
                sr <= 100 -> "Nublado"
                else -> "Muy Nublado"
            }
        } else {
            when {
                rh < 80.0 && sr > 1000 -> "Noche Despejada"
                rh >= 80.0 -> "Noche Parcialmente Nublada"
                else -> "Noche Muy Nublada"
            }
        }
    }

    private fun determineSkyConditionFromWidget(widget: WidgetData, isDaytime: Boolean): String {
        val rh = widget.relativeHumidity
        val sr = widget.solarRadiation.toDouble()
        return if (isDaytime) {
            when {
                sr > 500 -> "Despejado"
                sr in 100.0..500.0 -> "Parcialmente Nublado"
                sr <= 100 -> "Nublado"
                else -> "Muy Nublado"
            }
        } else {
            when {
                rh < 80.0 && sr > 1000 -> "Noche Despejada"
                rh >= 80.0 -> "Noche Parcialmente Nublada"
                else -> "Noche Muy Nublada"
            }
        }
    }

    private fun updateBackgroundAndIcon(weatherCondition: String, isDaytime: Boolean) {
        val cardView = findViewById<MaterialCardView>(R.id.card_temp)
        val weatherIcon = findViewById<ImageView>(R.id.weatherIcon)
        val iconRes = when (weatherCondition) {
            "Despejado" -> R.drawable.ic_weather_sunny
            "Parcialmente Nublado" -> R.drawable.ic_weather_sun_cloud
            "Nublado" -> R.drawable.ic_weather_cloud
            "Muy Nublado" -> R.drawable.ic_weather_very_cloudy
            "Noche Despejada" -> R.drawable.ic_weather_clear_night
            "Noche Parcialmente Nublada" -> R.drawable.ic_weather_cloudy_night
            else -> R.drawable.ic_weather_sunny
        }
        weatherIcon.setImageResource(iconRes)
        // Si querés cambiar fondo del card según condición, podés hacerlo acá con setCardBackgroundColor(...)
    }
}
