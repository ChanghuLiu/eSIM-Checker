package com.esim.checker

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.euicc.EuiccManager
import androidx.annotation.StringRes

enum class EsimResultStatus {
    READY,
    PARTIALLY_READY,
    NOT_READY,
}

data class EsimCompatibilityResult(
    val deviceManufacturer: String,
    val deviceModel: String,
    val deviceCodename: String,
    val androidVersion: String,
    val hasEuiccFeature: Boolean,
    val euiccManagerAvailable: Boolean,
    val euiccEnabled: Boolean,
    val androidEsimApiAvailable: Boolean,
    val resultStatus: EsimResultStatus,
)

/** Pure result classification kept separate so it can be tested on the JVM. */
object EsimResultClassifier {
    fun classify(
        hasEuiccFeature: Boolean,
        euiccManagerAvailable: Boolean,
        euiccEnabled: Boolean,
    ): EsimResultStatus = when {
        !hasEuiccFeature -> EsimResultStatus.NOT_READY
        euiccManagerAvailable && euiccEnabled -> EsimResultStatus.READY
        else -> EsimResultStatus.PARTIALLY_READY
    }
}

fun EsimCompatibilityResult.toFullReportText(
    context: Context,
    manualVerificationResult: ManualVerificationResult,
): String = context.getString(
    R.string.copy_full_report_template,
    deviceManufacturer,
    deviceModel,
    androidVersion,
    context.getString(if (hasEuiccFeature) R.string.detected else R.string.not_detected),
    context.getString(if (euiccEnabled) R.string.available else R.string.unavailable),
    context.getString(if (androidEsimApiAvailable) R.string.supported else R.string.unsupported),
    context.getString(resultStatus.labelRes()),
    context.getString(manualVerificationResult.labelRes()),
)

@StringRes
fun EsimResultStatus.labelRes(): Int = when (this) {
    EsimResultStatus.READY -> R.string.result_ready
    EsimResultStatus.PARTIALLY_READY -> R.string.result_partially_ready
    EsimResultStatus.NOT_READY -> R.string.esim_not_detected
}

class EsimCompatibilityChecker(private val context: Context) {
    fun check(): EsimCompatibilityResult {
        val hasEuiccFeature = context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_TELEPHONY_EUICC,
        )
        val euiccManager = context.getSystemService(EuiccManager::class.java)
        val euiccEnabled = readEnabledState(euiccManager)
        val androidEsimApiAvailable = isAndroidEsimApiAvailable()

        return EsimCompatibilityResult(
            deviceManufacturer = Build.MANUFACTURER.ifBlank {
                context.getString(R.string.unknown_value)
            },
            deviceModel = Build.MODEL.ifBlank { context.getString(R.string.unknown_value) },
            deviceCodename = Build.DEVICE.ifBlank {
                context.getString(R.string.unknown_value)
            },
            androidVersion = Build.VERSION.RELEASE.ifBlank {
                context.getString(R.string.unknown_value)
            },
            hasEuiccFeature = hasEuiccFeature,
            euiccManagerAvailable = euiccManager != null,
            euiccEnabled = euiccEnabled,
            androidEsimApiAvailable = androidEsimApiAvailable,
            resultStatus = EsimResultClassifier.classify(
                hasEuiccFeature = hasEuiccFeature,
                euiccManagerAvailable = euiccManager != null,
                euiccEnabled = euiccEnabled,
            ),
        )
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun isAndroidEsimApiAvailable(): Boolean {
        // Kept as an explicit SDK check so the API status is independent of hardware signals.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    private fun readEnabledState(euiccManager: EuiccManager?): Boolean {
        if (euiccManager == null) return false

        return try {
            euiccManager.isEnabled
        } catch (_: RuntimeException) {
            // Some vendor implementations can fail while the service is starting.
            false
        }
    }
}
