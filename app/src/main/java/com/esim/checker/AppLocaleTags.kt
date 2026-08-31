package com.esim.checker

import java.util.Locale

/**
 * Canonical app language tags. Regional system locales are intentionally
 * consolidated to the manually maintained in-app language set.
 */
object AppLocaleTags {
    const val ENGLISH = "en"
    const val ARABIC = "ar"
    const val SIMPLIFIED_CHINESE = "zh-CN"
    const val TRADITIONAL_CHINESE = "zh-TW"
    const val FRENCH = "fr"
    const val GERMAN = "de"
    const val HINDI = "hi"
    const val INDONESIAN = "id"
    const val ITALIAN = "it"
    const val JAPANESE = "ja"
    const val KOREAN = "ko"
    const val PORTUGUESE_BRAZIL = "pt-BR"
    const val RUSSIAN = "ru"
    const val SPANISH = "es-ES"
    const val TURKISH = "tr"

    /**
     * Converts legacy picker tags and regional variants to the current app
     * tags. In particular, old generic pt/es selections remain usable.
     */
    fun canonicalize(tag: String): String? {
        val normalized = tag.trim().replace('_', '-').lowercase(Locale.ROOT)
        return when {
            normalized == "en" || normalized.startsWith("en-") -> ENGLISH
            normalized == "ar" || normalized.startsWith("ar-") -> ARABIC
            normalized == "zh-cn" || normalized == "zh-hans" -> SIMPLIFIED_CHINESE
            normalized == "zh-tw" || normalized == "zh-hant" || normalized.startsWith("zh-hk") ->
                TRADITIONAL_CHINESE
            normalized == "zh" -> SIMPLIFIED_CHINESE
            normalized == "fr" || normalized.startsWith("fr-") -> FRENCH
            normalized == "de" || normalized.startsWith("de-") -> GERMAN
            normalized == "hi" || normalized.startsWith("hi-") -> HINDI
            normalized == "id" || normalized == "in" || normalized.startsWith("id-") ->
                INDONESIAN
            normalized == "it" || normalized.startsWith("it-") -> ITALIAN
            normalized == "ja" || normalized.startsWith("ja-") -> JAPANESE
            normalized == "ko" || normalized.startsWith("ko-") -> KOREAN
            normalized == "pt" || normalized.startsWith("pt-") -> PORTUGUESE_BRAZIL
            normalized == "ru" || normalized.startsWith("ru-") -> RUSSIAN
            normalized == "es" || normalized.startsWith("es-") -> SPANISH
            normalized == "tr" || normalized.startsWith("tr-") -> TURKISH
            else -> null
        }
    }
}
