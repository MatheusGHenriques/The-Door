package io.github.matheusghenriques.thedoor.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.matheusghenriques.thedoor.R
import io.github.matheusghenriques.thedoor.data.TimeSchedule
import java.util.Calendar

data class TimeSavedMetrics(
    val perDayFormatted: String = "0 min",
    val weekFormatted: String = "0 min",
    val totalFormatted: String = "0 min"
)

fun formatMinutes(minutes: Int): String = when {
    minutes <= 0 -> "0 min"
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

sealed interface DashboardCard {
    data class Opens(val appName: String, val openCount: Int, val maxOpens: Int) : DashboardCard {
        val fraction: Float
            get() = if (maxOpens > 0) (openCount.toFloat() / maxOpens).coerceIn(
                0f,
                1f
            ) else 0f
        val isWarning: Boolean get() = fraction in 0.8f..<1f
        val isExceeded: Boolean get() = fraction >= 1f
    }

    data class Limit(val appName: String, val usageMinutes: Int, val limitMinutes: Int) :
        DashboardCard {
        val fraction: Float
            get() = if (limitMinutes > 0) (usageMinutes.toFloat() / limitMinutes).coerceIn(
                0f,
                1f
            ) else 0f
        val isWarning: Boolean get() = fraction in 0.8f..<1f
        val isExceeded: Boolean get() = fraction >= 1f
    }

    data class Schedule(val appName: String, val summary: String) : DashboardCard
    data class Blocked(val appName: String) : DashboardCard
}

fun formatScheduleSummary(schedules: List<TimeSchedule>): String {
    return schedules.joinToString(" \u2022 ") { s ->
        "Blocked from %02d:%02d to %02d:%02d on %s".format(
            s.startHour, s.startMinute, s.endHour, s.endMinute,
            s.daysOfWeek.sorted().joinToString(", ") { dayLabel(it) }
        )
    }
}

private fun dayLabel(day: Int): String = when (day) {
    Calendar.MONDAY -> "Mon"
    Calendar.TUESDAY -> "Tue"
    Calendar.WEDNESDAY -> "Wed"
    Calendar.THURSDAY -> "Thu"
    Calendar.FRIDAY -> "Fri"
    Calendar.SATURDAY -> "Sat"
    Calendar.SUNDAY -> "Sun"
    else -> "?"
}

@Composable
fun MainScreen(
    redirectPhrase: String = "",
    zeroTrust: Boolean = false,
    showSettingsButton: Boolean = false,
    onSettingsClick: (() -> Unit)? = null,
    onToggleLanguage: (() -> Unit)? = null,
    dashboardCards: List<DashboardCard> = emptyList(),
    savedMetrics: TimeSavedMetrics = TimeSavedMetrics()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
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
                text = redirectPhrase.ifBlank { stringResource(R.string.onboarding_welcome_quote) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            if (showSettingsButton && onSettingsClick != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.main_btn_settings))
                }
            }

            if (savedMetrics.totalFormatted != "0 min") {
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader(stringResource(R.string.main_time_saved_title))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.main_time_saved_today, savedMetrics.perDayFormatted
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.main_time_saved_week, savedMetrics.weekFormatted
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.main_time_saved_alltime, savedMetrics.totalFormatted
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (dashboardCards.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.main_section_dashboard))

                dashboardCards.forEach { card ->
                    Spacer(Modifier.height(8.dp))
                    when (card) {
                        is DashboardCard.Opens -> OpensProgressCard(card)
                        is DashboardCard.Limit -> LimitProgressCard(card)
                        is DashboardCard.Schedule -> ScheduleCard(card)
                        is DashboardCard.Blocked -> BlockedAppCard(card)
                    }
                }
            }

            if (zeroTrust) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.main_zero_trust_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.main_zero_trust_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        if (onToggleLanguage != null) {
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
private fun OpensProgressCard(card: DashboardCard.Opens) {
    val indicatorColor = when {
        card.isExceeded -> MaterialTheme.colorScheme.error
        card.isWarning -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.main_opens_progress,
                        card.openCount,
                        card.maxOpens
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { card.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = indicatorColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun LimitProgressCard(card: DashboardCard.Limit) {
    val indicatorColor = when {
        card.isExceeded -> MaterialTheme.colorScheme.error
        card.isWarning -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${card.usageMinutes} / ${card.limitMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { card.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = indicatorColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ScheduleCard(card: DashboardCard.Schedule) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = card.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = card.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun BlockedAppCard(card: DashboardCard.Blocked) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = card.appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(R.string.main_status_blocked_completely),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
