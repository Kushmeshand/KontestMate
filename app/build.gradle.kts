import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Firebase
    id("kotlin-kapt")
}
android {
    namespace = "com.example.kontestmate"
    compileSdk = 35

    buildFeatures {
        viewBinding = true
        buildConfig = true   // <-- Enable BuildConfig generation
    }

    defaultConfig {
        applicationId = "com.example.kontestmate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Load OPENAI_API_KEY from local.properties
        val openAiKey: String? = run {
            val propsFile = rootProject.file("local.properties")
            if (propsFile.exists()) {
                val props = Properties()
                propsFile.reader().use { reader ->
                    props.load(reader)
                }
                props.getProperty("OPENAI_API_KEY")
            } else null
        }

        buildConfigField("String", "OPENAI_API_KEY", "\"${openAiKey ?: ""}\"")
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

    kotlinOptions {
        jvmTarget = "1.8"
    }
}


dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.volley)
    implementation(libs.filament.android)
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(libs.androidx.material3.android)
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation ("org.json:json:20240303")
    implementation ("org.jsoup:jsoup:1.16.1")

}
