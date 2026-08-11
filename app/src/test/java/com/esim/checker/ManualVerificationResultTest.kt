package com.esim.checker

import org.junit.Assert.assertEquals
import org.junit.Test

class ManualVerificationResultTest {
    @Test
    fun `stored manual result is restored`() {
        assertEquals(
            ManualVerificationResult.YES,
            ManualVerificationResult.fromStoredValue("YES"),
        )
    }

    @Test
    fun `missing or unknown stored result remains not checked`() {
        assertEquals(
            ManualVerificationResult.NOT_CHECKED,
            ManualVerificationResult.fromStoredValue("UNKNOWN"),
        )
        assertEquals(
            ManualVerificationResult.NOT_CHECKED,
            ManualVerificationResult.fromStoredValue(null),
        )
    }
}
