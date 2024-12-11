pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        plugins {
            id("com.google.gms.google-services") version "4.4.0"
        }
        // If Gradle version is below 4.1:
        maven {
            url = uri("https://maven.google.com")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()

        // If Gradle version is below 4.1:
        maven {
            url = uri("https://maven.google.com")
        }
    }
}

rootProject.name = "BetterTogether"
include(":app")
