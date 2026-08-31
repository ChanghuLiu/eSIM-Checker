import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseSigningPropertiesFile = providers
    .gradleProperty("esimCheckerSigningProperties")
    .map(::file)
    .orElse(
        providers.provider {
            file(File(System.getProperty("user.home"), ".config/esim-checker/signing.properties"))
        },
    )
    .get()
val releaseSigningProperties = Properties()
val releaseSigningConfigured = releaseSigningPropertiesFile.isFile

if (releaseSigningConfigured) {
    releaseSigningPropertiesFile.inputStream().use(releaseSigningProperties::load)
}

fun Properties.requiredSigningValue(name: String): String =
    getProperty(name)?.takeIf(String::isNotBlank)
        ?: error("Missing release signing property: $name")

android {
    namespace = "com.esim.checker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.esim.checker"
        minSdk = 27
        targetSdk = 36
        versionCode = 11
        versionName = "0.9.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseSigningProperties.requiredSigningValue("storeFile"))
                storePassword = releaseSigningProperties.requiredSigningValue("storePassword")
                keyAlias = releaseSigningProperties.requiredSigningValue("keyAlias")
                keyPassword = releaseSigningProperties.requiredSigningValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
