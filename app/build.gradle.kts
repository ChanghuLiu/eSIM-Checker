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
val productionRewardedAdsEnabled = providers.gradleProperty("enableProductionRewardedAds").orNull == "true"
val umpTestFlowEnabled = providers.gradleProperty("enableUmpTestFlow").orNull == "true"
val umpTestDeviceHashedId = providers.gradleProperty("umpTestDeviceHashedId").orNull.orEmpty()
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
        versionCode = 18
        versionName = "0.9.9"

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
        debug {
            // UMP's published message is package-bound; exact package is used only on a device without Play app installed.
            applicationIdSuffix = if (umpTestFlowEnabled) "" else ".adtest"
            versionNameSuffix = "-ads-test"
            manifestPlaceholders["admobAppId"] = if (umpTestFlowEnabled) {
                "ca-app-pub-8828197651861282~4506767275"
            } else {
                "ca-app-pub-3940256099942544~3347511713"
            }
            buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "ADMOB_APP_OPEN_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("boolean", "REWARDED_ADS_ENABLED", "true")
            buildConfigField("boolean", "UMP_TEST_FLOW_ENABLED", umpTestFlowEnabled.toString())
            buildConfigField("String", "UMP_TEST_DEVICE_HASHED_ID", "\"$umpTestDeviceHashedId\"")
        }
        release {
            buildConfigField("boolean", "UMP_TEST_FLOW_ENABLED", "false")
            buildConfigField("String", "UMP_TEST_DEVICE_HASHED_ID", "\"\"")
            buildConfigField("String", "ADMOB_REWARDED_AD_UNIT_ID", "\"ca-app-pub-8828197651861282/8426966450\"")
            buildConfigField("String", "ADMOB_APP_OPEN_AD_UNIT_ID", "\"ca-app-pub-8828197651861282/3192106597\"")
            buildConfigField("boolean", "REWARDED_ADS_ENABLED", productionRewardedAdsEnabled.toString())
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                // Enable the AGP 9.3 R8 pipeline, including code and resource shrinking.
                enable = true
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
    // Compose UI Graphics 1.10.4 resolves graphics-path 1.0.1, whose shipped
    // native library predates current 16 KB page-size guidance. Pin the
    // compatible AndroidX release directly and verify every ABI in the AAB.
    implementation("androidx.graphics:graphics-path:1.1.0")
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.play.billing)
    implementation(libs.google.mobile.ads)
    implementation(libs.google.ump)
    // Google Mobile Ads 25.5.0 pulls WorkManager 2.7.0 / Room 2.2.5. Those
    // 2021-era runtime components fail in the minified release startup path;
    // align to current stable WorkManager instead of keeping the app unshrunk.
    implementation("androidx.work:work-runtime:2.12.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
