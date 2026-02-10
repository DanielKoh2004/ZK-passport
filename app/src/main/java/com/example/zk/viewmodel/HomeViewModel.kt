package com.example.zk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zk.data.WalletDataStore
import kotlinx.coroutines.flow.*

/**
 * ViewModel for Home screen
 * Provides user name and credential status
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val walletDataStore = WalletDataStore(application)

    data class HomeState(
        val userName: String = "",
        val hasCredential: Boolean = false
    )

    val homeState: StateFlow<HomeState> = combine(
        walletDataStore.userName,
        walletDataStore.hasCredential
    ) { name, hasCred ->
        HomeState(
            userName = name,
            hasCredential = hasCred
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeState()
    )
}
