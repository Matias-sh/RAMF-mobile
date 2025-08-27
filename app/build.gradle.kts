plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
}

android {
    namespace = "com.cocido.ramf"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cocido.ramf"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Configuraciones para producción
        buildConfigField("String", "BASE_URL_DEV", "\"http://192.168.0.2:3100/api/\"")
        buildConfigField("String", "BASE_URL_PROD", "\"https://api.ramf.com.ar/api/\"")
        buildConfigField("boolean", "DEBUG_MODE", "true")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "DEBUG_MODE", "true")
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.0.2:3100/api/\"")
        }
        
        release {
            isMinifyEnabled = true
            isDebuggable = false
            isShrinkResources = true
            buildConfigField("boolean", "DEBUG_MODE", "false")
            buildConfigField("String", "API_BASE_URL", "\"https://api.ramf.com.ar/api/\"")
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            signingConfig = signingConfigs.getByName("debug") // Cambiar en producción
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)
    
    // Material Design
    implementation(libs.material)
    
    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    
    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // UI Components
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.preference.ktx)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Async
    implementation(libs.kotlinx.coroutines.android)
    
    // Security
    implementation(libs.androidx.security.crypto)
    
    // Database (comentado hasta que sea necesario)
    // implementation(libs.androidx.room.runtime)
    // implementation(libs.androidx.room.ktx)
    // kapt(libs.androidx.room.compiler)
    
    // Background work
    implementation(libs.androidx.work.runtime.ktx)
    
    // Data storage
    implementation(libs.androidx.datastore.preferences)
    
    // Images
    implementation(libs.glide)
    // kapt(libs.glide.compiler) // Comentado para evitar problemas KAPT
    
    // Charts
    implementation(libs.mpandroidchart)
    
    // Animations
    implementation(libs.lottie)
    
    // Google Services
    implementation(libs.google.signin)
    implementation(libs.google.maps)
    implementation(libs.google.location)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}