package com.esim.checker

import android.content.Context
import androidx.core.text.BidiFormatter

/**
 * Adds directional isolation around technical values without changing their
 * underlying content. This keeps model identifiers, versions and codenames
 * readable inside Arabic and other RTL text.
 */
fun Context.bidiIsolate(value: String): String {
    val locale = resources.configuration.locales[0]
    return BidiFormatter.getInstance(locale).unicodeWrap(value)
}
