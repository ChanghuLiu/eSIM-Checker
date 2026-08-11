package com.esim.checker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.esim.checker.ui.EsimCheckerApp
import com.esim.checker.ui.theme.ESIMCheckerTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val checker = EsimCompatibilityChecker(applicationContext)
        val initialResult = checker.check()

        setContent {
            var compatibilityResult by remember { mutableStateOf(initialResult) }

            ESIMCheckerTheme {
                EsimCheckerApp(
                    result = compatibilityResult,
                    onCheckAgain = { compatibilityResult = checker.check() },
                )
            }
        }
    }
}
