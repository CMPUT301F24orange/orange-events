plugins {
    alias(libs.plugins.android.application)

    //Added from firebase tutorial
//    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.orange"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.orange"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //Added from firebase tutorial
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-firestore")

    // Added to allow for Image picker
    implementation( "androidx.activity:activity:1.7.2" )
    implementation ("androidx.activity:activity-ktx:1.7.2")

    // Glide; allows for image retrieval from firebase storage
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")

}