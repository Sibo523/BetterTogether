plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Add this line for Firebase:
    id("com.google.gms.google-services")
    id("kotlin-kapt")

}

android {
    namespace = "com.example.bettertogether"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.bettertogether"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {
    // Core dependencies
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.lifecycle.runtime.ktx.v262)
    implementation(libs.androidx.appcompat.v161)

    // Jetpack Compose
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.constraintlayout.v214)
    implementation(libs.androidx.cardview)
    implementation(libs.material)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Firebase
    implementation(platform(libs.firebase.bom.v3210))
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.google.firebase.analytics)
    implementation("com.google.firebase:firebase-auth-ktx:latest_version")
    implementation("com.google.android.gms:play-services-auth:latest_version")

    // Glide for image loading
    implementation(libs.glide.v4160)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.core)
    annotationProcessor(libs.compiler.v4160)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation("com.airbnb.android:lottie:5.2.0")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test:runner:1.5.2")
    androidTestImplementation ("androidx.test:rules:1.5.0")
    androidTestImplementation ("androidx.test.espresso:espresso-intents:3.5.1")
    //androidTestImplementation("org.robolectric:robolectric:4.9")
    // Imgur for uploading an image
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.1")

    // SearchView
    implementation ("androidx.appcompat:appcompat:1.6.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.tbuonomo:dotsindicator:4.3")
//    implementation("com.tbuonomo.andrui:viewpagerdotsindicator:4.3")
//
//    implementation("androidx.viewpager2:viewpager2:1.1.0")
}

repositories {
    google()
    mavenCentral()
}
