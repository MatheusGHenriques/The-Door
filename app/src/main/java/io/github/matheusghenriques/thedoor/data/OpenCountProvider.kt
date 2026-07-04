package io.github.matheusghenriques.thedoor.data

import kotlinx.coroutines.flow.MutableStateFlow

object OpenCountProvider {
    val openCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
}
