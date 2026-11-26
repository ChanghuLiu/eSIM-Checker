package com.esim.checker

import android.content.ClipboardManager
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.esim.checker.ui.EsimCheckerApp
import com.esim.checker.ui.theme.ESIMCheckerTheme
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
        val result = EsimCompatibilityResult(
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
            deviceCodename = Build.DEVICE,
            androidVersion = Build.VERSION.RELEASE,
            hasEuiccFeature = false,
            euiccManagerAvailable = false,
            euiccEnabled = false,
            androidEsimApiAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
            resultStatus = EsimResultStatus.NOT_READY,
        )
        composeRule.activity.setContent {
            ESIMCheckerTheme {
                EsimCheckerApp(
                    result = result,
                    onCheckAgain = {},
                )
            }
        }
        composeRule.waitForIdle()

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
        val verification = ManualVerificationStore(context).readResult()
        val purchaseStore = TravelPurchaseCheckStore(context)
        val carrierUnlocked = purchaseStore.readCarrierUnlocked()
        val providerCompatibility = purchaseStore.readProviderCompatibility()
        val expectedPurchaseReport = result.toPurchaseCheckText(
            context = context,
            settingsVerification = verification,
            carrierUnlocked = carrierUnlocked,
            providerCompatibility = providerCompatibility,
            outcome = TravelPurchaseClassifier.classify(
                androidDetection = result.resultStatus,
                settingsVerification = verification,
                carrierUnlocked = carrierUnlocked,
                providerCompatibility = providerCompatibility,
            ),
        )
        assertEquals(expectedPurchaseReport, purchaseReport)
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
