package com.esim.checker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.esim.checker.AppAnalytics
import com.esim.checker.ui.EsimCheckerApp
import com.esim.checker.ui.theme.ESIMCheckerTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppAnalytics.log(this, "app_opened")
        enableEdgeToEdge()

        val checker = EsimCompatibilityChecker(applicationContext)
        setContent {
            var compatibilityResult by remember {
                mutableStateOf<com.esim.checker.EsimCompatibilityResult?>(null)
            }

            ESIMCheckerTheme {
                EsimCheckerApp(
                    result = compatibilityResult,
                    onCheckAgain = {
                        AppAnalytics.log(this, "esim_check_started")
                        try {
                            compatibilityResult = intent.applyDebugEsimSimulation(checker.check())
                            AppAnalytics.log(this, "esim_check_completed")
                        } catch (error: Exception) {
                            AppAnalytics.log(this, "esim_check_failed", "reason", "check_error")
                            throw error
                        }
                    },
                )
            }
        }
    }
}
