package io.github.matheusghenriques.thedoor.data

import kotlinx.coroutines.flow.MutableStateFlow

object CurrentUsageProvider {
    val currentUsage = MutableStateFlow<Map<String, Long>>(emptyMap())
}
