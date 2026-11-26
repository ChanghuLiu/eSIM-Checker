package com.esim.checker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationContextTest {
    @Test
    fun applicationIdIsCorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Debug runs use an isolated `.adtest` applicationId suffix; release
        // retains the Play package. Assert the manifest and generated config agree.
        assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
    }
}
