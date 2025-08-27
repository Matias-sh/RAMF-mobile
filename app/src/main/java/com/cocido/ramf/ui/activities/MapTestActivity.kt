package com.cocido.ramf.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cocido.ramf.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapTestActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    
    companion object {
        private const val TAG = "MapTestActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private val BUENOS_AIRES = LatLng(-34.6118, -58.3960)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Iniciando MapTestActivity")
        
        // Layout simplificado solo para test
        setContentView(R.layout.activity_map_test)
        
        // Obtener el fragmento de mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        if (mapFragment != null) {
            Log.d(TAG, "onCreate: Fragmento encontrado, llamando getMapAsync")
            mapFragment.getMapAsync(this)
        } else {
            Log.e(TAG, "onCreate: ERROR - No se pudo encontrar el fragmento de mapa")
            Toast.makeText(this, "Error: No se encontró el fragmento de mapa", Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady: Mapa listo!")
        mMap = googleMap
        
        // Verificar API Key
        try {
            val apiKey = getString(R.string.google_maps_api_key)
            Log.d(TAG, "onMapReady: API Key: ${apiKey.take(20)}...")
        } catch (e: Exception) {
            Log.e(TAG, "onMapReady: Error con API Key: ${e.message}")
        }
        
        // Configuración básica
        mMap.uiSettings.isZoomControlsEnabled = true
        
        // Mover cámara a Buenos Aires
        Log.d(TAG, "onMapReady: Moviendo cámara a Buenos Aires")
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BUENOS_AIRES, 10f))
        
        // Solicitar permisos de ubicación
        checkLocationPermissions()
        
        Toast.makeText(this, "Mapa inicializado correctamente", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkLocationPermissions: Permisos concedidos")
            try {
                mMap.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                Log.e(TAG, "checkLocationPermissions: Error: ${e.message}")
            }
        } else {
            Log.d(TAG, "checkLocationPermissions: Solicitando permisos")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
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
                    checkLocationPermissions()
                } else {
                    Toast.makeText(this, "Permisos de ubicación denegados", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}