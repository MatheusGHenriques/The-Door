package io.github.matheusghenriques.thedoor.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.matheusghenriques.thedoor.R

@Composable
fun ZeroTrustConfirmScreen(onBack: () -> Unit, onComplete: () -> Unit) {
    var understood by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.zero_trust_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            WarningBlock(
                title = stringResource(R.string.zero_trust_warning1_title),
                text = stringResource(R.string.zero_trust_warning1_text)
            )

            Spacer(Modifier.height(16.dp))

            WarningBlock(
                title = stringResource(R.string.zero_trust_warning2_title),
                text = stringResource(R.string.zero_trust_warning2_text)
            )

            Spacer(Modifier.height(16.dp))

            WarningBlock(
                title = stringResource(R.string.zero_trust_warning3_title),
                text = stringResource(R.string.zero_trust_warning3_text)
            )

            Spacer(Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = understood,
                    onCheckedChange = { understood = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        checkmarkColor = MaterialTheme.colorScheme.surface
                    )
                )
                Text(
                    text = stringResource(R.string.zero_trust_checkbox),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(16.dp))

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
                text = stringResource(R.string.zero_trust_btn_activate),
                onClick = onComplete,
                modifier = Modifier.weight(1f),
                enabled = understood
            )
        }
    }
}


@Composable
private fun WarningBlock(title: String, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
