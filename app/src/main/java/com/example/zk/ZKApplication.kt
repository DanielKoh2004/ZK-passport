package com.example.zk

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.zk.data.WalletDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application class for ZK Wallet
 * Handles app-wide initialization including language restoration
 */
class ZKApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Restore saved language preference on app startup
        restoreSavedLanguage()
    }

    private fun restoreSavedLanguage() {
        val walletDataStore = WalletDataStore(this)

        applicationScope.launch {
            try {
                val savedLanguage = walletDataStore.languageCode.first()
                if (savedLanguage.isNotEmpty() && savedLanguage != "en") {
                    val localeList = LocaleListCompat.forLanguageTags(savedLanguage)
                    AppCompatDelegate.setApplicationLocales(localeList)
                }
            } catch (e: Exception) {
                // Ignore errors during language restoration
            }
        }
    }
}
