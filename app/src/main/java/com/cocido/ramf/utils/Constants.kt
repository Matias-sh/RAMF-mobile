package com.cocido.ramf.utils

object Constants {
    // API Configuration
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
    
    // Cache Configuration
    const val CACHE_SIZE = 10 * 1024 * 1024L // 10MB
    const val CACHE_MAX_AGE = 60 * 5 // 5 minutes
    const val CACHE_MAX_STALE = 60 * 60 * 24 * 7 // 7 days
    
    // Chart Configuration
    const val CHART_ANIMATION_DURATION = 800
    const val MAX_CHART_ENTRIES = 1000
    const val CHART_ZOOM_LIMIT = 10f
    
    // Date Formats
    const val API_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    const val DISPLAY_DATE_FORMAT = "dd/MM/yyyy HH:mm"
    const val DISPLAY_DATE_SHORT_FORMAT = "dd/MM"
    const val DISPLAY_TIME_FORMAT = "HH:mm"
    
    // Preferences Keys
    const val PREFS_AUTH = "auth_prefs_encrypted"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_DATA = "user_data"
    const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
    const val KEY_SELECTED_STATION = "selected_station"
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_LANGUAGE = "language"
    
    // Station Configuration
    const val DEFAULT_STATION_ID = "00210E7D"
    const val STATION_UPDATE_INTERVAL = 30 * 1000L // 30 seconds
    
    // Error Messages
    const val ERROR_NETWORK = "Error de red. Verifica tu conexión"
    const val ERROR_TIMEOUT = "Tiempo de espera agotado"
    const val ERROR_SERVER = "Error del servidor"
    const val ERROR_UNKNOWN = "Error desconocido"
    const val ERROR_NO_DATA = "No hay datos disponibles"
    
    // Token Configuration
    const val TOKEN_EXPIRY_HOURS = 24L
    
    // Export Configuration
    const val EXPORT_QUALITY = 100
    const val EXPORT_WIDTH = 1920
    const val EXPORT_HEIGHT = 1080
}