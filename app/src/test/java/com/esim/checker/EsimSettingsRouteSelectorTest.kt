package com.esim.checker

import org.junit.Assert.assertEquals
import org.junit.Test

class EsimSettingsRouteSelectorTest {
    @Test
    fun `api 27 never selects embedded subscriptions route`() {
        val route = EsimSettingsRouteSelector.selectForSdk(
            sdkInt = 27,
            embeddedSubscriptionsAvailable = true,
            wirelessSettingsAvailable = true,
        )

        assertEquals(EsimSettingsRoute.WIRELESS_SETTINGS, route)
    }

    @Test
    fun `dedicated embedded subscriptions route is preferred`() {
        val route = EsimSettingsRouteSelector.select(
            embeddedSubscriptionsAvailable = true,
            wirelessSettingsAvailable = true,
        )

        assertEquals(EsimSettingsRoute.EMBEDDED_SUBSCRIPTIONS, route)
    }

    @Test
    fun `wireless settings is used when dedicated route is unavailable`() {
        val route = EsimSettingsRouteSelector.select(
            embeddedSubscriptionsAvailable = false,
            wirelessSettingsAvailable = true,
        )

        assertEquals(EsimSettingsRoute.WIRELESS_SETTINGS, route)
    }

    @Test
    fun `unavailable is returned when neither route exists`() {
        val route = EsimSettingsRouteSelector.select(
            embeddedSubscriptionsAvailable = false,
            wirelessSettingsAvailable = false,
        )

        assertEquals(EsimSettingsRoute.UNAVAILABLE, route)
    }
}
