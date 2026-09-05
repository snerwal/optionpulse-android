plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.optionpulse.scanner"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.optionpulse.scanner"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val apiBaseUrl = providers.gradleProperty("OPTIONPULSE_API_URL").orNull ?: "https://scanner.example.com/"
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        val enrollmentToken = providers.gradleProperty("OPTIONPULSE_DEVICE_TOKEN").orNull ?: ""
        buildConfigField("String", "DEVICE_ENROLLMENT_TOKEN", "\"${enrollmentToken.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    testImplementation("junit:junit:4.13.2")
}
