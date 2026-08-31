package com.esim.checker

import android.content.Context
import android.content.Intent

/** Release builds always use the result produced by the real Android capability check. */
@Suppress("UNUSED_PARAMETER")
internal fun Intent.applyDebugEsimSimulation(
    realResult: EsimCompatibilityResult,
): EsimCompatibilityResult = realResult

/** Release builds always attempt the dedicated eSIM settings route when Android exposes it. */
internal fun Context.forceDebugWirelessSettings(): Boolean = false
