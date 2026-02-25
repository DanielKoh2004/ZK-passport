package com.example.zk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zk.data.WalletDataStore
import com.example.zk.util.BiometricHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    companion object {
        // Persist lockout state across ViewModel instances within the same process
        @Volatile private var failedAttemptCount = 0
        @Volatile private var lockoutEndTime = 0L
    }

    private val walletDataStore = WalletDataStore(application)
    private val biometricHelper = BiometricHelper(application)
    private var lockoutCountdownJob: Job? = null

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

    init {
        // Restore lockout state if still active from a previous ViewModel instance
        val now = System.currentTimeMillis()
        if (lockoutEndTime > now) {
            _uiState.value = _uiState.value.copy(
                isLockedOut = true,
                lockoutRemainingSeconds = ((lockoutEndTime - now) / 1000).toInt() + 1,
                failedAttempts = failedAttemptCount
            )
            startLockoutCountdown()
        }
    }

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
        // Block input during lockout
        if (_uiState.value.isLockedOut) return

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
     * Verify unlock PIN against stored hash with lockout protection
     */
    private fun verifyUnlockPin() {
        viewModelScope.launch {
            // Check if still locked out
            val now = System.currentTimeMillis()
            if (now < lockoutEndTime) {
                val remainingSeconds = ((lockoutEndTime - now) / 1000).toInt() + 1
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLockedOut = true,
                    lockoutRemainingSeconds = remainingSeconds,
                    errorMessage = "Too many attempts. Try again in ${remainingSeconds}s"
                )
                _unlockPin.value = ""
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)

            val isValid = walletDataStore.verifyPin(_unlockPin.value)

            if (isValid) {
                failedAttemptCount = 0
                lockoutEndTime = 0
                lockoutCountdownJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isUnlocked = true,
                    isLockedOut = false,
                    lockoutRemainingSeconds = 0,
                    failedAttempts = 0,
                    errorMessage = null
                )
            } else {
                failedAttemptCount++
                val lockoutDuration = getLockoutDuration()

                val errorMsg: String
                if (lockoutDuration > 0) {
                    lockoutEndTime = System.currentTimeMillis() + lockoutDuration
                    startLockoutCountdown()
                    errorMsg = "Too many attempts. Locked for ${lockoutDuration / 1000}s"
                } else {
                    val attemptsUntilLock = 3 - failedAttemptCount
                    errorMsg = if (attemptsUntilLock > 0) {
                        "Incorrect PIN. $attemptsUntilLock attempt${if (attemptsUntilLock > 1) "s" else ""} before lockout."
                    } else {
                        "Incorrect PIN. Try again."
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg,
                    failedAttempts = failedAttemptCount
                )
                _unlockPin.value = ""
            }
        }
    }

    /**
     * Get lockout duration based on number of failed attempts
     * Exponential backoff: 30s → 60s → 5min → 10min
     */
    private fun getLockoutDuration(): Long {
        return when {
            failedAttemptCount >= 10 -> 600_000L  // 10 minutes
            failedAttemptCount >= 7 -> 300_000L   // 5 minutes
            failedAttemptCount >= 5 -> 60_000L    // 1 minute
            failedAttemptCount >= 3 -> 30_000L    // 30 seconds
            else -> 0L
        }
    }

    /**
     * Start countdown timer that updates UI every second during lockout
     */
    private fun startLockoutCountdown() {
        lockoutCountdownJob?.cancel()
        lockoutCountdownJob = viewModelScope.launch {
            while (System.currentTimeMillis() < lockoutEndTime) {
                val remaining = ((lockoutEndTime - System.currentTimeMillis()) / 1000).toInt() + 1
                _uiState.value = _uiState.value.copy(
                    isLockedOut = true,
                    lockoutRemainingSeconds = remaining,
                    errorMessage = "Too many attempts. Try again in ${remaining}s"
                )
                delay(1000)
            }
            // Lockout expired
            _uiState.value = _uiState.value.copy(
                isLockedOut = false,
                lockoutRemainingSeconds = 0,
                errorMessage = "Lockout expired. You may try again."
            )
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
    val errorMessage: String? = null,
    val isLockedOut: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
    val failedAttempts: Int = 0
)
