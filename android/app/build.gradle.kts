import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
}

fun endpoint(property: String, fallback: String): String {
    val value = providers.gradleProperty(property).orElse(fallback).get()
    require(value.none { it == '"' || it == '\\' || it.isWhitespace() }) { "Invalid URL for $property" }
    val uri = URI.create(value)
    val schemes = when (property) {
        "tolmolDebugRestUrl" -> setOf("http", "https")
        "tolmolDebugSocketUrl" -> setOf("ws", "wss")
        "tolmolReleaseRestUrl" -> setOf("https")
        else -> setOf("wss")
    }
    require(uri.scheme in schemes && uri.host != null && uri.userInfo == null) { "Invalid URL for $property" }
    if (property.endsWith("RestUrl")) require(value.endsWith('/')) { "$property must end in /" }
    return "\"$value\""
}

// Local-only OAuth configuration; never commit local.properties or provider credentials.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "").trim()
require(googleWebClientId.all { it.isLetterOrDigit() || it in "._-" }) { "Invalid GOOGLE_WEB_CLIENT_ID in local.properties" }

android {
    namespace = "com.tolmol.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tolmol.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        debug {
            buildConfigField("String", "REST_URL", endpoint("tolmolDebugRestUrl", "http://10.0.2.2:8080/"))
            buildConfigField("String", "SOCKET_URL", endpoint("tolmolDebugSocketUrl", "ws://10.0.2.2:8080/ws"))
        }
        release {
            buildConfigField("String", "REST_URL", endpoint("tolmolReleaseRestUrl", "https://tolmol.invalid/"))
            buildConfigField("String", "SOCKET_URL", endpoint("tolmolReleaseSocketUrl", "wss://tolmol.invalid/ws"))
        }
    }
}

dependencies {
    implementation(libs.exif)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation("androidx.compose.ui:ui-tooling-preview:1.11.0-rc01")
    debugImplementation("androidx.compose.ui:ui-tooling:1.11.0-rc01")
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.googleid)
    implementation(libs.activity)
    implementation(libs.material3)
    implementation(libs.navigation)
    implementation(libs.nav.viewmodel)
    implementation(libs.lifecycle)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    testImplementation(libs.koin.test)
    implementation(libs.retrofit)
    implementation(libs.converter)
    implementation(libs.okhttp)
    implementation(libs.json)
    implementation(libs.datastore)
    implementation(libs.location)
    implementation(libs.tasks)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
