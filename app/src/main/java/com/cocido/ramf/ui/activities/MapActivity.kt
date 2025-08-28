package com.cocido.ramf.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.LayoutInflater
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cocido.ramf.R
import com.cocido.ramf.models.WeatherStation
import com.cocido.ramf.models.WidgetData
import com.cocido.ramf.viewmodels.WeatherStationViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var btnBack: ImageButton
    private lateinit var btnNotifications: ImageButton
    private lateinit var filterContainer: LinearLayout
    private lateinit var tvSelectedParameter: TextView
    
    private lateinit var mMap: GoogleMap
    private var mapFragment: SupportMapFragment? = null
    private val viewModel: WeatherStationViewModel by viewModels()
    
    private var weatherStations: List<WeatherStation> = listOf()
    private var selectedParameter = "temperatura"
    private val stationMarkers = mutableMapOf<String, Marker>()
    private val stationWidgetData = mutableMapOf<String, WidgetData>()
    private var currentRequestedStationId: String? = null
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MapActivity"
        
        // Coordenadas centradas en Formosa, Argentina
        private val DEFAULT_LOCATION = LatLng(-25.3, -59.5) // Centro de Formosa
        private const val DEFAULT_ZOOM = 7.5f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Iniciando MapActivity")
        setContentView(R.layout.activity_map)

        initViews()
        setupListeners()
        setupObservers()
        
        // Inicializar el mapa
        Log.d(TAG, "onCreate: Inicializando fragmento de mapa")
        
        // Intentar obtener el fragmento del layout
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        
        if (mapFragment == null) {
            Log.w(TAG, "onCreate: Fragmento no encontrado en layout, creando programáticamente")
            mapFragment = SupportMapFragment.newInstance()
            
            // Buscar el contenedor
            val mapContainer = findViewById<android.widget.FrameLayout>(R.id.mapContainer)
            if (mapContainer != null) {
                Log.d(TAG, "onCreate: Agregando fragmento al contenedor")
                val newFragment = SupportMapFragment.newInstance()
                mapFragment = newFragment
                supportFragmentManager.beginTransaction()
                    .add(R.id.mapContainer, newFragment)
                    .commit()
            } else {
                Log.e(TAG, "onCreate: ERROR - No se encontró el contenedor del mapa")
                Toast.makeText(this, "Error al inicializar el mapa", Toast.LENGTH_LONG).show()
                return
            }
        }
        
        Log.d(TAG, "onCreate: Llamando getMapAsync")
        mapFragment?.getMapAsync(this)
        
        // Cargar datos
        Log.d(TAG, "onCreate: Cargando datos de estaciones")
        loadWeatherStations()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnNotifications = findViewById(R.id.btnNotifications)
        filterContainer = findViewById(R.id.filterContainer)
        tvSelectedParameter = findViewById(R.id.tvSelectedParameter)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnNotifications.setOnClickListener {
            Toast.makeText(this, "Notificaciones - En desarrollo", Toast.LENGTH_SHORT).show()
        }

        filterContainer.setOnClickListener {
            showParameterSelectionDialog()
        }
    }
    
    private fun setupObservers() {
        viewModel.weatherStations.observe(this) { stations ->
            Log.d(TAG, "setupObservers: Recibidas ${stations?.size ?: 0} estaciones")
            if (stations.isNotEmpty()) {
                weatherStations = stations
                Log.d(TAG, "setupObservers: Agregando marcadores y cargando datos")
                addMarkersToMap()
                loadWidgetDataForStations()
            } else {
                Log.w(TAG, "setupObservers: Lista de estaciones vacía")
            }
        }
        
        // Observar los datos del widget
        viewModel.widgetData.observe(this) { widgetData ->
            widgetData?.let { data ->
                Log.d(TAG, "setupObservers: Recibidos datos del widget para estación")
                val stationId = currentRequestedStationId
                if (stationId != null) {
                    Log.d(TAG, "setupObservers: Asociando datos con estación $stationId")
                    stationWidgetData[stationId] = data
                    updateMarkerForStation(stationId)
                } else {
                    Log.w(TAG, "setupObservers: No se pudo asociar datos - currentRequestedStationId es null")
                }
            }
        }
        
        viewModel.error.observe(this) { errorMessage ->
            Log.e(TAG, "setupObservers: Error recibido - $errorMessage")
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun showParameterSelectionDialog() {
        val parameters = arrayOf(
            "Temperatura del aire",
            "Humedad relativa", 
            "Velocidad del viento",
            "Precipitación última hora",
            "Radiación solar",
            "Presión atmosférica",
            "Punto de rocío"
        )
        
        val parameterKeys = arrayOf(
            "temperatura",
            "humedad",
            "viento",
            "precipitacion",
            "radiacion",
            "presion",
            "punto_rocio"
        )

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Seleccionar Parámetro")
        builder.setItems(parameters) { dialog, which ->
            tvSelectedParameter.text = parameters[which]
            selectedParameter = parameterKeys[which]
            updateMarkersWithParameter(selectedParameter)
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady: Mapa listo para usar")
        mMap = googleMap
        
        // Verificar que la API Key esté configurada
        try {
            val apiKey = getString(R.string.google_maps_api_key)
            Log.d(TAG, "onMapReady: API Key configurada: ${apiKey.take(20)}...")
        } catch (e: Exception) {
            Log.e(TAG, "onMapReady: Error obteniendo API Key: ${e.message}")
        }
        
        // Configurar el mapa
        Log.d(TAG, "onMapReady: Configurando controles UI del mapa")
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        
        // Solicitar permisos de ubicación
        if (checkLocationPermission()) {
            Log.d(TAG, "onMapReady: Permisos de ubicación concedidos")
            enableMyLocation()
        } else {
            Log.d(TAG, "onMapReady: Solicitando permisos de ubicación")
            requestLocationPermission()
        }
        
        // Configurar la cámara inicial
        Log.d(TAG, "onMapReady: Configurando cámara inicial en Buenos Aires")
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM))
        
        // Configurar listener para marcadores
        mMap.setOnMarkerClickListener { marker ->
            showStationInfo(marker)
            true
        }
        
        // Si ya tenemos estaciones, agregar marcadores
        if (weatherStations.isNotEmpty()) {
            Log.d(TAG, "onMapReady: Agregando ${weatherStations.size} marcadores al mapa")
            addMarkersToMap()
        } else {
            Log.d(TAG, "onMapReady: No hay estaciones cargadas aún")
        }
    }
    
    private fun loadWeatherStations() {
        viewModel.fetchWeatherStations()
    }
    
    private fun loadWidgetDataForStations() {
        Log.d(TAG, "loadWidgetDataForStations: Iniciando carga de datos para ${weatherStations.size} estaciones")
        
        // Observar los datos del widget una sola vez
        if (!::mMap.isInitialized) return
        
        // Cargar datos de cada estación
        weatherStations.forEachIndexed { index, station ->
            Log.d(TAG, "loadWidgetDataForStations: Cargando datos para estación ${station.name} (${station.id})")
            
            // Usar un delay pequeño para evitar saturar el API
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                currentRequestedStationId = station.id
                viewModel.fetchWidgetData(station.id)
            }, (index * 200L)) // 200ms entre cada llamada
        }
    }
    
    private fun findStationIdForWidgetData(widgetData: WidgetData): String? {
        // Por ahora, necesitamos almacenar el ID de la estación que se está consultando
        // en el ViewModel para poder asociar los datos correctamente
        // Esto será mejorado cuando el backend incluya el station_id en la respuesta
        return currentRequestedStationId
    }
    
    private fun addMarkersToMap() {
        if (!::mMap.isInitialized) {
            Log.w(TAG, "addMarkersToMap: Mapa no inicializado")
            return
        }
        
        Log.d(TAG, "addMarkersToMap: Agregando ${weatherStations.size} marcadores al mapa")
        
        // Limpiar marcadores existentes
        stationMarkers.values.forEach { it.remove() }
        stationMarkers.clear()
        
        var markersAdded = 0
        
        weatherStations.forEach { station ->
            val coordinates = station.position?.coordinates
            if (coordinates != null && coordinates.size >= 2) {
                // Verificar que las coordenadas estén dentro de Formosa aproximadamente
                val lat = coordinates[1]
                val lng = coordinates[0]
                
                Log.d(TAG, "addMarkersToMap: Agregando marcador para ${station.name} en ($lat, $lng)")
                
                val latLng = LatLng(lat, lng)
                
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title(station.name ?: "Estación ${station.id}")
                    .snippet(getMarkerSnippet(station.id))
                
                // Usar marcador personalizado con valor visible (como FieldClimate)
                setCustomMarkerIcon(markerOptions, station.id)
                
                val marker = mMap.addMarker(markerOptions)
                marker?.let {
                    stationMarkers[station.id] = it
                    markersAdded++
                    Log.d(TAG, "addMarkersToMap: Marcador agregado para estación ${station.id}")
                }
            } else {
                Log.w(TAG, "addMarkersToMap: Coordenadas inválidas para estación ${station.id}")
            }
        }
        
        Log.d(TAG, "addMarkersToMap: Total de marcadores agregados: $markersAdded")
    }
    
    private fun getMarkerSnippet(stationId: String): String {
        val widgetData = stationWidgetData[stationId]
        if (widgetData != null) {
            return when (selectedParameter) {
                "temperatura" -> "${String.format("%.1f", widgetData.temperature)}°C"
                "humedad" -> "${String.format("%.1f", widgetData.relativeHumidity)}%"
                "viento" -> "${String.format("%.1f", widgetData.windSpeed)} m/s"
                "precipitacion" -> "${String.format("%.1f", widgetData.rainLastHour)} mm"
                "radiacion" -> "${widgetData.solarRadiation} W/m²"
                "presion" -> "${String.format("%.1f", widgetData.airPressure)} hPa"
                "punto_rocio" -> "${String.format("%.1f", widgetData.dewPoint)}°C"
                else -> "Datos no disponibles"
            }
        }
        return "Cargando datos..."
    }
    
    private fun setCustomMarkerIcon(markerOptions: MarkerOptions, stationId: String) {
        val widgetData = stationWidgetData[stationId]
        if (widgetData != null) {
            // Obtener solo el valor según el parámetro seleccionado (como FieldClimate)
            val value = when (selectedParameter) {
                "temperatura" -> String.format("%.1f", widgetData.temperature)
                "humedad" -> String.format("%.1f", widgetData.relativeHumidity)
                "viento" -> String.format("%.1f", widgetData.windSpeed)
                "precipitacion" -> String.format("%.1f", widgetData.rainLastHour)
                "radiacion" -> widgetData.solarRadiation.toString()
                "presion" -> String.format("%.0f", widgetData.airPressure)
                "punto_rocio" -> String.format("%.1f", widgetData.dewPoint)
                else -> "--"
            }
            
            // Crear marcador personalizado con solo el valor visible
            val customIcon = createCustomMarker(value)
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(customIcon))
        } else {
            // Marcador por defecto mientras carga
            val customIcon = createCustomMarker("--")
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(customIcon))
        }
    }
    
    private fun createCustomMarker(value: String): Bitmap {
        // Inflar el layout personalizado
        val inflater = LayoutInflater.from(this)
        val markerView = inflater.inflate(R.layout.custom_marker_layout, null)
        
        // Establecer solo el valor
        val valueText = markerView.findViewById<TextView>(R.id.markerValueText)
        valueText.text = value
        
        // Medir y hacer layout
        markerView.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)
        
        // Convertir a bitmap
        val bitmap = Bitmap.createBitmap(
            markerView.measuredWidth, 
            markerView.measuredHeight, 
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)
        
        return bitmap
    }

    private fun setMarkerIcon(markerOptions: MarkerOptions, stationId: String) {
        val widgetData = stationWidgetData[stationId]
        if (widgetData != null) {
            when (selectedParameter) {
                "temperatura" -> {
                    val temp = widgetData.temperature
                    val hue = when {
                        temp < 0 -> BitmapDescriptorFactory.HUE_CYAN
                        temp < 15 -> BitmapDescriptorFactory.HUE_BLUE
                        temp < 25 -> BitmapDescriptorFactory.HUE_GREEN
                        temp < 35 -> BitmapDescriptorFactory.HUE_YELLOW
                        else -> BitmapDescriptorFactory.HUE_RED
                    }
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
                }
                "humedad" -> {
                    val humidity = widgetData.relativeHumidity
                    val hue = when {
                        humidity < 30 -> BitmapDescriptorFactory.HUE_RED
                        humidity < 60 -> BitmapDescriptorFactory.HUE_YELLOW
                        humidity < 80 -> BitmapDescriptorFactory.HUE_GREEN
                        else -> BitmapDescriptorFactory.HUE_BLUE
                    }
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
                }
                "viento" -> {
                    val windSpeed = widgetData.windSpeed
                    val hue = when {
                        windSpeed < 2 -> BitmapDescriptorFactory.HUE_GREEN
                        windSpeed < 5 -> BitmapDescriptorFactory.HUE_YELLOW
                        windSpeed < 8 -> BitmapDescriptorFactory.HUE_ORANGE
                        else -> BitmapDescriptorFactory.HUE_RED
                    }
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
                }
                "precipitacion" -> {
                    val rain = widgetData.rainLastHour
                    val hue = when {
                        rain == 0.0 -> BitmapDescriptorFactory.HUE_AZURE
                        rain < 1 -> BitmapDescriptorFactory.HUE_GREEN
                        rain < 5 -> BitmapDescriptorFactory.HUE_YELLOW
                        else -> BitmapDescriptorFactory.HUE_BLUE
                    }
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
                }
                "radiacion" -> {
                    val radiation = widgetData.solarRadiation
                    val hue = when {
                        radiation < 200 -> BitmapDescriptorFactory.HUE_BLUE
                        radiation < 500 -> BitmapDescriptorFactory.HUE_GREEN
                        radiation < 800 -> BitmapDescriptorFactory.HUE_YELLOW
                        else -> BitmapDescriptorFactory.HUE_RED
                    }
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
                }
                "presion" -> {
                    val pressure = widgetData.airPressure
                    val hue = when {
                        pressure < 1010 -> BitmapDescriptorFactory.HUE_BLUE
                        pressure < 1020 -> BitmapDescriptorFactory.HUE_GREEN
                        else -> BitmapDescriptorFactory.HUE_RED
                    }
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
                }
                "punto_rocio" -> {
                    val dewPoint = widgetData.dewPoint
                    val hue = when {
                        dewPoint < 0 -> BitmapDescriptorFactory.HUE_CYAN
                        dewPoint < 10 -> BitmapDescriptorFactory.HUE_BLUE
                        dewPoint < 20 -> BitmapDescriptorFactory.HUE_GREEN
                        else -> BitmapDescriptorFactory.HUE_YELLOW
                    }
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
                }
                else -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            }
        } else {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        }
    }
    
    private fun updateMarkersWithParameter(parameter: String) {
        Log.d(TAG, "updateMarkersWithParameter: Actualizando marcadores para parámetro: $parameter")
        
        if (!::mMap.isInitialized) return
        
        val tempMarkers = mutableMapOf<String, Marker>()
        
        stationMarkers.forEach { (stationId, marker) ->
            // Recrear el marcador con el nuevo ícono y snippet
            val position = marker.position
            val title = marker.title
            marker.remove()
            
            val markerOptions = MarkerOptions()
                .position(position)
                .title(title)
                .snippet(getMarkerSnippet(stationId))
            
            setCustomMarkerIcon(markerOptions, stationId)
            
            val newMarker = mMap.addMarker(markerOptions)
            newMarker?.let {
                tempMarkers[stationId] = it
                Log.d(TAG, "updateMarkersWithParameter: Marcador actualizado para estación $stationId")
            }
        }
        
        // Reemplazar los marcadores
        stationMarkers.clear()
        stationMarkers.putAll(tempMarkers)
        
        Log.d(TAG, "updateMarkersWithParameter: Actualizados ${tempMarkers.size} marcadores")
    }
    
    private fun updateMarkerForStation(stationId: String) {
        val marker = stationMarkers[stationId]
        if (marker != null) {
            marker.snippet = getMarkerSnippet(stationId)
        }
    }
    
    private fun showStationInfo(marker: Marker) {
        // Solo mostrar el nombre de la estación en una etiqueta simple
        val stationName = marker.title ?: "Estación Desconocida"
        
        // Mostrar toast simple con el nombre de la estación
        Toast.makeText(this, stationName, Toast.LENGTH_SHORT).show()
    }
    
    
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    
    private fun enableMyLocation() {
        if (checkLocationPermission()) {
            try {
                mMap.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                Log.e(TAG, "Error enabling location: ${e.message}")
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                } else {
                    Toast.makeText(
                        this,
                        "Permiso de ubicación denegado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}