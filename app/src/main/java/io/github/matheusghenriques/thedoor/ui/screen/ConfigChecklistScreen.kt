package io.github.matheusghenriques.thedoor.ui.screen

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.matheusghenriques.thedoor.R
import io.github.matheusghenriques.thedoor.TheDoorAdminReceiver
import io.github.matheusghenriques.thedoor.data.PackageConstants

@Composable
fun ConfigChecklistScreen(
    blockAdultContent: Boolean, blockSocial: Boolean, onBack: () -> Unit, onComplete: () -> Unit
) {
    val context = LocalContext.current

    var accessibilityOn by remember { mutableStateOf(isAccessibilityEnabled(context)) }
    var adminOn by remember { mutableStateOf(isDeviceAdminActive(context)) }
    var notificationsOn by remember { mutableStateOf(areNotificationsEnabled(context)) }
    var usbDebugOff by remember { mutableStateOf(isUsbDebuggingDisabled(context)) }
    var extInstallOff by remember { mutableStateOf(isExternalInstallDisabled(context)) }
    var chromeRemoved by remember {
        mutableStateOf(
            !isPackageInstalled(
                context, PackageConstants.CHROME
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
    var rethinkInstalled by remember {
        mutableStateOf(
            isPackageInstalled(
                context, PackageConstants.RETHINK_DNS
            )
        )
    }

    var ublockDone by remember { mutableStateOf(false) }
    var blocknsfwDone by remember { mutableStateOf(false) }
    var rethinkConfigured by remember { mutableStateOf(false) }
    var searchDomainsBlocked by remember { mutableStateOf(false) }
    var firefoxSearchConfigured by remember { mutableStateOf(false) }
    var timeLimitsDone by remember { mutableStateOf(false) }
    var socialDnsDone by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityOn = isAccessibilityEnabled(context)
                adminOn = isDeviceAdminActive(context)
                notificationsOn = areNotificationsEnabled(context)
                usbDebugOff = isUsbDebuggingDisabled(context)
                extInstallOff = isExternalInstallDisabled(context)
                chromeRemoved = !isPackageInstalled(context, PackageConstants.CHROME)
                firefoxInstalled = isPackageInstalled(context, PackageConstants.FIREFOX)
                rethinkInstalled = isPackageInstalled(context, PackageConstants.RETHINK_DNS)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.checklist_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.checklist_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.checklist_section_permissions))

            AutoItem(
                isDone = accessibilityOn,
                title = stringResource(R.string.checklist_item_accessibility),
                description = stringResource(R.string.checklist_item_accessibility_desc),
                actionLabel = stringResource(R.string.btn_abrir),
                onAction = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                })
            Spacer(Modifier.height(8.dp))
            AutoItem(
                isDone = adminOn,
                title = stringResource(R.string.checklist_item_admin),
                description = stringResource(R.string.checklist_item_admin_desc),
                actionLabel = stringResource(R.string.btn_abrir),
                onAction = {
                    context.startActivity(
                        Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                })
            Spacer(Modifier.height(8.dp))
            AutoItem(
                isDone = notificationsOn,
                title = stringResource(R.string.checklist_item_notifications),
                description = stringResource(R.string.checklist_item_notifications_desc),
                actionLabel = stringResource(R.string.btn_abrir),
                onAction = { openAppSettings(context, context.packageName) })

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            SectionHeader(stringResource(R.string.checklist_section_system))

            AutoItem(
                isDone = usbDebugOff,
                title = stringResource(R.string.checklist_item_usb_debug),
                description = stringResource(R.string.checklist_item_usb_debug_desc),
                actionLabel = stringResource(R.string.btn_abrir),
                onAction = {
                    openSettings(
                        context, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                    )
                })
            Spacer(Modifier.height(8.dp))
            AutoItem(
                isDone = extInstallOff,
                title = stringResource(R.string.checklist_item_ext_install),
                description = stringResource(R.string.checklist_item_ext_install_desc),
                actionLabel = stringResource(R.string.btn_abrir),
                onAction = { openUnknownSources(context) })

            if (blockAdultContent) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                SectionHeader(stringResource(R.string.checklist_section_browser))

                AutoItem(
                    isDone = chromeRemoved,
                    title = stringResource(R.string.checklist_item_chrome),
                    description = stringResource(R.string.checklist_item_chrome_desc),
                    actionLabel = stringResource(R.string.btn_abrir),
                    onAction = { openAppSettings(context, PackageConstants.CHROME) })
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
                    title = stringResource(R.string.checklist_item_ublock),
                    description = stringResource(R.string.checklist_item_ublock_desc)
                )
                Spacer(Modifier.height(8.dp))
                ManualItem(
                    isDone = blocknsfwDone,
                    onToggle = { blocknsfwDone = it },
                    title = stringResource(R.string.checklist_item_blocknsfw),
                    description = stringResource(R.string.checklist_item_blocknsfw_desc)
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                SectionHeader(stringResource(R.string.checklist_section_dns_adult))

                ManualItem(
                    isDone = searchDomainsBlocked,
                    onToggle = { searchDomainsBlocked = it },
                    title = stringResource(R.string.checklist_item_search_blocks),
                    description = stringResource(R.string.checklist_item_search_blocks_desc)
                )
                Spacer(Modifier.height(8.dp))
                ManualItem(
                    isDone = firefoxSearchConfigured,
                    onToggle = { firefoxSearchConfigured = it },
                    title = stringResource(R.string.checklist_item_safe_default),
                    description = stringResource(R.string.checklist_item_safe_default_desc)
                )
            }

            if (blockAdultContent || blockSocial) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                SectionHeader(stringResource(R.string.checklist_section_rethink))

                AutoItem(
                    isDone = rethinkInstalled,
                    title = stringResource(R.string.checklist_item_rethink_installed),
                    description = stringResource(R.string.checklist_item_rethink_installed_desc),
                    actionLabel = stringResource(R.string.btn_instalar),
                    onAction = { openPlayStore(context, PackageConstants.RETHINK_DNS) })
                Spacer(Modifier.height(8.dp))
                ManualItem(
                    isDone = rethinkConfigured,
                    onToggle = { rethinkConfigured = it },
                    title = stringResource(R.string.checklist_item_rethink_configured),
                    description = stringResource(R.string.checklist_item_rethink_configured_desc)
                )
            }

            if (blockSocial) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                SectionHeader(stringResource(R.string.checklist_section_social))

                ManualItem(
                    isDone = timeLimitsDone,
                    onToggle = { timeLimitsDone = it },
                    title = stringResource(R.string.checklist_item_time_limits),
                    description = stringResource(R.string.checklist_item_time_limits_desc)
                )
                Spacer(Modifier.height(8.dp))
                ManualItem(
                    isDone = socialDnsDone,
                    onToggle = { socialDnsDone = it },
                    title = stringResource(R.string.checklist_item_social_dns),
                    description = stringResource(R.string.checklist_item_social_dns_desc)
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onBack, modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.btn_voltar))
            }
            Spacer(Modifier.width(12.dp))
            TimedButton(
                text = stringResource(R.string.checklist_btn_finalizar),
                onClick = onComplete,
                modifier = Modifier.weight(1f)
            )
        }
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
                    contentDescription = stringResource(R.string.checklist_icon_done),
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
    isDone: Boolean, onToggle: (Boolean) -> Unit, title: String, description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = { onToggle(!isDone) }) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.checklist_icon_done),
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
        }
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val service =
        "${context.packageName}/io.github.matheusghenriques.thedoor.TheDoorAccessibilityService"
    val enabled = try {
        Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
    } catch (_: SecurityException) {
        ""
    }
    return enabled.split(':').any { it.trim().equals(service, ignoreCase = true) }
}

private fun isDeviceAdminActive(context: Context): Boolean {
    return try {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val component = ComponentName(context, TheDoorAdminReceiver::class.java)
        dpm.isAdminActive(component)
    } catch (_: Exception) {
        false
    }
}

private fun areNotificationsEnabled(context: Context): Boolean {
    return try {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    } catch (_: Exception) {
        false
    }
}

private fun isUsbDebuggingDisabled(context: Context): Boolean {
    return try {
        Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 0
    } catch (_: SecurityException) {
        false
    }
}

private fun isExternalInstallDisabled(context: Context): Boolean {
    return try {
        !context.packageManager.canRequestPackageInstalls()
    } catch (_: Exception) {
        true
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

private fun openSettings(context: Context, action: String) {
    context.startActivity(Intent(action).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun openAppSettings(context: Context, packageName: String) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:$packageName".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
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

private fun openUnknownSources(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (_: Exception) {
        openSettings(context, Settings.ACTION_SECURITY_SETTINGS)
    }
}
