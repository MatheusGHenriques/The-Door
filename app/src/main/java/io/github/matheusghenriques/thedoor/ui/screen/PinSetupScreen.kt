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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.matheusghenriques.thedoor.R

@Composable
fun PinSetupScreen(
    onBack: () -> Unit, onComplete: (String) -> Unit, onKeepPin: (() -> Unit)? = null
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val errorDigits = stringResource(R.string.pin_error_digits)
    val errorMismatch = stringResource(R.string.pin_error_mismatch)

    val isValid =
        pin.length == 6 && pin.all { it.isDigit() } && confirmPin.length == 6 && confirmPin.all { it.isDigit() } && pin == confirmPin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.pin_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.pin_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        pin = it
                        error = null
                    }
                },
                label = { Text(stringResource(R.string.pin_input_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                        confirmPin = it
                        error = null
                    }
                },
                label = { Text(stringResource(R.string.pin_confirm_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            error?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (onKeepPin != null) {
            Spacer(Modifier.height(16.dp))
            TimedButton(
                text = stringResource(R.string.pin_keep_current),
                onClick = onKeepPin,
                modifier = Modifier.fillMaxWidth()
            )
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
                text = stringResource(R.string.btn_proximo), onClick = {
                    when {
                        pin.length != 6 || !pin.all { it.isDigit() } -> error = errorDigits

                        pin != confirmPin -> error = errorMismatch

                        else -> onComplete(pin)
                    }
                }, modifier = Modifier.weight(1f), enabled = isValid
            )
        }
    }
}
