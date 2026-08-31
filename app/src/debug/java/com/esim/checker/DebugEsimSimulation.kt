package com.esim.checker

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.util.Locale

/**
 * Debug-build-only QA hook. Launch the activity with [DEBUG_ESIM_RESULT_EXTRA] set to
 * READY, NEEDS_VERIFICATION, or NOT_DETECTED to exercise each UI state on an emulator.
 */
internal fun Intent.applyDebugEsimSimulation(
    realResult: EsimCompatibilityResult,
): EsimCompatibilityResult {
    check(BuildConfig.DEBUG)

    return when (getStringExtra(DEBUG_ESIM_RESULT_EXTRA)?.uppercase(Locale.ROOT)) {
        "READY" -> realResult.copy(
            hasEuiccFeature = true,
            euiccManagerAvailable = true,
            euiccEnabled = true,
            androidEsimApiAvailable = true,
            resultStatus = EsimResultStatus.READY,
        )

        "NEEDS_VERIFICATION" -> realResult.copy(
            hasEuiccFeature = true,
            euiccManagerAvailable = true,
            euiccEnabled = false,
            androidEsimApiAvailable = true,
            resultStatus = EsimResultStatus.PARTIALLY_READY,
        )

        "NOT_DETECTED" -> realResult.copy(
            hasEuiccFeature = false,
            euiccManagerAvailable = false,
            euiccEnabled = false,
            androidEsimApiAvailable = true,
            resultStatus = EsimResultStatus.NOT_READY,
        )

        else -> realResult
    }
}

private const val DEBUG_ESIM_RESULT_EXTRA = "com.esim.checker.debug.ESIM_RESULT"

/** Debug-only switch for exercising the wireless-settings fallback on an emulator. */
internal fun Context.forceDebugWirelessSettings(): Boolean =
    (this as? Activity)?.intent?.getBooleanExtra(
        DEBUG_FORCE_WIRELESS_SETTINGS_EXTRA,
        false,
    ) == true

private const val DEBUG_FORCE_WIRELESS_SETTINGS_EXTRA =
    "com.esim.checker.debug.FORCE_WIRELESS_SETTINGS"
