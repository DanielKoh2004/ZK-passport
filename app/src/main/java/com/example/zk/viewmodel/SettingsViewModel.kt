package com.example.zk.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zk.data.WalletDataStore
import com.example.zk.util.BiometricHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Settings screen
 * Provides user name, passport expiry, biometric settings, and language
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val walletDataStore = WalletDataStore(application)
    private val biometricHelper = BiometricHelper(application)

    data class SettingsState(
        val userName: String = "",
        val passportExpiry: String = "",
        val hasCredential: Boolean = false,
        val isBiometricEnabled: Boolean = false,
        val isBiometricAvailable: Boolean = false,
        val biometricUnavailableReason: String = "",
        val currentLanguage: String = "en"
    )

    val settingsState: StateFlow<SettingsState> = combine(
        walletDataStore.userName,
        walletDataStore.passportExpiry,
        walletDataStore.hasCredential,
        walletDataStore.isBiometricEnabled,
        walletDataStore.languageCode
    ) { name, expiry, hasCred, biometricEnabled, language ->
        SettingsState(
            userName = name,
            passportExpiry = expiry,
            hasCredential = hasCred,
            isBiometricEnabled = biometricEnabled,
            isBiometricAvailable = biometricHelper.isBiometricAvailable(),
            biometricUnavailableReason = biometricHelper.getBiometricUnavailableReason(),
            currentLanguage = language
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsState(
            isBiometricAvailable = biometricHelper.isBiometricAvailable(),
            biometricUnavailableReason = biometricHelper.getBiometricUnavailableReason()
        )
    )

    /**
     * Enable or disable biometric login
     */
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            walletDataStore.setBiometricEnabled(enabled)
        }
    }

    /**
     * Set app language
     * Uses AppCompatDelegate to change locale at runtime
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            walletDataStore.setLanguage(languageCode)
            // Apply locale change using AppCompat
            val localeList = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }
}
