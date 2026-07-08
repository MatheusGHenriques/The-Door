package io.github.matheusghenriques.thedoor.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.matheusghenriques.thedoor.R
import io.github.matheusghenriques.thedoor.data.PackageConstants
import io.github.matheusghenriques.thedoor.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel, onToggleLanguage: () -> Unit, onStartAppLimits: () -> Unit
) {
    BackHandler(enabled = viewModel.currentPage > 0) {
        viewModel.previousPage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = viewModel.currentPage, transitionSpec = {
                (if (targetState > initialState) {
                    slideInHorizontally(tween(300)) { width -> width } togetherWith slideOutHorizontally(
                        tween(300)
                    ) { width -> -width }
                } else {
                    slideInHorizontally(tween(300)) { width -> -width } togetherWith slideOutHorizontally(
                        tween(300)
                    ) { width -> width }
                }).using(SizeTransform(clip = false))
            }) { page ->
            when (page) {
                0 -> WelcomePage(viewModel)
                1 -> SelectionPage(viewModel)
                2 -> OnboardingChecklistPage(viewModel)
                3 -> FinalPage(onStartAppLimits)
            }
        }

        PageIndicator(
            currentPage = viewModel.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )

        if (viewModel.currentPage == 0) {
            TextButton(
                onClick = onToggleLanguage,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (LocalLocale.current.platformLocale.language == "pt") "EN" else "PT-BR",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_quote),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TimedButton(
            text = stringResource(R.string.btn_proximo),
            onClick = { viewModel.nextPage() },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SelectionPage(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_selection_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboarding_selection_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            SelectionCard(
                title = stringResource(R.string.onboarding_social_title),
                description = stringResource(R.string.onboarding_social_desc),
                selected = viewModel.blockSocial,
                onClick = { viewModel.toggleSocial() })

            Spacer(modifier = Modifier.height(16.dp))

            SelectionCard(
                title = stringResource(R.string.onboarding_adult_title),
                description = stringResource(R.string.onboarding_adult_desc),
                selected = viewModel.blockAdultContent,
                onClick = { viewModel.toggleAdultContent() })

            Spacer(modifier = Modifier.height(16.dp))

            SelectionCard(
                title = stringResource(R.string.onboarding_gambling_title),
                description = stringResource(R.string.onboarding_gambling_desc),
                selected = viewModel.blockGambling,
                onClick = { viewModel.toggleGambling() })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = { viewModel.previousPage() }) {
                Text(stringResource(R.string.btn_voltar))
            }

            TimedButton(
                text = stringResource(R.string.btn_continuar),
                onClick = { viewModel.nextPage() },
                enabled = viewModel.canContinue
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SelectionCard(
    title: String, description: String, selected: Boolean, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun OnboardingChecklistPage(viewModel: OnboardingViewModel) {
    val context = LocalContext.current

    var rethinkInstalled by remember {
        mutableStateOf(
            isPackageInstalled(
                context, PackageConstants.RETHINK_DNS
            )
        )
    }
    var firefoxInstalled by remember {
        mutableStateOf(
            isPackageInstalled(
                context, PackageConstants.FIREFOX
            )
        )
    }
    var chromeRemoved by remember {
        mutableStateOf(
            !isPackageInstalled(
                context, PackageConstants.CHROME
            )
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                rethinkInstalled = isPackageInstalled(context, PackageConstants.RETHINK_DNS)
                firefoxInstalled = isPackageInstalled(context, PackageConstants.FIREFOX)
                chromeRemoved = !isPackageInstalled(context, PackageConstants.CHROME)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dnsFilterDone = remember { mutableStateOf(false) }
    val vpnActivated = remember { mutableStateOf(false) }
    val vpnAlwaysOn = remember { mutableStateOf(false) }
    val newAppsBlocked = remember { mutableStateOf(false) }
    var ublockDone by remember { mutableStateOf(false) }
    var blocknsfwDone by remember { mutableStateOf(false) }
    var safeSearchDone by remember { mutableStateOf(false) }
    var gamblingDone by remember { mutableStateOf(false) }
    var timeLimitsDone by remember { mutableStateOf(false) }
    var gradualDone by remember { mutableStateOf(false) }
    val socialDnsDone = remember { mutableStateOf(false) }
    var bankingIsolationDone by remember { mutableStateOf(false) }

    val adultOrGambling = viewModel.blockAdultContent || viewModel.blockGambling
    val dnsBadge = if (adultOrGambling) stringResource(R.string.onboarding_badge_required)
    else stringResource(R.string.onboarding_badge_additional)
    val additionalBadge = stringResource(R.string.onboarding_badge_additional)

    @Composable
    fun DnsItems(showDnsFilter: Boolean) {
        AutoItem(
            isDone = rethinkInstalled,
            title = stringResource(R.string.checklist_item_rethink_installed),
            description = stringResource(R.string.checklist_item_rethink_installed_desc),
            actionLabel = stringResource(R.string.btn_instalar),
            onAction = { openPlayStore(context, PackageConstants.RETHINK_DNS) })
        if (showDnsFilter) {
            Spacer(Modifier.height(8.dp))
            ManualItem(
                isDone = dnsFilterDone.value,
                onToggle = { dnsFilterDone.value = it },
                title = stringResource(R.string.onboarding_dns_filter),
                description = stringResource(R.string.onboarding_dns_filter_desc),
                actionLabel = stringResource(R.string.btn_abrir),
                onAction = { openApp(context, PackageConstants.RETHINK_DNS) })
        }
        Spacer(Modifier.height(8.dp))
        ManualItem(
            isDone = vpnActivated.value,
            onToggle = { vpnActivated.value = it },
            title = stringResource(R.string.onboarding_vpn_activate),
            description = stringResource(R.string.onboarding_vpn_activate_desc),
            actionLabel = stringResource(R.string.btn_abrir),
            onAction = { openApp(context, PackageConstants.RETHINK_DNS) })
        Spacer(Modifier.height(8.dp))
        ManualItem(
            isDone = vpnAlwaysOn.value,
            onToggle = { vpnAlwaysOn.value = it },
            title = stringResource(R.string.onboarding_vpn_always),
            description = stringResource(R.string.onboarding_vpn_always_desc),
            actionLabel = stringResource(R.string.btn_abrir),
            onAction = { openVpnSettings(context) })
        Spacer(Modifier.height(8.dp))
        ManualItem(
            isDone = newAppsBlocked.value,
            onToggle = { newAppsBlocked.value = it },
            title = stringResource(R.string.onboarding_new_apps),
            description = stringResource(R.string.onboarding_new_apps_desc),
            badge = if (adultOrGambling) additionalBadge else null,
            actionLabel = stringResource(R.string.btn_abrir),
            onAction = { openApp(context, PackageConstants.RETHINK_DNS) })
        if (viewModel.blockSocial) {
            Spacer(Modifier.height(8.dp))
            ManualItem(
                isDone = socialDnsDone.value,
                onToggle = { socialDnsDone.value = it },
                title = stringResource(R.string.onboarding_social_dns),
                description = stringResource(R.string.onboarding_social_dns_desc),
                badge = if (adultOrGambling) additionalBadge else null,
                actionLabel = stringResource(R.string.btn_abrir),
                onAction = { openApp(context, PackageConstants.RETHINK_DNS) })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_guide_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.onboarding_guide_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            ChecklistSectionHeader(
                title = stringResource(R.string.guide_section_banking),
                badge = stringResource(R.string.onboarding_badge_required),
                isRequired = true
            )
            ManualItem(
                isDone = bankingIsolationDone,
                onToggle = { bankingIsolationDone = it },
                title = stringResource(R.string.onboarding_banking_warning),
                description = stringResource(R.string.onboarding_banking_warning_desc),
            )

            Spacer(Modifier.height(16.dp))

            if (adultOrGambling) {
                ChecklistSectionHeader(
                    title = stringResource(R.string.guide_section_dns),
                    badge = dnsBadge,
                    isRequired = true
                )
                DnsItems(showDnsFilter = true)
            }

            if (viewModel.blockGambling) {
                Spacer(Modifier.height(16.dp))

                ChecklistSectionHeader(
                    title = stringResource(R.string.guide_section_gambling),
                    badge = stringResource(R.string.onboarding_badge_required),
                    isRequired = true
                )
                ManualItem(
                    isDone = gamblingDone,
                    onToggle = { gamblingDone = it },
                    title = stringResource(R.string.onboarding_gambling),
                    description = stringResource(R.string.onboarding_gambling_block_desc),
                    actionLabel = stringResource(R.string.btn_abrir),
                    onAction = { openApp(context, PackageConstants.RETHINK_DNS) })
            }

            if (viewModel.blockSocial) {
                Spacer(Modifier.height(16.dp))

                ChecklistSectionHeader(
                    title = stringResource(R.string.guide_section_limits),
                    badge = stringResource(R.string.onboarding_badge_required),
                    isRequired = true
                )
                ManualItem(
                    isDone = timeLimitsDone,
                    onToggle = { timeLimitsDone = it },
                    title = stringResource(R.string.onboarding_time_limits),
                    description = stringResource(R.string.onboarding_time_limits_desc)
                )
                Spacer(Modifier.height(8.dp))
                ManualItem(
                    isDone = gradualDone,
                    onToggle = { gradualDone = it },
                    title = stringResource(R.string.onboarding_gradual_reduction),
                    description = stringResource(R.string.onboarding_gradual_reduction_desc),
                    badge = additionalBadge
                )
            }

            if (!adultOrGambling) {
                Spacer(Modifier.height(16.dp))

                ChecklistSectionHeader(
                    title = stringResource(R.string.guide_section_dns),
                    badge = dnsBadge,
                    isRequired = false
                )
                DnsItems(showDnsFilter = false)
            }

            if (viewModel.blockAdultContent) {
                Spacer(Modifier.height(16.dp))

                ChecklistSectionHeader(
                    title = stringResource(R.string.guide_section_browser),
                    badge = additionalBadge,
                    isRequired = false
                )
                AutoItem(
                    isDone = chromeRemoved,
                    title = stringResource(R.string.checklist_item_chrome),
                    description = stringResource(R.string.checklist_item_chrome_desc),
                    actionLabel = stringResource(R.string.btn_abrir),
                    onAction = { openAppSettings(context) })
                Spacer(Modifier.height(8.dp))
                AutoItem(
                    isDone = firefoxInstalled,
                    title = stringResource(R.string.checklist_item_firefox),
                    description = stringResource(R.string.checklist_item_firefox_desc),
                    actionLabel = stringResource(R.string.btn_instalar),
                    onAction = { openPlayStore(context, PackageConstants.FIREFOX) })
                Spacer(Modifier.height(8.dp))
                ManualItem(
                    isDone = ublockDone,
                    onToggle = { ublockDone = it },
                    title = stringResource(R.string.onboarding_ublock),
                    description = stringResource(R.string.onboarding_ublock_desc),
                    actionLabel = stringResource(R.string.btn_abrir),
                    onAction = { openApp(context, PackageConstants.FIREFOX) })
                Spacer(Modifier.height(8.dp))
                ManualItem(
                    isDone = blocknsfwDone,
                    onToggle = { blocknsfwDone = it },
                    title = stringResource(R.string.onboarding_blocknsfw),
                    description = stringResource(R.string.onboarding_blocknsfw_desc),
                    actionLabel = stringResource(R.string.btn_abrir),
                    onAction = { openApp(context, PackageConstants.FIREFOX) })
                Spacer(Modifier.height(8.dp))
                ManualItem(
                    isDone = safeSearchDone,
                    onToggle = { safeSearchDone = it },
                    title = stringResource(R.string.onboarding_safe_search),
                    description = stringResource(R.string.onboarding_safe_search_desc),
                    actionLabel = stringResource(R.string.btn_abrir),
                    onAction = { openApp(context, PackageConstants.FIREFOX) })
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_reminder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = { viewModel.previousPage() }) {
                Text(stringResource(R.string.btn_voltar))
            }

            TimedButton(
                text = stringResource(R.string.btn_proximo), onClick = { viewModel.nextPage() })
        }

        Spacer(Modifier.height(40.dp))
    }
}


@Composable
private fun ChecklistSectionHeader(title: String, badge: String, isRequired: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        BadgeChip(text = badge, isRequired = isRequired)
    }
}

@Composable
private fun BadgeChip(text: String, isRequired: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp), color = if (isRequired) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface, border = if (isRequired) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isRequired) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AutoItem(
    isDone: Boolean,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.size(8.dp))
                OutlinedButton(
                    onClick = onAction, contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ManualItem(
    isDone: Boolean,
    onToggle: (Boolean) -> Unit,
    title: String,
    description: String,
    badge: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isDone) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badge != null) {
                        Spacer(Modifier.width(6.dp))
                        BadgeChip(text = badge, isRequired = false)
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.size(8.dp))
                OutlinedButton(
                    onClick = onAction, contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }
}

private fun openPlayStore(context: Context, packageName: String) {
    val marketIntent = Intent(Intent.ACTION_VIEW).apply {
        data = "market://details?id=$packageName".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (marketIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(marketIntent)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = "https://play.google.com/store/apps/details?id=$packageName".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

private fun openAppSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${PackageConstants.CHROME}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun openApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } else {
        openPlayStore(context, packageName)
    }
}

private fun openVpnSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

@Composable
private fun FinalPage(
    onStartAppLimits: () -> Unit
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_final_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_final_warning),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(96.dp))

            Text(
                text = stringResource(R.string.onboarding_final_stat),
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboarding_final_stat_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_final_question),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !permissionGranted
            ) {
                Text(
                    if (permissionGranted) stringResource(R.string.onboarding_notif_allowed)
                    else stringResource(R.string.onboarding_final_notif_btn)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        TimedButton(
            text = stringResource(R.string.onboarding_final_limits_btn),
            onClick = onStartAppLimits,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun PageIndicator(
    currentPage: Int, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier, horizontalArrangement = Arrangement.Center
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (index == currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    )
            )
            if (index < 3) Spacer(modifier = Modifier.width(6.dp))
        }
    }
}
