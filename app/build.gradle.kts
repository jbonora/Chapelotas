// Archivo: build.gradle.kts
// Ubicación: [Carpeta Raíz de tu Proyecto]/app/
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.chapelotas.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chapelotas.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        project.rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use {
            properties.load(it)
        }
        buildConfigField("String", "OPENAI_API_KEY", "\"${properties.getProperty("OPENAI_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // ---- Hilt (Inyección de dependencias) ----
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // ---- Compose (Interfaz de Usuario) ----
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ---- Room (Base de datos local) ----
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ---- Lógica de Negocio y Utilidades ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.gson)
    implementation(libs.accompanist.permissions)
    implementation(libs.generativeai)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.datastore.preferences) // <-- Ahora funcionará

    // ---- Testeo ----
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}