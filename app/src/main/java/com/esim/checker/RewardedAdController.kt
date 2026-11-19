package com.esim.checker

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Rewarded-only ad flow. Debug uses Google's test IDs. Release remains closed by default and
 * can be enabled only with -PenableProductionRewardedAds=true after policy/readiness gates pass.
 */
class RewardedAdController(
    private val context: Context,
    private val onReadyChanged: (Boolean) -> Unit,
    private val onPrivacyOptionsRequiredChanged: (Boolean) -> Unit,
) {
    private var rewarded: RewardedAd? = null
    private var loading = false
    private var showing = false
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)
    private var requestAdsAllowed = false
    private var sdkInitialized = false
    private var consentRequestStarted = false

    fun start(activity: Activity) {
        if (!adsFeatureEnabled()) return
        if (BuildConfig.DEBUG && !BuildConfig.UMP_TEST_FLOW_ENABLED) {
            requestAdsAllowed = true
            initializeAndLoad()
            return
        }
        if (consentRequestStarted) return
        consentRequestStarted = true
        val parametersBuilder = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG &&
            BuildConfig.UMP_TEST_FLOW_ENABLED &&
            BuildConfig.UMP_TEST_DEVICE_HASHED_ID.isNotBlank()
        ) {
            val debugSettings = ConsentDebugSettings.Builder(context)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(BuildConfig.UMP_TEST_DEVICE_HASHED_ID)
                .build()
            parametersBuilder.setConsentDebugSettings(debugSettings)
        }
        val parameters = parametersBuilder.build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                updatePrivacyOptionsRequirement()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updatePrivacyOptionsRequirement()
                    requestAdsAllowed = consentInformation.canRequestAds()
                    if (requestAdsAllowed) initializeAndLoad()
                }
            },
            {
                // UMP may still hold a valid choice from an earlier session.
                updatePrivacyOptionsRequirement()
                requestAdsAllowed = consentInformation.canRequestAds()
                if (requestAdsAllowed) initializeAndLoad()
            },
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        if ((BuildConfig.DEBUG && !BuildConfig.UMP_TEST_FLOW_ENABLED) ||
            !adsFeatureEnabled()
        ) return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            updatePrivacyOptionsRequirement()
            requestAdsAllowed = consentInformation.canRequestAds()
            if (requestAdsAllowed) initializeAndLoad()
        }
    }

    private fun updatePrivacyOptionsRequirement() {
        onPrivacyOptionsRequiredChanged(
            (!BuildConfig.DEBUG || BuildConfig.UMP_TEST_FLOW_ENABLED) &&
                consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
        )
    }

    private fun adsFeatureEnabled(): Boolean =
        BuildConfig.DEBUG || BuildConfig.REWARDED_ADS_ENABLED

    private fun initializeAndLoad() {
        if (!adsFeatureEnabled() || !requestAdsAllowed || sdkInitialized) return
        sdkInitialized = true
        MobileAds.initialize(context) { loadRewarded() }
    }

    fun loadRewarded() {
        if (!adsFeatureEnabled() || !requestAdsAllowed || loading || rewarded != null) return
        loading = true
        RewardedAd.load(context, BuildConfig.ADMOB_REWARDED_AD_UNIT_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loading = false
                    rewarded = ad
                    onReadyChanged(true)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    rewarded = null
                    onReadyChanged(false)
                }
            })
    }

    fun showRewarded(
        activity: Activity,
        onEarned: () -> Unit,
        onClosedWithoutReward: () -> Unit,
        onUnavailable: () -> Unit,
    ) {
        if (!adsFeatureEnabled() || !requestAdsAllowed || showing) {
            onUnavailable()
            return
        }
        val ad = rewarded ?: run {
            loadRewarded()
            onUnavailable()
            return
        }
        rewarded = null
        onReadyChanged(false)
        showing = true
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                showing = false
                if (!earned) onClosedWithoutReward()
                loadRewarded()
            }
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                showing = false
                onUnavailable()
                loadRewarded()
            }
        }
        ad.show(activity) {
            if (!earned) {
                earned = true
                onEarned()
            }
        }
    }
}
