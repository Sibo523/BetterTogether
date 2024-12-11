// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Add this line for Firebase:
    id("com.google.gms.google-services") version "4.4.0" apply false
}