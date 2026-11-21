package com.esim.checker

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Sends fixed workflow events only. If no Firebase config is installed for this
 * app, analytics stays inactive and the app remains usable.
 */
object AppAnalytics {
    fun log(context: Context, event: String, parameter: String? = null, value: String? = null) {
        val appContext = context.applicationContext
        val firebaseApp = runCatching {
            FirebaseApp.getApps(appContext).firstOrNull() ?: FirebaseApp.initializeApp(appContext)
        }.getOrNull() ?: return
        val analytics = runCatching { FirebaseAnalytics.getInstance(firebaseApp) }.getOrNull() ?: return
        val parameters = Bundle()
        if (parameter != null && value != null) parameters.putString(parameter, value)
        analytics.logEvent(event, parameters)
    }
}
