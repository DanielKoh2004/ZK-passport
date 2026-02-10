package com.example.zk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zk.data.WalletDataStore
import com.example.zk.util.BiometricHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication operations:
 * - Unlock wallet with 6-digit PIN
 * - Unlock wallet with biometric
 * - Change PIN (verify old, set new)
 *
 * This is LOCAL wallet unlock - no web password, no cloud auth.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val walletDataStore = WalletDataStore(application)
    private val biometricHelper = BiometricHelper(application)

    // Check if wallet exists
    val isWalletInitialized: StateFlow<Boolean> = walletDataStore.isWalletInitialized
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Check if credential is stored
    val hasCredential: StateFlow<Boolean> = walletDataStore.hasCredential
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // User name
    val userName: StateFlow<String> = walletDataStore.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    // Check if biometric login is enabled
    val isBiometricEnabled: StateFlow<Boolean> = walletDataStore.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Check if biometric is available on device
    val isBiometricAvailable: Boolean = biometricHelper.isBiometricAvailable()

    // UI State
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // PIN entry for unlock
    private val _unlockPin = MutableStateFlow("")
    val unlockPin: StateFlow<String> = _unlockPin.asStateFlow()

    // Change PIN state
    private val _changePinStep = MutableStateFlow(ChangePinStep.CURRENT)
    val changePinStep: StateFlow<ChangePinStep> = _changePinStep.asStateFlow()

    private val _currentPin = MutableStateFlow("")
    val currentPin: StateFlow<String> = _currentPin.asStateFlow()

    private val _newPin = MutableStateFlow("")
    val newPin: StateFlow<String> = _newPin.asStateFlow()

    private val _confirmNewPin = MutableStateFlow("")
    val confirmNewPin: StateFlow<String> = _confirmNewPin.asStateFlow()

    /**
     * Get the BiometricHelper instance for use in UI
     */
    fun getBiometricHelper(): BiometricHelper = biometricHelper

    /**
     * Mark as unlocked after successful biometric authentication
     */
    fun unlockWithBiometric() {
        _uiState.value = _uiState.value.copy(
            isUnlocked = true,
            errorMessage = null
        )
    }

    /**
     * Set error message for biometric authentication failure
     */
    fun setBiometricError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    /**
     * Enter digit for unlock PIN
     */
    fun enterUnlockDigit(digit: String) {
        if (_unlockPin.value.length < 6) {
            _unlockPin.value += digit
            _uiState.value = _uiState.value.copy(errorMessage = null)

            // Auto-verify when 6 digits entered
            if (_unlockPin.value.length == 6) {
                verifyUnlockPin()
            }
        }
    }

    /**
     * Delete last unlock digit
     */
    fun deleteUnlockDigit() {
        if (_unlockPin.value.isNotEmpty()) {
            _unlockPin.value = _unlockPin.value.dropLast(1)
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    /**
     * Verify unlock PIN
     */
    private fun verifyUnlockPin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val isValid = walletDataStore.verifyPin(_unlockPin.value)

            if (isValid) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isUnlocked = true,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Incorrect PIN. Try again."
                )
                _unlockPin.value = ""
            }
        }
    }

    /**
     * Reset unlock state
     */
    fun resetUnlock() {
        _unlockPin.value = ""
        _uiState.value = _uiState.value.copy(isUnlocked = false, errorMessage = null)
    }

    /**
     * Enter digit for change PIN flow
     */
    fun enterChangePinDigit(digit: String) {
        _uiState.value = _uiState.value.copy(errorMessage = null)

        when (_changePinStep.value) {
            ChangePinStep.CURRENT -> {
                if (_currentPin.value.length < 6) {
                    _currentPin.value += digit
                    if (_currentPin.value.length == 6) {
                        verifyCurrentPin()
                    }
                }
            }
            ChangePinStep.NEW -> {
                if (_newPin.value.length < 6) {
                    _newPin.value += digit
                    if (_newPin.value.length == 6) {
                        _changePinStep.value = ChangePinStep.CONFIRM
                    }
                }
            }
            ChangePinStep.CONFIRM -> {
                if (_confirmNewPin.value.length < 6) {
                    _confirmNewPin.value += digit
                    if (_confirmNewPin.value.length == 6) {
                        validateAndChangePin()
                    }
                }
            }
        }
    }

    /**
     * Delete digit in change PIN flow
     */
    fun deleteChangePinDigit() {
        _uiState.value = _uiState.value.copy(errorMessage = null)

        when (_changePinStep.value) {
            ChangePinStep.CURRENT -> {
                if (_currentPin.value.isNotEmpty()) {
                    _currentPin.value = _currentPin.value.dropLast(1)
                }
            }
            ChangePinStep.NEW -> {
                if (_newPin.value.isNotEmpty()) {
                    _newPin.value = _newPin.value.dropLast(1)
                } else {
                    _changePinStep.value = ChangePinStep.CURRENT
                }
            }
            ChangePinStep.CONFIRM -> {
                if (_confirmNewPin.value.isNotEmpty()) {
                    _confirmNewPin.value = _confirmNewPin.value.dropLast(1)
                } else {
                    _changePinStep.value = ChangePinStep.NEW
                }
            }
        }
    }

    /**
     * Verify current PIN before allowing change
     */
    private fun verifyCurrentPin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val isValid = walletDataStore.verifyPin(_currentPin.value)

            if (isValid) {
                _changePinStep.value = ChangePinStep.NEW
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = null)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Incorrect PIN"
                )
                _currentPin.value = ""
            }
        }
    }

    /**
     * Validate new PIN matches confirm and change
     */
    private fun validateAndChangePin() {
        if (_newPin.value != _confirmNewPin.value) {
            _uiState.value = _uiState.value.copy(errorMessage = "PINs don't match")
            _confirmNewPin.value = ""
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val success = walletDataStore.changePin(_currentPin.value, _newPin.value)

            if (success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pinChanged = true,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to change PIN"
                )
            }
        }
    }

    /**
     * Reset change PIN state
     */
    fun resetChangePin() {
        _changePinStep.value = ChangePinStep.CURRENT
        _currentPin.value = ""
        _newPin.value = ""
        _confirmNewPin.value = ""
        _uiState.value = _uiState.value.copy(pinChanged = false, errorMessage = null)
    }

    /**
     * Get current PIN entry based on step
     */
    fun getCurrentPinEntry(): String {
        return when (_changePinStep.value) {
            ChangePinStep.CURRENT -> _currentPin.value
            ChangePinStep.NEW -> _newPin.value
            ChangePinStep.CONFIRM -> _confirmNewPin.value
        }
    }

    /**
     * Logout - clear session (not wallet data)
     */
    fun logout() {
        _unlockPin.value = ""
        _uiState.value = AuthUiState()
    }

    /**
     * Delete wallet completely
     */
    fun deleteWallet(onComplete: () -> Unit) {
        viewModelScope.launch {
            walletDataStore.clearWallet()
            onComplete()
        }
    }
}

enum class ChangePinStep {
    CURRENT,
    NEW,
    CONFIRM
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isUnlocked: Boolean = false,
    val pinChanged: Boolean = false,
    val errorMessage: String? = null
)
