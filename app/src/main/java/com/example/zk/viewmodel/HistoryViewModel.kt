package com.example.zk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zk.data.WalletDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for History/Activity screen
 * Manages proof history data and filtering
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val walletDataStore = WalletDataStore(application)

    data class HistoryState(
        val proofHistory: List<WalletDataStore.ProofHistoryEntry> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    val historyState: StateFlow<HistoryState> = walletDataStore.proofHistory
        .map { history ->
            HistoryState(
                proofHistory = history,
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HistoryState()
        )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun getFilteredHistory(allHistory: List<WalletDataStore.ProofHistoryEntry>, filter: String): List<WalletDataStore.ProofHistoryEntry> {
        return when (filter) {
            "Success" -> allHistory.filter { it.success }
            "Failed" -> allHistory.filter { !it.success }
            else -> allHistory
        }
    }

    /**
     * Clear all proof history
     */
    fun clearHistory(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            walletDataStore.clearProofHistory()
            onComplete()
        }
    }
}
