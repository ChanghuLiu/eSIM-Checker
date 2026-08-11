package com.esim.checker

import org.junit.Assert.assertEquals
import org.junit.Test

class TravelPurchaseClassifierTest {
    @Test
    fun `all required checks passing is ready to buy`() {
        assertEquals(
            TravelPurchaseOutcome.READY_TO_BUY,
            classify(
                androidDetection = EsimResultStatus.READY,
                settingsVerification = ManualVerificationResult.YES,
                carrierUnlocked = PurchaseCheckAnswer.YES,
                providerCompatibility = PurchaseCheckAnswer.YES,
            ),
        )
    }

    @Test
    fun `incomplete answers require verification`() {
        assertEquals(
            TravelPurchaseOutcome.VERIFY_BEFORE_BUYING,
            classify(),
        )
    }

    @Test
    fun `partial Android detection requires verification even when answers are yes`() {
        assertEquals(
            TravelPurchaseOutcome.VERIFY_BEFORE_BUYING,
            classify(
                androidDetection = EsimResultStatus.PARTIALLY_READY,
                settingsVerification = ManualVerificationResult.YES,
                carrierUnlocked = PurchaseCheckAnswer.YES,
                providerCompatibility = PurchaseCheckAnswer.YES,
            ),
        )
    }

    @Test
    fun `settings answer no remains a verification outcome`() {
        assertEquals(
            TravelPurchaseOutcome.VERIFY_BEFORE_BUYING,
            classify(
                settingsVerification = ManualVerificationResult.NO,
                carrierUnlocked = PurchaseCheckAnswer.YES,
                providerCompatibility = PurchaseCheckAnswer.YES,
            ),
        )
    }

    @Test
    fun `hardware not detected is do not buy yet`() {
        assertEquals(
            TravelPurchaseOutcome.DO_NOT_BUY_YET,
            classify(androidDetection = EsimResultStatus.NOT_READY),
        )
    }

    @Test
    fun `carrier locked answer is do not buy yet`() {
        assertEquals(
            TravelPurchaseOutcome.DO_NOT_BUY_YET,
            classify(carrierUnlocked = PurchaseCheckAnswer.NO),
        )
    }

    @Test
    fun `provider incompatibility answer is do not buy yet`() {
        assertEquals(
            TravelPurchaseOutcome.DO_NOT_BUY_YET,
            classify(providerCompatibility = PurchaseCheckAnswer.NO),
        )
    }

    @Test
    fun `not checked and not sure answers are listed as missing`() {
        assertEquals(
            listOf(
                ManualCheckItem.SETTINGS_ESIM_OPTION,
                ManualCheckItem.CARRIER_UNLOCK,
                ManualCheckItem.PROVIDER_EXACT_MODEL,
            ),
            PurchaseCheckProgress.missingManualChecks(
                settingsVerification = ManualVerificationResult.NOT_CHECKED,
                carrierUnlocked = PurchaseCheckAnswer.NOT_SURE,
                providerCompatibility = PurchaseCheckAnswer.NOT_CHECKED,
            ),
        )
    }

    @Test
    fun `explicit yes and no answers are not missing`() {
        assertEquals(
            emptyList<ManualCheckItem>(),
            PurchaseCheckProgress.missingManualChecks(
                settingsVerification = ManualVerificationResult.NO,
                carrierUnlocked = PurchaseCheckAnswer.YES,
                providerCompatibility = PurchaseCheckAnswer.NO,
            ),
        )
    }

    @Test
    fun `entered answer detection ignores only untouched state`() {
        assertEquals(
            false,
            PurchaseCheckProgress.hasEnteredAnswers(
                settingsVerification = ManualVerificationResult.NOT_CHECKED,
                carrierUnlocked = PurchaseCheckAnswer.NOT_CHECKED,
                providerCompatibility = PurchaseCheckAnswer.NOT_CHECKED,
            ),
        )
        assertEquals(
            true,
            PurchaseCheckProgress.hasEnteredAnswers(
                settingsVerification = ManualVerificationResult.NOT_SURE,
                carrierUnlocked = PurchaseCheckAnswer.NOT_CHECKED,
                providerCompatibility = PurchaseCheckAnswer.NOT_CHECKED,
            ),
        )
    }

    @Test
    fun `checks completed requires positive automatic and manual checks`() {
        assertEquals(
            true,
            PurchaseCheckProgress.allChecksPositive(
                androidDetection = EsimResultStatus.READY,
                settingsVerification = ManualVerificationResult.YES,
                carrierUnlocked = PurchaseCheckAnswer.YES,
                providerCompatibility = PurchaseCheckAnswer.YES,
            ),
        )
        assertEquals(
            false,
            PurchaseCheckProgress.allChecksPositive(
                androidDetection = EsimResultStatus.PARTIALLY_READY,
                settingsVerification = ManualVerificationResult.YES,
                carrierUnlocked = PurchaseCheckAnswer.YES,
                providerCompatibility = PurchaseCheckAnswer.YES,
            ),
        )
    }

    private fun classify(
        androidDetection: EsimResultStatus = EsimResultStatus.READY,
        settingsVerification: ManualVerificationResult = ManualVerificationResult.NOT_CHECKED,
        carrierUnlocked: PurchaseCheckAnswer = PurchaseCheckAnswer.NOT_CHECKED,
        providerCompatibility: PurchaseCheckAnswer = PurchaseCheckAnswer.NOT_CHECKED,
    ): TravelPurchaseOutcome = TravelPurchaseClassifier.classify(
        androidDetection = androidDetection,
        settingsVerification = settingsVerification,
        carrierUnlocked = carrierUnlocked,
        providerCompatibility = providerCompatibility,
    )
}
