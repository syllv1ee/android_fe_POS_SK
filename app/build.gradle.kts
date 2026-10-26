plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Schema tiap versi DB → app/schemas/*.json (WAJIB di-commit). Dipakai MigrationTestHelper
// (androidTest) untuk memastikan Migration menghasilkan schema yang sama dengan entity.
room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.mhyddnn.mubarokappmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mhyddnn.mubarokappmobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Server dev (NestJS) di LAN. Device fisik menembak IP host langsung.
            // (Emulator: ganti ke http://10.0.2.2:53110/api/) — port = PORT di .env sales-kanvas-be.
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.4:53110/api/\"")
            // Login asli (BE lokal aktif): Bearer token diperlukan untuk endpoint terlindungi.
            // Set true hanya untuk dev UI cepat tanpa BE (sesi dev tanpa token → API 401).
            buildConfigField("boolean", "SKIP_LOGIN", "false")
        }
        release {
            // R8: buang kode/ikon tak terpakai (material-icons-extended besar). Aturan Gson di
            // proguard-rules.pro — DTO baru taruh di paket `feature.<fitur>.data`.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Ganti dengan domain produksi (HTTPS).
            buildConfigField("String", "BASE_URL", "\"https://api.example.com/api/\"")
            // Produksi WAJIB lewat login.
            buildConfigField("boolean", "SKIP_LOGIN", "false")
        }
    }
    compileOptions {
        // Naik ke 17: disyaratkan Compose Unstyled (lihat CLAUDE.md §3).
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time (dipakai Formatters & AppDatePickerField) butuh API 26+ tanpa ini — minSdk 24.
        isCoreLibraryDesugaringEnabled = true
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    // material-icons-extended: paket ikon saja (group androidx.compose.material, bukan
    // material3) — tetap dipakai, tidak bergantung ke Material3.
    implementation(libs.androidx.material.icons.extended)
    // Compose Unstyled: pengganti Material3 (lihat CLAUDE.md §3).
    implementation(libs.compose.unstyled)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Offline: cache (Room) + antrean sinkron (WorkManager)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
