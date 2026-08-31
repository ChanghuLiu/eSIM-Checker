package com.esim.checker

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit

enum class PurchaseCheckAnswer {
    NOT_CHECKED,
    YES,
    NO,
    NOT_SURE;

    companion object {
        fun fromStoredValue(value: String?): PurchaseCheckAnswer =
            entries.firstOrNull { it.name == value } ?: NOT_CHECKED
    }
}

enum class TravelPurchaseOutcome {
    READY_TO_BUY,
    VERIFY_BEFORE_BUYING,
    DO_NOT_BUY_YET,
}

enum class ManualCheckItem {
    SETTINGS_ESIM_OPTION,
    CARRIER_UNLOCK,
    PROVIDER_EXACT_MODEL,
}

/** Pure purchase-check classification kept separate for JVM tests. */
object TravelPurchaseClassifier {
    fun classify(
        androidDetection: EsimResultStatus,
        settingsVerification: ManualVerificationResult,
        carrierUnlocked: PurchaseCheckAnswer,
        providerCompatibility: PurchaseCheckAnswer,
    ): TravelPurchaseOutcome = when {
        androidDetection == EsimResultStatus.NOT_READY ||
            carrierUnlocked == PurchaseCheckAnswer.NO ||
            providerCompatibility == PurchaseCheckAnswer.NO -> {
            TravelPurchaseOutcome.DO_NOT_BUY_YET
        }

        androidDetection == EsimResultStatus.READY &&
            settingsVerification == ManualVerificationResult.YES &&
            carrierUnlocked == PurchaseCheckAnswer.YES &&
            providerCompatibility == PurchaseCheckAnswer.YES -> {
            TravelPurchaseOutcome.READY_TO_BUY
        }

        else -> TravelPurchaseOutcome.VERIFY_BEFORE_BUYING
    }
}

/** Pure progress logic for confirmation and missing-item UI. */
object PurchaseCheckProgress {
    fun hasEnteredAnswers(
        settingsVerification: ManualVerificationResult,
        carrierUnlocked: PurchaseCheckAnswer,
        providerCompatibility: PurchaseCheckAnswer,
    ): Boolean = settingsVerification != ManualVerificationResult.NOT_CHECKED ||
        carrierUnlocked != PurchaseCheckAnswer.NOT_CHECKED ||
        providerCompatibility != PurchaseCheckAnswer.NOT_CHECKED

    fun missingManualChecks(
        settingsVerification: ManualVerificationResult,
        carrierUnlocked: PurchaseCheckAnswer,
        providerCompatibility: PurchaseCheckAnswer,
    ): List<ManualCheckItem> = buildList {
        if (settingsVerification == ManualVerificationResult.NOT_CHECKED ||
            settingsVerification == ManualVerificationResult.NOT_SURE
        ) {
            add(ManualCheckItem.SETTINGS_ESIM_OPTION)
        }
        if (carrierUnlocked == PurchaseCheckAnswer.NOT_CHECKED ||
            carrierUnlocked == PurchaseCheckAnswer.NOT_SURE
        ) {
            add(ManualCheckItem.CARRIER_UNLOCK)
        }
        if (providerCompatibility == PurchaseCheckAnswer.NOT_CHECKED ||
            providerCompatibility == PurchaseCheckAnswer.NOT_SURE
        ) {
            add(ManualCheckItem.PROVIDER_EXACT_MODEL)
        }
    }

    fun allChecksPositive(
        androidDetection: EsimResultStatus,
        settingsVerification: ManualVerificationResult,
        carrierUnlocked: PurchaseCheckAnswer,
        providerCompatibility: PurchaseCheckAnswer,
    ): Boolean = androidDetection == EsimResultStatus.READY &&
        settingsVerification == ManualVerificationResult.YES &&
        carrierUnlocked == PurchaseCheckAnswer.YES &&
        providerCompatibility == PurchaseCheckAnswer.YES
}

class TravelPurchaseCheckStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun readCarrierUnlocked(): PurchaseCheckAnswer = PurchaseCheckAnswer.fromStoredValue(
        preferences.getString(KEY_CARRIER_UNLOCKED, null),
    )

    fun saveCarrierUnlocked(answer: PurchaseCheckAnswer) {
        preferences.edit {
            putString(KEY_CARRIER_UNLOCKED, answer.name)
        }
    }

    fun readProviderCompatibility(): PurchaseCheckAnswer =
        PurchaseCheckAnswer.fromStoredValue(
            preferences.getString(KEY_PROVIDER_COMPATIBILITY, null),
        )

    fun saveProviderCompatibility(answer: PurchaseCheckAnswer) {
        preferences.edit {
            putString(KEY_PROVIDER_COMPATIBILITY, answer.name)
        }
    }

    fun reset() {
        preferences.edit {
            remove(KEY_CARRIER_UNLOCKED)
            remove(KEY_PROVIDER_COMPATIBILITY)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "travel_esim_purchase_check"
        const val KEY_CARRIER_UNLOCKED = "carrier_unlocked"
        const val KEY_PROVIDER_COMPATIBILITY = "provider_compatibility"
    }
}

@StringRes
fun PurchaseCheckAnswer.labelRes(): Int = when (this) {
    PurchaseCheckAnswer.NOT_CHECKED -> R.string.not_checked
    PurchaseCheckAnswer.YES -> R.string.yes
    PurchaseCheckAnswer.NO -> R.string.no
    PurchaseCheckAnswer.NOT_SURE -> R.string.not_sure
}

@StringRes
fun TravelPurchaseOutcome.labelRes(): Int = when (this) {
    TravelPurchaseOutcome.READY_TO_BUY -> R.string.ready_to_buy
    TravelPurchaseOutcome.VERIFY_BEFORE_BUYING -> R.string.verify_before_buying
    TravelPurchaseOutcome.DO_NOT_BUY_YET -> R.string.do_not_buy_yet
}

fun EsimCompatibilityResult.toPurchaseCheckText(
    context: Context,
    settingsVerification: ManualVerificationResult,
    carrierUnlocked: PurchaseCheckAnswer,
    providerCompatibility: PurchaseCheckAnswer,
    outcome: TravelPurchaseOutcome,
): String {
    val displayName = if (deviceModel.startsWith(deviceManufacturer, ignoreCase = true)) {
        context.bidiIsolate(deviceModel)
    } else {
        context.getString(
            R.string.device_display_name,
            context.bidiIsolate(deviceManufacturer),
            context.bidiIsolate(deviceModel),
        )
    }
    return context.getString(
        R.string.copy_purchase_check_template,
        displayName,
        context.bidiIsolate(deviceManufacturer),
        context.bidiIsolate(deviceModel),
        context.bidiIsolate(deviceCodename),
        context.bidiIsolate(androidVersion),
        context.getString(if (hasEuiccFeature) R.string.detected else R.string.not_detected),
        context.getString(if (euiccEnabled) R.string.available else R.string.unavailable),
        context.getString(
            if (androidEsimApiAvailable) R.string.supported else R.string.unsupported,
        ),
        context.getString(settingsVerification.labelRes()),
        context.getString(carrierUnlocked.labelRes()),
        context.getString(providerCompatibility.labelRes()),
        context.getString(outcome.labelRes()),
    )
}
