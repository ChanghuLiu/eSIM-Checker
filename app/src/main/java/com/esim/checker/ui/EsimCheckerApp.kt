package com.esim.checker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esim.checker.BuildConfig
import com.esim.checker.AppLocaleTags
import com.esim.checker.bidiIsolate
import com.esim.checker.EsimCompatibilityResult
import com.esim.checker.EsimResultStatus
import com.esim.checker.EsimSettingsNavigator
import com.esim.checker.ManualVerificationResult
import com.esim.checker.ManualVerificationStore
import com.esim.checker.ManualCheckItem
import com.esim.checker.PurchaseCheckAnswer
import com.esim.checker.PurchaseCheckProgress
import com.esim.checker.R
import com.esim.checker.TravelPurchaseCheckStore
import com.esim.checker.TravelPurchaseClassifier
import com.esim.checker.TravelPurchaseOutcome
import com.esim.checker.labelRes
import com.esim.checker.toFullReportText
import com.esim.checker.toPurchaseCheckText

private data class StatusContent(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:StringRes val symbolRes: Int,
    @param:StringRes val symbolDescriptionRes: Int,
    @param:StringRes val readinessLabelRes: Int,
)

private data class StatusColors(
    val foreground: Color,
    val background: Color,
)

private data class SettingsVerificationContent(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:StringRes val buttonRes: Int,
)

private data class LanguageOption(
    val languageTag: String?,
    @param:StringRes val labelRes: Int,
)

private enum class AppPage {
    MAIN,
    TRAVEL_CHECK,
    HOW_TO,
    ABOUT,
    PRIVACY,
}

private enum class PurchaseSignal {
    PASS,
    UNCERTAIN,
    FAIL,
}

@Composable
fun EsimCheckerApp(
    result: EsimCompatibilityResult,
    onCheckAgain: () -> Unit,
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val verificationStore = remember(applicationContext) {
        ManualVerificationStore(applicationContext)
    }
    val purchaseCheckStore = remember(applicationContext) {
        TravelPurchaseCheckStore(applicationContext)
    }
    var currentPage by rememberSaveable { mutableStateOf(AppPage.MAIN) }
    var howToReturnPage by rememberSaveable { mutableStateOf(AppPage.MAIN) }
    var manualVerificationResult by rememberSaveable {
        mutableStateOf(verificationStore.readResult())
    }
    var showManualVerification by rememberSaveable {
        mutableStateOf(verificationStore.shouldShowVerificationCard())
    }
    var carrierUnlocked by rememberSaveable {
        mutableStateOf(purchaseCheckStore.readCarrierUnlocked())
    }
    var providerCompatibility by rememberSaveable {
        mutableStateOf(purchaseCheckStore.readProviderCompatibility())
    }
    val settingsUnavailableMessage = stringResource(R.string.settings_unavailable)
    val unknownValue = stringResource(R.string.unknown_value)
    val feedbackEmail = stringResource(R.string.contact_email)
    val feedbackSubject = stringResource(R.string.feedback_subject)
    val feedbackEmailBody = stringResource(
        R.string.feedback_email_body,
        BuildConfig.VERSION_NAME,
        Build.VERSION.RELEASE.ifBlank { unknownValue },
        Build.MANUFACTURER.ifBlank { unknownValue },
        Build.MODEL.ifBlank { unknownValue },
    )
    val feedbackNoEmailAppMessage = stringResource(R.string.feedback_no_email_app)
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        verificationStore.markSettingsVisited()
        showManualVerification = true
    }
    val openSettings = {
        val launchResult = EsimSettingsNavigator.open(context) { intent ->
            settingsLauncher.launch(intent)
        }
        if (!launchResult.launched) {
            Toast.makeText(
                context,
                settingsUnavailableMessage,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    if (currentPage != AppPage.MAIN) {
        BackHandler {
            currentPage = when (currentPage) {
                AppPage.HOW_TO -> howToReturnPage
                AppPage.PRIVACY -> AppPage.ABOUT
                AppPage.ABOUT, AppPage.TRAVEL_CHECK, AppPage.MAIN -> AppPage.MAIN
            }
        }
    }

    when (currentPage) {
        AppPage.MAIN -> CompatibilityScreen(
            result = result,
            manualVerificationResult = manualVerificationResult,
            showManualVerification = showManualVerification,
            onCheckAgain = onCheckAgain,
            onOpenSettings = openSettings,
            onManualVerificationChanged = { updatedResult ->
                manualVerificationResult = updatedResult
                verificationStore.saveResult(updatedResult)
            },
            onShowTravelCheck = { currentPage = AppPage.TRAVEL_CHECK },
            onShowInstructions = {
                howToReturnPage = AppPage.MAIN
                currentPage = AppPage.HOW_TO
            },
            onShowAbout = { currentPage = AppPage.ABOUT },
        )

        AppPage.TRAVEL_CHECK -> TravelEsimCheckScreen(
            result = result,
            settingsVerification = manualVerificationResult,
            carrierUnlocked = carrierUnlocked,
            providerCompatibility = providerCompatibility,
            onBack = { currentPage = AppPage.MAIN },
            onOpenSettings = openSettings,
            onSettingsVerificationChanged = { updatedResult ->
                manualVerificationResult = updatedResult
                verificationStore.saveResult(updatedResult)
            },
            onCarrierUnlockedChanged = { updatedAnswer ->
                carrierUnlocked = updatedAnswer
                purchaseCheckStore.saveCarrierUnlocked(updatedAnswer)
            },
            onProviderCompatibilityChanged = { updatedAnswer ->
                providerCompatibility = updatedAnswer
                purchaseCheckStore.saveProviderCompatibility(updatedAnswer)
            },
            onStartNewCheck = {
                manualVerificationResult = ManualVerificationResult.NOT_CHECKED
                carrierUnlocked = PurchaseCheckAnswer.NOT_CHECKED
                providerCompatibility = PurchaseCheckAnswer.NOT_CHECKED
                showManualVerification = false
                verificationStore.reset()
                purchaseCheckStore.reset()
                onCheckAgain()
            },
        )

        AppPage.HOW_TO -> HowToAddEsimScreen(
            onBack = { currentPage = howToReturnPage },
        )

        AppPage.ABOUT -> AboutScreen(
            onBack = { currentPage = AppPage.MAIN },
            onShowPrivacy = { currentPage = AppPage.PRIVACY },
            onShowInstructions = {
                howToReturnPage = AppPage.ABOUT
                currentPage = AppPage.HOW_TO
            },
            onSendFeedback = {
                val feedbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.fromParts("mailto", feedbackEmail, null)
                    putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
                    putExtra(Intent.EXTRA_TEXT, feedbackEmailBody)
                }
                try {
                    context.startActivity(feedbackIntent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                        context,
                        feedbackNoEmailAppMessage,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )

        AppPage.PRIVACY -> PrivacyPolicyScreen(
            onBack = { currentPage = AppPage.ABOUT },
        )
    }
}

@Composable
private fun CompatibilityScreen(
    result: EsimCompatibilityResult,
    manualVerificationResult: ManualVerificationResult,
    showManualVerification: Boolean,
    onCheckAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    onManualVerificationChanged: (ManualVerificationResult) -> Unit,
    onShowTravelCheck: () -> Unit,
    onShowInstructions: () -> Unit,
    onShowAbout: () -> Unit,
) {
    val status = statusContent(result.resultStatus)
    val statusColors = statusColors(result.resultStatus)
    val context = LocalContext.current
    val clipboardLabel = stringResource(R.string.clipboard_label)
    val copiedMessage = stringResource(R.string.full_report_copied)
    val symbolDescription = stringResource(status.symbolDescriptionRes)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.size(18.dp))

            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .semantics { contentDescription = symbolDescription },
                shape = CircleShape,
                color = statusColors.background,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(status.symbolRes),
                        color = statusColors.foreground,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = stringResource(status.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(status.descriptionRes),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.size(20.dp))

            DeviceCard(result = result)

            Spacer(modifier = Modifier.size(12.dp))

            CompatibilityCard(result = result)

            Text(
                text = stringResource(R.string.feedback_guidance),
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.size(12.dp))

            SettingsVerificationActionCard(
                resultStatus = result.resultStatus,
                onOpenSettings = onOpenSettings,
            )

            if (showManualVerification) {
                Spacer(modifier = Modifier.size(12.dp))
                ManualVerificationCard(
                    result = manualVerificationResult,
                    onResultSelected = onManualVerificationChanged,
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            TravelCheckEntryCard(onClick = onShowTravelCheck)

            Spacer(modifier = Modifier.size(12.dp))

            ReadinessCard(
                result = result,
                status = status,
                statusColors = statusColors,
            )

            Spacer(modifier = Modifier.size(12.dp))

            BeforeYouBuyCard(result = result)

            OutlinedButton(
                onClick = onCheckAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                Text(stringResource(R.string.check_again))
            }

            OutlinedButton(
                onClick = onShowInstructions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.how_to_add_esim))
            }

            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(
                        ClipData.newPlainText(
                            clipboardLabel,
                            result.toFullReportText(
                                context = context,
                                manualVerificationResult = manualVerificationResult,
                            ),
                        ),
                    )
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.copy_full_report))
            }

            Spacer(modifier = Modifier.size(8.dp))

            LanguageCard()

            TextButton(
                onClick = onShowAbout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.about))
            }

            Text(
                text = stringResource(R.string.compatibility_disclaimer),
                modifier = Modifier.padding(top = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.privacy_footer),
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (BuildConfig.DEBUG) {
                Text(
                    text = stringResource(
                        R.string.debug_status,
                        result.hasEuiccFeature,
                        result.euiccManagerAvailable,
                        result.euiccEnabled,
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(result: EsimCompatibilityResult) {
    val context = LocalContext.current
    InfoCard(title = stringResource(R.string.device_section)) {
        Text(
            text = deviceDisplayName(result),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
        InfoRow(
            label = stringResource(R.string.manufacturer_label),
            value = context.bidiIsolate(result.deviceManufacturer),
        )
        InfoRow(
            label = stringResource(R.string.device_model_label),
            value = context.bidiIsolate(result.deviceModel),
        )
        InfoRow(
            label = stringResource(R.string.android_version_label),
            value = context.bidiIsolate(result.androidVersion),
            isLast = true,
        )
    }
}

@Composable
private fun CompatibilityCard(result: EsimCompatibilityResult) {
    InfoCard(title = stringResource(R.string.compatibility_section)) {
        InfoRow(
            label = stringResource(R.string.esim_hardware_label),
            value = stringResource(
                if (result.hasEuiccFeature) R.string.supported else R.string.not_detected,
            ),
            valueColor = signalColor(result.hasEuiccFeature),
        )
        InfoRow(
            label = stringResource(R.string.esim_service_label),
            value = stringResource(
                if (result.euiccEnabled) R.string.available else R.string.unavailable,
            ),
            valueColor = signalColor(result.euiccEnabled),
        )
        InfoRow(
            label = stringResource(R.string.android_esim_api_label),
            value = stringResource(
                if (result.androidEsimApiAvailable) R.string.supported else R.string.unsupported,
            ),
            valueColor = signalColor(result.androidEsimApiAvailable),
            isLast = true,
        )
    }
}

@Composable
private fun SettingsVerificationActionCard(
    resultStatus: EsimResultStatus,
    onOpenSettings: () -> Unit,
) {
    val content = settingsVerificationContent(resultStatus)

    InfoCard(title = stringResource(content.titleRes)) {
        Text(
            text = stringResource(content.descriptionRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            Text(stringResource(content.buttonRes))
        }
    }
}

@Composable
private fun ManualVerificationCard(
    result: ManualVerificationResult,
    onResultSelected: (ManualVerificationResult) -> Unit,
) {
    InfoCard(title = stringResource(R.string.did_you_see_esim_option)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ManualVerificationChoice(
                labelRes = R.string.yes,
                selected = result == ManualVerificationResult.YES,
                onClick = { onResultSelected(ManualVerificationResult.YES) },
                modifier = Modifier.weight(1f),
            )
            ManualVerificationChoice(
                labelRes = R.string.no,
                selected = result == ManualVerificationResult.NO,
                onClick = { onResultSelected(ManualVerificationResult.NO) },
                modifier = Modifier.weight(1f),
            )
            ManualVerificationChoice(
                labelRes = R.string.not_sure,
                selected = result == ManualVerificationResult.NOT_SURE,
                onClick = { onResultSelected(ManualVerificationResult.NOT_SURE) },
                modifier = Modifier.weight(1f),
            )
        }

        if (result != ManualVerificationResult.NOT_CHECKED) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            when (result) {
                ManualVerificationResult.YES -> ManualVerificationMessage(
                    titleRes = R.string.esim_option_found,
                    descriptionRes = R.string.esim_option_found_description,
                )

                ManualVerificationResult.NO -> ManualVerificationMessage(
                    titleRes = R.string.no_esim_option_found,
                    descriptionRes = R.string.no_esim_option_found_description,
                )

                ManualVerificationResult.NOT_SURE -> Text(
                    text = stringResource(R.string.not_sure_instructions),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )

                ManualVerificationResult.NOT_CHECKED -> Unit
            }
        }

        Text(
            text = stringResource(R.string.manual_observation_note),
            modifier = Modifier.padding(top = 14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ManualVerificationChoice(
    @StringRes labelRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(
                text = stringResource(labelRes),
                textAlign = TextAlign.Center,
            )
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(
                text = stringResource(labelRes),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ManualVerificationMessage(
    @StringRes titleRes: Int,
    @StringRes descriptionRes: Int,
) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = stringResource(descriptionRes),
        modifier = Modifier.padding(top = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun TravelCheckEntryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.travel_esim_check),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.travel_esim_check_subtitle),
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TravelEsimCheckScreen(
    result: EsimCompatibilityResult,
    settingsVerification: ManualVerificationResult,
    carrierUnlocked: PurchaseCheckAnswer,
    providerCompatibility: PurchaseCheckAnswer,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSettingsVerificationChanged: (ManualVerificationResult) -> Unit,
    onCarrierUnlockedChanged: (PurchaseCheckAnswer) -> Unit,
    onProviderCompatibilityChanged: (PurchaseCheckAnswer) -> Unit,
    onStartNewCheck: () -> Unit,
) {
    val context = LocalContext.current
    var showStartNewCheckDialog by rememberSaveable { mutableStateOf(false) }
    val outcome = TravelPurchaseClassifier.classify(
        androidDetection = result.resultStatus,
        settingsVerification = settingsVerification,
        carrierUnlocked = carrierUnlocked,
        providerCompatibility = providerCompatibility,
    )
    val reportClipboardLabel = stringResource(R.string.purchase_check_clipboard_label)
    val reportCopiedMessage = stringResource(R.string.purchase_check_copied)
    val modelClipboardLabel = stringResource(R.string.exact_model_clipboard_label)
    val modelCopiedMessage = stringResource(R.string.exact_model_copied)
    val newCheckStartedMessage = stringResource(R.string.new_check_started)
    val hasEnteredAnswers = PurchaseCheckProgress.hasEnteredAnswers(
        settingsVerification = settingsVerification,
        carrierUnlocked = carrierUnlocked,
        providerCompatibility = providerCompatibility,
    )
    val startNewCheck = {
        onStartNewCheck()
        Toast.makeText(context, newCheckStartedMessage, Toast.LENGTH_SHORT).show()
    }

    if (showStartNewCheckDialog) {
        AlertDialog(
            onDismissRequest = { showStartNewCheckDialog = false },
            title = { Text(stringResource(R.string.start_new_check)) },
            text = { Text(stringResource(R.string.start_new_check_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        showStartNewCheckDialog = false
                        startNewCheck()
                    },
                ) {
                    Text(stringResource(R.string.start_new_check_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartNewCheckDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.travel_esim_check),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.travel_esim_check_subtitle),
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )

            PurchaseStepCard(
                number = 1,
                titleRes = R.string.step_device_esim_support,
            ) {
                PurchaseSignalLabel(
                    signal = result.resultStatus.toPurchaseSignal(),
                    labelRes = when (result.resultStatus) {
                        EsimResultStatus.READY -> R.string.purchase_status_pass
                        EsimResultStatus.PARTIALLY_READY -> {
                            R.string.readiness_label_needs_verification
                        }
                        EsimResultStatus.NOT_READY -> R.string.readiness_label_not_detected
                    },
                )
                Text(
                    text = stringResource(R.string.automatic_android_detection_note),
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            PurchaseStepCard(
                number = 2,
                titleRes = R.string.step_settings_option,
            ) {
                Text(
                    text = stringResource(R.string.settings_option_question),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(settingsVerification.labelRes()),
                    modifier = Modifier.padding(top = 8.dp),
                    color = purchaseSignalColor(settingsVerification.toPurchaseSignal()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                SettingsVerificationChoices(
                    selected = settingsVerification,
                    onSelected = onSettingsVerificationChanged,
                    modifier = Modifier.padding(top = 12.dp),
                )
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.open_esim_settings))
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            PurchaseStepCard(
                number = 3,
                titleRes = R.string.step_carrier_unlock,
            ) {
                Text(
                    text = stringResource(R.string.carrier_unlock_question),
                    style = MaterialTheme.typography.bodyMedium,
                )
                PurchaseAnswerChoices(
                    selected = carrierUnlocked,
                    onSelected = onCarrierUnlockedChanged,
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (carrierUnlocked == PurchaseCheckAnswer.NOT_SURE) {
                    Text(
                        text = stringResource(R.string.carrier_locked_warning),
                        modifier = Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = stringResource(R.string.carrier_lock_manual_note),
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            PurchaseStepCard(
                number = 4,
                titleRes = R.string.step_provider_compatibility,
            ) {
                Text(
                    text = stringResource(R.string.provider_compatibility_question),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.exact_model),
                    modifier = Modifier.padding(top = 14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = context.bidiIsolate(result.deviceModel),
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = {
                        copyToClipboard(
                            context = context,
                            label = modelClipboardLabel,
                            value = result.deviceModel,
                            confirmation = modelCopiedMessage,
                        )
                    },
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(stringResource(R.string.copy_exact_model))
                }
                PurchaseAnswerChoices(
                    selected = providerCompatibility,
                    onSelected = onProviderCompatibilityChanged,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = stringResource(R.string.regional_model_explanation),
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            SystemEvidenceCard(
                result = result,
                settingsVerification = settingsVerification,
                carrierUnlocked = carrierUnlocked,
                providerCompatibility = providerCompatibility,
            )

            VerificationProgressCard(
                result = result,
                settingsVerification = settingsVerification,
                carrierUnlocked = carrierUnlocked,
                providerCompatibility = providerCompatibility,
                modifier = Modifier.padding(top = 12.dp),
            )

            Spacer(modifier = Modifier.size(12.dp))

            PurchaseOutcomeCard(outcome = outcome)

            Spacer(modifier = Modifier.size(12.dp))

            PurchaseSummaryCard(
                result = result,
                settingsVerification = settingsVerification,
                carrierUnlocked = carrierUnlocked,
                providerCompatibility = providerCompatibility,
                outcome = outcome,
            )

            Button(
                onClick = {
                    copyToClipboard(
                        context = context,
                        label = reportClipboardLabel,
                        value = result.toPurchaseCheckText(
                            context = context,
                            settingsVerification = settingsVerification,
                            carrierUnlocked = carrierUnlocked,
                            providerCompatibility = providerCompatibility,
                            outcome = outcome,
                        ),
                        confirmation = reportCopiedMessage,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            ) {
                Text(stringResource(R.string.copy_purchase_check))
            }
            OutlinedButton(
                onClick = {
                    if (hasEnteredAnswers) {
                        showStartNewCheckDialog = true
                    } else {
                        startNewCheck()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                Text(stringResource(R.string.start_new_check))
            }
            Text(
                text = stringResource(R.string.purchase_check_local_note),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SystemEvidenceCard(
    result: EsimCompatibilityResult,
    settingsVerification: ManualVerificationResult,
    carrierUnlocked: PurchaseCheckAnswer,
    providerCompatibility: PurchaseCheckAnswer,
) {
    InfoCard(title = stringResource(R.string.system_evidence)) {
        Text(
            text = stringResource(R.string.automatically_detected),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.size(12.dp))
        InfoRow(
            label = stringResource(R.string.esim_hardware_label),
            value = stringResource(
                if (result.hasEuiccFeature) R.string.detected else R.string.not_detected,
            ),
            valueColor = signalColor(result.hasEuiccFeature),
        )
        InfoRow(
            label = stringResource(R.string.esim_service_label),
            value = stringResource(
                if (result.euiccEnabled) R.string.available else R.string.unavailable,
            ),
            valueColor = signalColor(result.euiccEnabled),
        )
        InfoRow(
            label = stringResource(R.string.android_esim_api_label),
            value = stringResource(
                if (result.androidEsimApiAvailable) R.string.supported else R.string.unsupported,
            ),
            valueColor = signalColor(result.androidEsimApiAvailable),
        )
        InfoRow(
            label = stringResource(R.string.exact_device_model),
            value = LocalContext.current.bidiIsolate(result.deviceModel),
            isLast = true,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = stringResource(R.string.user_verified),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.size(12.dp))
        InfoRow(
            label = stringResource(R.string.evidence_settings_option),
            value = stringResource(settingsVerification.labelRes()),
            valueColor = purchaseSignalColor(settingsVerification.toPurchaseSignal()),
        )
        InfoRow(
            label = stringResource(R.string.evidence_carrier_unlocked),
            value = stringResource(carrierUnlocked.labelRes()),
            valueColor = purchaseSignalColor(carrierUnlocked.toPurchaseSignal()),
        )
        InfoRow(
            label = stringResource(R.string.evidence_provider_exact_model),
            value = stringResource(providerCompatibility.labelRes()),
            valueColor = purchaseSignalColor(providerCompatibility.toPurchaseSignal()),
            isLast = true,
        )
    }
}

@Composable
private fun VerificationProgressCard(
    result: EsimCompatibilityResult,
    settingsVerification: ManualVerificationResult,
    carrierUnlocked: PurchaseCheckAnswer,
    providerCompatibility: PurchaseCheckAnswer,
    modifier: Modifier = Modifier,
) {
    val missingItems = PurchaseCheckProgress.missingManualChecks(
        settingsVerification = settingsVerification,
        carrierUnlocked = carrierUnlocked,
        providerCompatibility = providerCompatibility,
    )
    val allChecksPositive = PurchaseCheckProgress.allChecksPositive(
        androidDetection = result.resultStatus,
        settingsVerification = settingsVerification,
        carrierUnlocked = carrierUnlocked,
        providerCompatibility = providerCompatibility,
    )

    if (missingItems.isEmpty() && !allChecksPositive) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allChecksPositive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            },
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(
                    if (allChecksPositive) {
                        R.string.checks_completed
                    } else {
                        R.string.still_needs_verification
                    },
                ),
                color = if (allChecksPositive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!allChecksPositive) {
                missingItems.forEach { item ->
                    Text(
                        text = stringResource(
                            R.string.bullet_item_format,
                            stringResource(item.labelRes()),
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@StringRes
private fun ManualCheckItem.labelRes(): Int = when (this) {
    ManualCheckItem.SETTINGS_ESIM_OPTION -> R.string.missing_settings_option
    ManualCheckItem.CARRIER_UNLOCK -> R.string.missing_carrier_unlock_status
    ManualCheckItem.PROVIDER_EXACT_MODEL -> R.string.missing_provider_exact_model_support
}

@Composable
private fun PurchaseStepCard(
    number: Int,
    @StringRes titleRes: Int,
    content: @Composable () -> Unit,
) {
    InfoCard(
        title = stringResource(
            R.string.purchase_step_title,
            number,
            stringResource(titleRes),
        ),
        content = content,
    )
}

@Composable
private fun SettingsVerificationChoices(
    selected: ManualVerificationResult,
    onSelected: (ManualVerificationResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ManualVerificationChoice(
            labelRes = R.string.yes,
            selected = selected == ManualVerificationResult.YES,
            onClick = { onSelected(ManualVerificationResult.YES) },
            modifier = Modifier.weight(1f),
        )
        ManualVerificationChoice(
            labelRes = R.string.no,
            selected = selected == ManualVerificationResult.NO,
            onClick = { onSelected(ManualVerificationResult.NO) },
            modifier = Modifier.weight(1f),
        )
        ManualVerificationChoice(
            labelRes = R.string.not_sure,
            selected = selected == ManualVerificationResult.NOT_SURE,
            onClick = { onSelected(ManualVerificationResult.NOT_SURE) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PurchaseAnswerChoices(
    selected: PurchaseCheckAnswer,
    onSelected: (PurchaseCheckAnswer) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            PurchaseCheckAnswer.YES to R.string.yes,
            PurchaseCheckAnswer.NO to R.string.no,
            PurchaseCheckAnswer.NOT_SURE to R.string.not_sure,
        ).forEach { (answer, labelRes) ->
            ManualVerificationChoice(
                labelRes = labelRes,
                selected = selected == answer,
                onClick = { onSelected(answer) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PurchaseSignalLabel(
    signal: PurchaseSignal,
    @StringRes labelRes: Int,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = purchaseSignalContainerColor(signal),
    ) {
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = purchaseSignalContentColor(signal),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PurchaseOutcomeCard(outcome: TravelPurchaseOutcome) {
    val signal = when (outcome) {
        TravelPurchaseOutcome.READY_TO_BUY -> PurchaseSignal.PASS
        TravelPurchaseOutcome.VERIFY_BEFORE_BUYING -> PurchaseSignal.UNCERTAIN
        TravelPurchaseOutcome.DO_NOT_BUY_YET -> PurchaseSignal.FAIL
    }
    val descriptionRes = when (outcome) {
        TravelPurchaseOutcome.READY_TO_BUY -> R.string.ready_to_buy_description
        TravelPurchaseOutcome.VERIFY_BEFORE_BUYING -> R.string.verify_before_buying_description
        TravelPurchaseOutcome.DO_NOT_BUY_YET -> R.string.do_not_buy_yet_description
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = purchaseSignalContainerColor(signal)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(outcome.labelRes()),
                color = purchaseSignalContentColor(signal),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(descriptionRes),
                modifier = Modifier.padding(top = 8.dp),
                color = purchaseSignalContentColor(signal),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (outcome == TravelPurchaseOutcome.READY_TO_BUY) {
                Text(
                    text = stringResource(R.string.compatibility_not_guaranteed),
                    modifier = Modifier.padding(top = 10.dp),
                    color = purchaseSignalContentColor(signal),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PurchaseSummaryCard(
    result: EsimCompatibilityResult,
    settingsVerification: ManualVerificationResult,
    carrierUnlocked: PurchaseCheckAnswer,
    providerCompatibility: PurchaseCheckAnswer,
    outcome: TravelPurchaseOutcome,
) {
    InfoCard(title = stringResource(R.string.travel_purchase_check)) {
        PurchaseSummaryRow(
            labelRes = R.string.summary_device_support,
            signal = result.resultStatus.toPurchaseSignal(),
        )
        PurchaseSummaryRow(
            labelRes = R.string.summary_system_esim_option,
            signal = settingsVerification.toPurchaseSignal(),
        )
        PurchaseSummaryRow(
            labelRes = R.string.summary_carrier_unlocked,
            signal = carrierUnlocked.toPurchaseSignal(),
        )
        PurchaseSummaryRow(
            labelRes = R.string.summary_provider_compatibility,
            signal = providerCompatibility.toPurchaseSignal(),
            isLast = true,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
        InfoRow(
            label = stringResource(R.string.overall_label),
            value = stringResource(outcome.labelRes()),
            valueColor = purchaseSignalColor(
                when (outcome) {
                    TravelPurchaseOutcome.READY_TO_BUY -> PurchaseSignal.PASS
                    TravelPurchaseOutcome.VERIFY_BEFORE_BUYING -> PurchaseSignal.UNCERTAIN
                    TravelPurchaseOutcome.DO_NOT_BUY_YET -> PurchaseSignal.FAIL
                },
            ),
            isLast = true,
        )
    }
}

@Composable
private fun PurchaseSummaryRow(
    @StringRes labelRes: Int,
    signal: PurchaseSignal,
    isLast: Boolean = false,
) {
    val valueDescriptionRes = when (signal) {
        PurchaseSignal.PASS -> R.string.purchase_status_pass
        PurchaseSignal.UNCERTAIN -> R.string.manual_verification_recommended
        PurchaseSignal.FAIL -> R.string.esim_not_detected
    }
    InfoRow(
        label = stringResource(labelRes),
        value = stringResource(
            when (signal) {
                PurchaseSignal.PASS -> R.string.status_symbol_ready
                PurchaseSignal.UNCERTAIN -> R.string.status_symbol_partially_ready
                PurchaseSignal.FAIL -> R.string.status_symbol_not_ready
            },
        ),
        valueColor = purchaseSignalColor(signal),
        valueContentDescription = stringResource(valueDescriptionRes),
        isLast = isLast,
    )
}

private fun EsimResultStatus.toPurchaseSignal(): PurchaseSignal = when (this) {
    EsimResultStatus.READY -> PurchaseSignal.PASS
    EsimResultStatus.PARTIALLY_READY -> PurchaseSignal.UNCERTAIN
    EsimResultStatus.NOT_READY -> PurchaseSignal.FAIL
}

private fun ManualVerificationResult.toPurchaseSignal(): PurchaseSignal = when (this) {
    ManualVerificationResult.YES -> PurchaseSignal.PASS
    ManualVerificationResult.NO -> PurchaseSignal.FAIL
    ManualVerificationResult.NOT_CHECKED,
    ManualVerificationResult.NOT_SURE,
    -> PurchaseSignal.UNCERTAIN
}

private fun PurchaseCheckAnswer.toPurchaseSignal(): PurchaseSignal = when (this) {
    PurchaseCheckAnswer.YES -> PurchaseSignal.PASS
    PurchaseCheckAnswer.NO -> PurchaseSignal.FAIL
    PurchaseCheckAnswer.NOT_CHECKED,
    PurchaseCheckAnswer.NOT_SURE,
    -> PurchaseSignal.UNCERTAIN
}

@Composable
private fun purchaseSignalColor(signal: PurchaseSignal): Color = when (signal) {
    PurchaseSignal.PASS -> MaterialTheme.colorScheme.primary
    PurchaseSignal.UNCERTAIN -> MaterialTheme.colorScheme.tertiary
    PurchaseSignal.FAIL -> MaterialTheme.colorScheme.error
}

@Composable
private fun purchaseSignalContainerColor(signal: PurchaseSignal): Color = when (signal) {
    PurchaseSignal.PASS -> MaterialTheme.colorScheme.primaryContainer
    PurchaseSignal.UNCERTAIN -> MaterialTheme.colorScheme.tertiaryContainer
    PurchaseSignal.FAIL -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun purchaseSignalContentColor(signal: PurchaseSignal): Color = when (signal) {
    PurchaseSignal.PASS -> MaterialTheme.colorScheme.onPrimaryContainer
    PurchaseSignal.UNCERTAIN -> MaterialTheme.colorScheme.onTertiaryContainer
    PurchaseSignal.FAIL -> MaterialTheme.colorScheme.onErrorContainer
}

private fun copyToClipboard(
    context: android.content.Context,
    label: String,
    value: String,
    confirmation: String,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, confirmation, Toast.LENGTH_SHORT).show()
}

@Composable
private fun ReadinessCard(
    result: EsimCompatibilityResult,
    status: StatusContent,
    statusColors: StatusColors,
) {
    InfoCard(title = stringResource(R.string.esim_readiness_section)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(status.titleRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = statusColors.background,
            ) {
                Text(
                    text = stringResource(status.readinessLabelRes),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = statusColors.foreground,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
        InfoRow(
            label = stringResource(R.string.euicc_manager_label),
            value = stringResource(
                if (result.euiccManagerAvailable) R.string.available else R.string.unavailable,
            ),
            valueColor = signalColor(result.euiccManagerAvailable),
            isLast = true,
        )
    }
}

@Composable
private fun BeforeYouBuyCard(result: EsimCompatibilityResult) {
    InfoCard(title = stringResource(R.string.before_you_buy)) {
        ChecklistRow(
            isPositive = result.androidEsimApiAvailable,
            text = stringResource(
                if (result.androidEsimApiAvailable) {
                    R.string.check_android_support
                } else {
                    R.string.check_android_support_unavailable
                },
            ),
        )
        ChecklistRow(
            isPositive = result.euiccEnabled,
            text = stringResource(
                if (result.euiccEnabled) {
                    R.string.check_service_available
                } else {
                    R.string.check_service_unavailable
                },
            ),
        )
        ChecklistRow(
            isPositive = null,
            text = stringResource(R.string.carrier_unlock_provider_check),
        )
    }
}

@Composable
private fun ChecklistRow(
    isPositive: Boolean?,
    text: String,
) {
    val symbolDescription = stringResource(
        when (isPositive) {
            true -> R.string.esim_supported
            false -> R.string.esim_not_detected
            null -> R.string.manual_verification_recommended
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(
                when (isPositive) {
                    true -> R.string.status_symbol_ready
                    false -> R.string.status_symbol_not_ready
                    null -> R.string.status_symbol_warning
                },
            ),
            modifier = Modifier.semantics {
                contentDescription = symbolDescription
            },
            color = when (isPositive) {
                true -> signalColor(true)
                false -> signalColor(false)
                null -> MaterialTheme.colorScheme.tertiary
            },
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LanguageCard() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val options = languageOptions()
    val applicationLocales = AppCompatDelegate.getApplicationLocales()
    val rawSelectedTag = if (applicationLocales.isEmpty) {
        null
    } else {
        applicationLocales[0]?.toLanguageTag()
    }
    val selectedTag = rawSelectedTag?.let(AppLocaleTags::canonicalize)
    val selectedOption = options.firstOrNull { it.languageTag == selectedTag } ?: options.first()

    LaunchedEffect(rawSelectedTag, selectedTag) {
        if (rawSelectedTag != null && selectedTag != null && rawSelectedTag != selectedTag) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(selectedTag),
            )
        }
    }

    InfoCard(title = stringResource(R.string.language_section)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(selectedOption.labelRes),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            expanded = false
                            val locales = option.languageTag?.let {
                                LocaleListCompat.forLanguageTags(it)
                            } ?: LocaleListCompat.getEmptyLocaleList()
                            AppCompatDelegate.setApplicationLocales(locales)
                        },
                    )
                }
            }
        }
    }
}

private fun languageOptions(): List<LanguageOption> = listOf(
    LanguageOption(languageTag = null, labelRes = R.string.system_default),
    LanguageOption(languageTag = "en", labelRes = R.string.language_name_english),
    LanguageOption(languageTag = "ar", labelRes = R.string.language_name_arabic),
    LanguageOption(languageTag = "zh-CN", labelRes = R.string.language_name_simplified_chinese),
    LanguageOption(languageTag = "zh-TW", labelRes = R.string.language_name_traditional_chinese),
    LanguageOption(languageTag = "fr", labelRes = R.string.language_name_french),
    LanguageOption(languageTag = "de", labelRes = R.string.language_name_german),
    LanguageOption(languageTag = "hi", labelRes = R.string.language_name_hindi),
    LanguageOption(languageTag = "id", labelRes = R.string.language_name_indonesian),
    LanguageOption(languageTag = "it", labelRes = R.string.language_name_italian),
    LanguageOption(languageTag = "ja", labelRes = R.string.language_name_japanese),
    LanguageOption(languageTag = "ko", labelRes = R.string.language_name_korean),
    LanguageOption(languageTag = "pt-BR", labelRes = R.string.language_name_portuguese),
    LanguageOption(languageTag = "ru", labelRes = R.string.language_name_russian),
    LanguageOption(languageTag = "es-ES", labelRes = R.string.language_name_spanish),
    LanguageOption(languageTag = "tr", labelRes = R.string.language_name_turkish),
)

@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueContentDescription: String? = null,
    isLast: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (valueContentDescription == null) {
                        Modifier
                    } else {
                        Modifier.semantics {
                            contentDescription = valueContentDescription
                        }
                    },
                ),
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            softWrap = true,
        )
    }
}

@Composable
private fun AboutScreen(
    onBack: () -> Unit,
    onShowPrivacy: () -> Unit,
    onShowInstructions: () -> Unit,
    onSendFeedback: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.about_description),
                modifier = Modifier.padding(top = 18.dp, bottom = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedButton(
                onClick = onShowPrivacy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.privacy_policy))
            }
            OutlinedButton(
                onClick = onShowInstructions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.how_to_add_esim))
            }
            OutlinedButton(
                onClick = onSendFeedback,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.send_feedback))
            }
        }
    }
}

@Composable
private fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.privacy_policy),
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            PrivacyParagraph(R.string.privacy_local_checks)
            PrivacyParagraph(R.string.privacy_no_collection)
            PrivacyParagraph(R.string.privacy_local_device_data)
            PrivacyParagraph(R.string.privacy_no_account)
            PrivacyParagraph(R.string.privacy_no_backend)
            PrivacyParagraph(R.string.privacy_no_ads_analytics)
            PrivacyParagraph(R.string.privacy_no_guarantee)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.contact_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.contact_email),
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyParagraph(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.padding(bottom = 14.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun HowToAddEsimScreen(onBack: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.how_to_add_esim),
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.how_to_intro),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )

            InstructionStep(number = 1, textRes = R.string.instruction_open_settings)
            InstructionStep(
                number = 2,
                textRes = R.string.instruction_open_network,
            )
            InstructionStep(
                number = 3,
                textRes = R.string.instruction_look_for_add,
            )
            InstructionStep(
                number = 4,
                textRes = R.string.instruction_scan_qr,
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Text(
                    text = stringResource(R.string.menu_names_vary),
                    modifier = Modifier.padding(18.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun InstructionStep(
    number: Int,
    @StringRes textRes: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.instruction_step_number, number),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = stringResource(textRes),
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, top = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun statusContent(status: EsimResultStatus): StatusContent = when (status) {
    EsimResultStatus.READY -> StatusContent(
        titleRes = status.labelRes(),
        descriptionRes = R.string.status_description_ready,
        symbolRes = R.string.status_symbol_ready,
        symbolDescriptionRes = R.string.esim_supported,
        readinessLabelRes = R.string.readiness_label_ready,
    )

    EsimResultStatus.PARTIALLY_READY -> StatusContent(
        titleRes = status.labelRes(),
        descriptionRes = R.string.status_description_partially_ready,
        symbolRes = R.string.status_symbol_partially_ready,
        symbolDescriptionRes = R.string.esim_support_uncertain,
        readinessLabelRes = R.string.readiness_label_needs_verification,
    )

    EsimResultStatus.NOT_READY -> StatusContent(
        titleRes = status.labelRes(),
        descriptionRes = R.string.status_description_not_ready,
        symbolRes = R.string.status_symbol_not_ready,
        symbolDescriptionRes = R.string.esim_not_detected,
        readinessLabelRes = R.string.readiness_label_not_detected,
    )
}

private fun settingsVerificationContent(
    status: EsimResultStatus,
): SettingsVerificationContent = when (status) {
    EsimResultStatus.READY -> SettingsVerificationContent(
        titleRes = R.string.verify_in_settings,
        descriptionRes = R.string.verify_in_settings_description,
        buttonRes = R.string.open_esim_settings,
    )

    EsimResultStatus.PARTIALLY_READY -> SettingsVerificationContent(
        titleRes = R.string.manual_verification_recommended,
        descriptionRes = R.string.manual_verification_recommended_description,
        buttonRes = R.string.open_esim_settings,
    )

    EsimResultStatus.NOT_READY -> SettingsVerificationContent(
        titleRes = R.string.one_more_check,
        descriptionRes = R.string.one_more_check_description,
        buttonRes = R.string.open_sim_settings,
    )
}

@Composable
private fun statusColors(status: EsimResultStatus): StatusColors = when (status) {
    EsimResultStatus.READY -> StatusColors(
        foreground = MaterialTheme.colorScheme.onPrimaryContainer,
        background = MaterialTheme.colorScheme.primaryContainer,
    )

    EsimResultStatus.PARTIALLY_READY -> StatusColors(
        foreground = MaterialTheme.colorScheme.onTertiaryContainer,
        background = MaterialTheme.colorScheme.tertiaryContainer,
    )

    EsimResultStatus.NOT_READY -> StatusColors(
        foreground = MaterialTheme.colorScheme.onErrorContainer,
        background = MaterialTheme.colorScheme.errorContainer,
    )
}

@Composable
private fun signalColor(isPositive: Boolean): Color = if (isPositive) {
    MaterialTheme.colorScheme.primary
} else {
    MaterialTheme.colorScheme.error
}

@Composable
private fun deviceDisplayName(result: EsimCompatibilityResult): String {
    val context = LocalContext.current
    return if (result.deviceModel.startsWith(result.deviceManufacturer, ignoreCase = true)) {
        context.bidiIsolate(result.deviceModel)
    } else {
        stringResource(
            R.string.device_display_name,
            context.bidiIsolate(result.deviceManufacturer),
            context.bidiIsolate(result.deviceModel),
        )
    }
}
