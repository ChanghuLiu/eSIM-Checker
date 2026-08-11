package com.esim.checker

import org.junit.Assert.assertEquals
import org.junit.Test

class EsimResultClassifierTest {
    @Test
    fun `feature and enabled manager are ready`() {
        val result = EsimResultClassifier.classify(
            hasEuiccFeature = true,
            euiccManagerAvailable = true,
            euiccEnabled = true,
        )

        assertEquals(EsimResultStatus.READY, result)
    }

    @Test
    fun `feature with disabled manager is partially ready`() {
        val result = EsimResultClassifier.classify(
            hasEuiccFeature = true,
            euiccManagerAvailable = true,
            euiccEnabled = false,
        )

        assertEquals(EsimResultStatus.PARTIALLY_READY, result)
    }

    @Test
    fun `feature with missing manager is partially ready`() {
        val result = EsimResultClassifier.classify(
            hasEuiccFeature = true,
            euiccManagerAvailable = false,
            euiccEnabled = false,
        )

        assertEquals(EsimResultStatus.PARTIALLY_READY, result)
    }

    @Test
    fun `missing feature is not ready regardless of manager state`() {
        val result = EsimResultClassifier.classify(
            hasEuiccFeature = false,
            euiccManagerAvailable = true,
            euiccEnabled = true,
        )

        assertEquals(EsimResultStatus.NOT_READY, result)
    }

}
