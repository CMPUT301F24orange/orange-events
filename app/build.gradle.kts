import org.gradle.external.javadoc.JavadocMemberLevel

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
            buildConfigField("boolean", "IS_TESTING", "false")
        }
        debug {
            buildConfigField("boolean", "IS_TESTING", "true")
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
            excludes.addAll(listOf(
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md",
                "/META-INF/LICENSE",
                "/META-INF/NOTICE",
                "/META-INF/DEPENDENCIES"
            ))
        }
    }
}



// Create Javadoc tasks for each variant
android.applicationVariants.all {
    val variant = this
    tasks.register("generate${variant.name.capitalize()}Javadoc", Javadoc::class) {
        description = "Generates Javadoc for ${variant.name}."
        source = variant.javaCompileProvider.get().source

        destinationDir = file("$rootDir/javadoc/${variant.name}")
        isFailOnError = false

        val androidJar = "${android.sdkDirectory}/platforms/${android.compileSdkVersion}/android.jar"
        classpath = files(variant.javaCompileProvider.get().classpath, androidJar)

        (options as StandardJavadocDocletOptions).apply {
            memberLevel = JavadocMemberLevel.PROTECTED
        }
    }
}
// Create Javadoc tasks for each variant

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

    // Added from firebase tutorial
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-firestore")

    // Added for QR scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.3.3")
    testImplementation("org.mockito:mockito-core:4.6.1")
    androidTestImplementation("org.mockito:mockito-android:4.6.1")
    androidTestImplementation("androidx.navigation:navigation-testing:2.5.3")
    testImplementation("org.mockito:mockito-android:4.0.0")
    androidTestImplementation(libs.junit.jupiter)

    implementation ("androidx.recyclerview:recyclerview:1.2.1")

    // Geolocation dependencies
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    androidTestImplementation("com.google.android.gms:play-services-maps:18.2.0")
}