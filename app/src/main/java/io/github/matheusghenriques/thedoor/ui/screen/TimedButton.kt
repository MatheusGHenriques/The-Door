package io.github.matheusghenriques.thedoor.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun TimedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    delayMillis: Int = 3000,
    enabled: Boolean = true
) {
    val progress = remember { Animatable(0f) }
    var timerReady by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val layoutDirection = LayoutDirection.Ltr
    val shape = ButtonDefaults.shape

    val disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    val containerColor by animateColorAsState(
        targetValue = if (enabled) primary else disabledContainerColor,
        animationSpec = tween(250),
        label = "Container Color"
    )

    val textColor by animateColorAsState(
        targetValue = if (enabled) onPrimary else disabledContentColor,
        animationSpec = tween(250),
        label = "Text Color"
    )

    LaunchedEffect(Unit) {
        timerReady = false
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(delayMillis, easing = LinearEasing))
        timerReady = true
    }

    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        enabled = timerReady && enabled,
        shape = shape,
        color = Color.Transparent
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
            Row(
                Modifier
                    .defaultMinSize(
                        minWidth = ButtonDefaults.MinWidth, minHeight = ButtonDefaults.MinHeight
                    )
                    .drawBehind {
                        val outline = shape.createOutline(size, layoutDirection, density)
                        val path = Path().apply { addOutline(outline) }

                        clipPath(path) {
                            if (timerReady) {
                                drawRect(color = containerColor)
                            } else {
                                drawRect(
                                    color = containerColor,
                                    size = Size(size.width * progress.value, size.height)
                                )
                                val trackAlpha = containerColor.alpha * 0.3f
                                drawRect(
                                    color = containerColor.copy(alpha = trackAlpha),
                                    topLeft = Offset(x = size.width * progress.value, y = 0f),
                                    size = Size(size.width * (1f - progress.value), size.height)
                                )
                            }
                        }
                    }
                    .padding(ButtonDefaults.ContentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Text(text = text, color = textColor)
            }
        }
    }
}