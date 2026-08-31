package com.esim.checker

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

enum class EsimSettingsRoute {
    EMBEDDED_SUBSCRIPTIONS,
    WIRELESS_SETTINGS,
    UNAVAILABLE,
}

data class EsimSettingsLaunchResult(
    val launched: Boolean,
    val route: EsimSettingsRoute,
)

/** Pure route selection kept separate for JVM tests. */
object EsimSettingsRouteSelector {
    fun select(
        embeddedSubscriptionsAvailable: Boolean,
        wirelessSettingsAvailable: Boolean,
    ): EsimSettingsRoute = when {
        embeddedSubscriptionsAvailable -> EsimSettingsRoute.EMBEDDED_SUBSCRIPTIONS
        wirelessSettingsAvailable -> EsimSettingsRoute.WIRELESS_SETTINGS
        else -> EsimSettingsRoute.UNAVAILABLE
    }

    fun selectForSdk(
        sdkInt: Int,
        embeddedSubscriptionsAvailable: Boolean,
        wirelessSettingsAvailable: Boolean,
    ): EsimSettingsRoute = select(
        embeddedSubscriptionsAvailable = sdkInt >= Build.VERSION_CODES.P &&
            embeddedSubscriptionsAvailable,
        wirelessSettingsAvailable = wirelessSettingsAvailable,
    )
}

object EsimSettingsNavigator {
    fun open(
        context: Context,
        launch: (Intent) -> Unit,
    ): EsimSettingsLaunchResult {
        val wirelessSettingsIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        val embeddedSubscriptionsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Api28SettingsIntent.create()
        } else {
            null
        }
        val embeddedSubscriptionsAvailable = embeddedSubscriptionsIntent != null &&
            !context.forceDebugWirelessSettings() &&
            canHandle(context = context, intent = embeddedSubscriptionsIntent)
        val wirelessSettingsAvailable = canHandle(
            context = context,
            intent = wirelessSettingsIntent,
        )

        val initialRoute = EsimSettingsRouteSelector.select(
            embeddedSubscriptionsAvailable = embeddedSubscriptionsAvailable,
            wirelessSettingsAvailable = wirelessSettingsAvailable,
        )

        if (initialRoute == EsimSettingsRoute.EMBEDDED_SUBSCRIPTIONS &&
            embeddedSubscriptionsIntent != null &&
            launchSafely(embeddedSubscriptionsIntent, launch)
        ) {
            return EsimSettingsLaunchResult(
                launched = true,
                route = EsimSettingsRoute.EMBEDDED_SUBSCRIPTIONS,
            )
        }

        if (wirelessSettingsAvailable && launchSafely(wirelessSettingsIntent, launch)) {
            return EsimSettingsLaunchResult(
                launched = true,
                route = EsimSettingsRoute.WIRELESS_SETTINGS,
            )
        }

        return EsimSettingsLaunchResult(
            launched = false,
            route = EsimSettingsRoute.UNAVAILABLE,
        )
    }

    @Suppress("DEPRECATION")
    private fun canHandle(
        context: Context,
        intent: Intent,
    ): Boolean = try {
        context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ) != null
    } catch (_: RuntimeException) {
        false
    }

    private fun launchSafely(
        intent: Intent,
        launch: (Intent) -> Unit,
    ): Boolean = try {
        launch(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: RuntimeException) {
        false
    }
}

@RequiresApi(Build.VERSION_CODES.P)
private object Api28SettingsIntent {
    fun create(): Intent = Intent(
        android.telephony.euicc.EuiccManager.ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS,
    )
}
