package com.esim.checker

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit

enum class ManualVerificationResult {
    NOT_CHECKED,
    YES,
    NO,
    NOT_SURE;

    companion object {
        fun fromStoredValue(value: String?): ManualVerificationResult =
            entries.firstOrNull { it.name == value } ?: NOT_CHECKED
    }
}

class ManualVerificationStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun readResult(): ManualVerificationResult = ManualVerificationResult.fromStoredValue(
        preferences.getString(KEY_RESULT, null),
    )

    fun saveResult(result: ManualVerificationResult) {
        preferences.edit {
            putString(KEY_RESULT, result.name)
        }
    }

    fun shouldShowVerificationCard(): Boolean = preferences.getBoolean(
        KEY_SETTINGS_VISITED,
        false,
    )

    fun markSettingsVisited() {
        preferences.edit {
            putBoolean(KEY_SETTINGS_VISITED, true)
        }
    }

    fun reset() {
        preferences.edit {
            remove(KEY_RESULT)
            remove(KEY_SETTINGS_VISITED)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "manual_esim_verification"
        const val KEY_RESULT = "settings_result"
        const val KEY_SETTINGS_VISITED = "settings_visited"
    }
}

@StringRes
fun ManualVerificationResult.labelRes(): Int = when (this) {
    ManualVerificationResult.NOT_CHECKED -> R.string.not_checked
    ManualVerificationResult.YES -> R.string.yes
    ManualVerificationResult.NO -> R.string.no
    ManualVerificationResult.NOT_SURE -> R.string.not_sure
}
