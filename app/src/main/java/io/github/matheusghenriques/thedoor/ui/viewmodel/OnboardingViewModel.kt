package io.github.matheusghenriques.thedoor.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.matheusghenriques.thedoor.data.AppPreferences
import io.github.matheusghenriques.thedoor.data.ProtectionConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(private val prefs: AppPreferences) : ViewModel() {

    var currentPage by mutableIntStateOf(0)

    init {
        viewModelScope.launch {
            currentPage = prefs.onboardingPage.first()
        }
    }

    var blockAdultContent by mutableStateOf(false)
        private set

    var blockSocial by mutableStateOf(false)
        private set

    var blockGambling by mutableStateOf(false)
        private set

    val canContinue get() = blockAdultContent || blockSocial || blockGambling

    fun toggleAdultContent() {
        blockAdultContent = !blockAdultContent
    }

    fun toggleSocial() {
        blockSocial = !blockSocial
    }

    fun toggleGambling() {
        blockGambling = !blockGambling
    }

    fun nextPage() {
        if (currentPage < 3) {
            currentPage++
            viewModelScope.launch { prefs.saveOnboardingPage(currentPage) }
        }
    }

    fun previousPage() {
        if (currentPage > 0) {
            currentPage--
            viewModelScope.launch { prefs.saveOnboardingPage(currentPage) }
        }
    }

    fun finish(protectionConfig: ProtectionConfig, onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.finishOnboarding(blockAdultContent, blockSocial, blockGambling, protectionConfig)
            onDone()
        }
    }
}
