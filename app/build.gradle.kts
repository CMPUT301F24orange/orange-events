plugins {
    alias(libs.plugins.android.application)
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
            buildConfigField ("boolean", "IS_TESTING", "false")
        }
        debug {
            buildConfigField( "boolean", "IS_TESTING", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}
/**
dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.recyclerview)
    implementation(libs.fragment.testing)
    implementation(libs.espresso.intents)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation( "androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation ("androidx.test:rules:1.5.0")

    // Added to allow for Image picker
    implementation( "androidx.activity:activity:1.7.2" )
    implementation ("androidx.activity:activity-ktx:1.7.2")

    //Added from firebase tutorial
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-firestore")
    //
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")

    //Added for QR scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("com.google.zxing:core:3.3.3")
    testImplementation ("org.mockito:mockito-core:4.5.1")

    androidTestImplementation ("org.mockito:mockito-android:4.5.1")
    androidTestImplementation ("androidx.navigation:navigation-testing:2.5.3")
    testImplementation ("org.mockito:mockito-android:4.0.0")

    //Mocking the data
    //testImplementation("org.mockito:mockito-inline:4.5.1")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    androidTestImplementation("androidx.test:core:1.8.0")
    testImplementation ("org.mockito:mockito-inline:3.12.0")  // for inline mocking
    testImplementation ("org.mockito:mockito-android:2.28.2")// for Android compatibility
    testImplementation ("junit:junit:4.13.2")

}
*/

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.recyclerview)
    implementation(libs.fragment.testing)
    implementation(libs.espresso.intents)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // Added to allow for Image picker
    implementation("androidx.activity:activity:1.7.2")
    implementation("androidx.activity:activity-ktx:1.7.2")

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")

    // QR scanning libraries
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.3.3")
    testImplementation("org.mockito:mockito-core:4.6.1")
    androidTestImplementation("org.mockito:mockito-android:4.6.1")


    // Unit and Android Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.5.1")
    testImplementation("org.mockito:mockito-inline:4.5.1")
    androidTestImplementation("org.mockito:mockito-android:4.5.1")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    androidTestImplementation("androidx.navigation:navigation-testing:2.5.3")
    testImplementation("org.mockito:mockito-android:4.0.0")
    androidTestImplementation(libs.junit.jupiter)

    implementation ("androidx.recyclerview:recyclerview:1.2.1")

    // Geolocation dependencies
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    androidTestImplementation("com.google.android.gms:play-services-maps:18.2.0")

    // Added for Notifications
    implementation("com.google.firebase:firebase-messaging:24.1.0")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.guava:guava:32.1.2-jre") // or the latest version
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
}
