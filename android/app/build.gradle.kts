plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val buildNumber = (System.getenv("VERSION_CODE") ?: "1").toInt()
val keystorePassword: String? = System.getenv("KEYSTORE_PASSWORD")

android {
    namespace = "io.github.matt2868.lifeplanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.matt2868.lifeplanner"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = buildNumber.toString()
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release.p12")
            storeType = "pkcs12"
            storePassword = keystorePassword
            keyAlias = "planner"
            keyPassword = keystorePassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePassword != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.11.0")
}
