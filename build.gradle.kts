
// Project-level build.gradle.kts
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.3.0") // Android Gradle Plugin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10") // Kotlin Plugin
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Add this line for Firebase:
    id("com.google.gms.google-services") version "4.4.0" apply false
}