package com.example.zk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zk.data.WalletDataStore
import kotlinx.coroutines.flow.*

/**
 * ViewModel for Home screen
 * Provides user name, credential status, proof statistics
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val walletDataStore = WalletDataStore(application)

    data class HomeState(
        val userName: String = "",
        val hasCredential: Boolean = false,
        val totalProofs: Int = 0,
        val successfulProofs: Int = 0,
        val lastProofTimestamp: Long = 0L,
        val passportExpiry: String = ""
    )

    val homeState: StateFlow<HomeState> = combine(
        walletDataStore.userName,
        walletDataStore.hasCredential,
        walletDataStore.proofHistory,
        walletDataStore.passportExpiry
    ) { name, hasCred, history, expiry ->
        HomeState(
            userName = name,
            hasCredential = hasCred,
            totalProofs = history.size,
            successfulProofs = history.count { it.success },
            lastProofTimestamp = history.firstOrNull()?.timestamp ?: 0L,
            passportExpiry = expiry
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeState()
    )
}
