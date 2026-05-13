// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    // Esta línea debe coincidir con lo que tengas en libs.versions.toml
    id("com.google.gms.google-services") version "4.4.2" apply false
}