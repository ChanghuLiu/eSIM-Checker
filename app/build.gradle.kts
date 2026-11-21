import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") version "4.6.0" apply false
}

if (file("google-services.json").isFile) {
    pluginManager.apply("com.google.gms.google-services")
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
        versionCode = 17
        versionName = "0.9.8"

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
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.play.billing)
    implementation(libs.google.mobile.ads)
    implementation(libs.google.ump)
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
