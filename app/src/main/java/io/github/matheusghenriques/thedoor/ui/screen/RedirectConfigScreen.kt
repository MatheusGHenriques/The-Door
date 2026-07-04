package io.github.matheusghenriques.thedoor.ui.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.matheusghenriques.thedoor.R
import io.github.matheusghenriques.thedoor.data.RedirectType
import io.github.matheusghenriques.thedoor.ui.viewmodel.RedirectConfigViewModel

@Composable
fun RedirectConfigScreen(onBack: () -> Unit, onComplete: () -> Unit) {
    val vm: RedirectConfigViewModel = viewModel()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.redirect_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.redirect_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            var showAppDialog by remember { mutableStateOf(false) }

            RedirectOptionTile(
                title = stringResource(R.string.redirect_tile_door_title),
                subtitle = stringResource(R.string.redirect_tile_door_subtitle),
                selected = vm.redirectType == RedirectType.THE_DOOR,
                onClick = { vm.redirectType = RedirectType.THE_DOOR },
                bottomContent = if (vm.redirectType == RedirectType.THE_DOOR) {
                    {
                        OutlinedTextField(
                            value = vm.theDoorPhrase,
                            onValueChange = { vm.theDoorPhrase = it },
                            label = { Text(stringResource(R.string.redirect_phrase_label)) },
                            placeholder = { Text(stringResource(R.string.redirect_phrase_hint)) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 2
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.redirect_phrase_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else null)

            Spacer(Modifier.height(12.dp))

            RedirectOptionTile(
                title = stringResource(R.string.redirect_tile_app_title),
                subtitle = stringResource(R.string.redirect_tile_app_subtitle),
                selected = vm.redirectType == RedirectType.APP,
                onClick = {
                    vm.redirectType = RedirectType.APP
                    if (vm.availableApps.isNotEmpty()) showAppDialog = true
                },
                trailing = if (vm.redirectType == RedirectType.APP) {
                    {
                        val app = vm.selectedApp
                        if (app != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(drawable = app.icon, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.redirect_select_action),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else null)

            Spacer(Modifier.height(12.dp))

            RedirectOptionTile(
                title = stringResource(R.string.redirect_tile_none_title),
                subtitle = stringResource(R.string.redirect_tile_none_subtitle),
                selected = vm.redirectType == RedirectType.NONE,
                onClick = { vm.redirectType = RedirectType.NONE })

            if (showAppDialog) {
                var searchQuery by remember { mutableStateOf("") }

                val filteredApps = remember(searchQuery, vm.availableApps, vm.selectedAppPackage) {
                    if (searchQuery.isBlank()) vm.availableApps
                    else vm.availableApps.filter { app ->
                        app.appName.lowercase()
                            .contains(searchQuery.lowercase()) || app.packageName.lowercase()
                            .contains(searchQuery.lowercase())
                    }
                }

                Dialog(onDismissRequest = { showAppDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.redirect_dialog_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { showAppDialog = false }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.btn_fechar)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.redirect_dialog_search_hint)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search, contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                singleLine = true
                            )

                            if (filteredApps.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.redirect_dialog_empty),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp, vertical = 24.dp
                                    )
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false),
                                    contentPadding = PaddingValues(
                                        horizontal = 8.dp, vertical = 4.dp
                                    )
                                ) {
                                    items(filteredApps, key = { it.packageName }) { app ->
                                        val isSelected = app.packageName == vm.selectedAppPackage
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    vm.selectApp(app.packageName)
                                                    showAppDialog = false
                                                }
                                                .padding(vertical = 10.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            AppIcon(
                                                drawable = app.icon, modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(Modifier.size(12.dp))
                                            Text(
                                                text = app.appName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.redirect_toast_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.redirect_toast_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = vm.toastEnabled,
                            onCheckedChange = { vm.toastEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                                checkedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                        )
                    }

                    if (vm.toastEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = vm.toastMessage,
                                onValueChange = { vm.toastMessage = it },
                                label = { Text(stringResource(R.string.redirect_toast_label)) },
                                placeholder = { Text(stringResource(R.string.redirect_toast_hint)) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            val message = vm.toastMessage.ifBlank {
                                stringResource(R.string.redirect_toast_hint)
                            }
                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_testar),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        val isNextEnabled by remember {
            derivedStateOf {
                val appInvalid =
                    vm.redirectType == RedirectType.APP && vm.selectedAppPackage.isEmpty()
                val toastInvalid = vm.toastEnabled && vm.toastMessage.isBlank()
                !(appInvalid || toastInvalid)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedButton(
                onClick = onBack, modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.btn_voltar))
            }
            Spacer(Modifier.width(12.dp))
            TimedButton(
                text = stringResource(R.string.btn_proximo),
                onClick = { vm.save(onComplete) },
                modifier = Modifier.weight(1f),
                enabled = isNextEnabled
            )
        }
    }
}

@Composable
private fun RedirectOptionTile(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.size(8.dp))
                    trailing()
                }
            }
            if (bottomContent != null) {
                Spacer(Modifier.size(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.size(12.dp))
                bottomContent()
            }
        }
    }
}
