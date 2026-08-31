package com.esim.checker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLocaleTagsTest {
    @Test
    fun legacyAndRegionalTagsNormalizeToManualLanguages() {
        assertEquals(AppLocaleTags.INDONESIAN, AppLocaleTags.canonicalize("in"))
        assertEquals(AppLocaleTags.INDONESIAN, AppLocaleTags.canonicalize("id-ID"))
        assertEquals(AppLocaleTags.PORTUGUESE_BRAZIL, AppLocaleTags.canonicalize("pt"))
        assertEquals(AppLocaleTags.PORTUGUESE_BRAZIL, AppLocaleTags.canonicalize("pt-PT"))
        assertEquals(AppLocaleTags.SPANISH, AppLocaleTags.canonicalize("es"))
        assertEquals(AppLocaleTags.SPANISH, AppLocaleTags.canonicalize("es-MX"))
        assertEquals(AppLocaleTags.TRADITIONAL_CHINESE, AppLocaleTags.canonicalize("zh-HK"))
    }

    @Test
    fun unknownTagsAreNotSilentlyMapped() {
        assertNull(AppLocaleTags.canonicalize("xx"))
    }
}
