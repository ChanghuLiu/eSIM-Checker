package com.esim.checker

import android.content.ClipboardManager
import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipboardActionsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun clipboardActionsContainExpectedValuesWithoutSensitiveOrDebugData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.onNodeWithText(context.getString(R.string.copy_full_report))
            .performScrollTo()
            .performClick()
        val fullReport = clipboardText()
        assertTrue(fullReport.contains(Build.MANUFACTURER))
        assertTrue(fullReport.contains(Build.MODEL))
        assertTrue(fullReport.contains(Build.VERSION.RELEASE))
        assertTrue(fullReport.contains(context.getString(R.string.android_esim_api_label)))
        assertSafeReport(fullReport)

        composeRule.onNodeWithText(context.getString(R.string.travel_esim_check))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.copy_exact_model))
            .performScrollTo()
            .performClick()
        assertEquals(Build.MODEL, clipboardText())

        composeRule.onNodeWithText(context.getString(R.string.copy_purchase_check))
            .performScrollTo()
            .performClick()
        val purchaseReport = clipboardText()
        assertTrue(purchaseReport.contains(Build.MANUFACTURER))
        assertTrue(purchaseReport.contains(Build.MODEL))
        assertTrue(purchaseReport.contains(Build.DEVICE))
        assertTrue(purchaseReport.contains(Build.VERSION.RELEASE))
        assertTrue(purchaseReport.contains("\n\nAutomatic checks:\n"))
        assertTrue(purchaseReport.contains("\n\nManual checks:\n"))
        assertTrue(purchaseReport.contains("\n\nOverall:\n"))
        assertSafeReport(purchaseReport)
    }

    private fun clipboardText(): String {
        composeRule.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        return clipboard.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()
    }

    private fun assertSafeReport(report: String) {
        val lowercaseReport = report.lowercase()
        listOf(
            "imei",
            "\neid",
            "phone number",
            "euiccfeature=",
            "manager=",
            "enabled=",
        ).forEach { forbiddenValue ->
            assertFalse(
                "Clipboard report contains forbidden value: $forbiddenValue",
                lowercaseReport.contains(forbiddenValue),
            )
        }
    }
}
