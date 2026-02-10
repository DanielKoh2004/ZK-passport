package com.example.zk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zk.data.WalletDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for wallet creation flow:
 * 1. Set PIN
 * 2. Enrollment (passport scan simulation)
 * 3. Wallet created success
 *
 * This is for LOCAL wallet initialization only.
 * No cloud account, no blockchain storage of personal data.
 */
class WalletSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val walletDataStore = WalletDataStore(application)

    // UI State
    private val _uiState = MutableStateFlow(WalletSetupUiState())
    val uiState: StateFlow<WalletSetupUiState> = _uiState.asStateFlow()

    // PIN entry state
    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    private val _confirmPin = MutableStateFlow("")
    val confirmPin: StateFlow<String> = _confirmPin.asStateFlow()

    private val _isConfirmingPin = MutableStateFlow(false)
    val isConfirmingPin: StateFlow<Boolean> = _isConfirmingPin.asStateFlow()

    // Enrollment state
    private val _enrollmentComplete = MutableStateFlow(false)
    val enrollmentComplete: StateFlow<Boolean> = _enrollmentComplete.asStateFlow()

    // Extracted data from passport (mock)
    private val _extractedName = MutableStateFlow("")
    val extractedName: StateFlow<String> = _extractedName.asStateFlow()

    /**
     * Enter PIN digit
     */
    fun enterPinDigit(digit: String) {
        if (_isConfirmingPin.value) {
            if (_confirmPin.value.length < 6) {
                _confirmPin.value += digit

                // Auto-validate when confirm PIN is complete
                if (_confirmPin.value.length == 6) {
                    validateConfirmPin()
                }
            }
        } else {
            if (_pin.value.length < 6) {
                _pin.value += digit

                // Move to confirm when PIN is complete
                if (_pin.value.length == 6) {
                    _isConfirmingPin.value = true
                }
            }
        }
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Delete last PIN digit
     */
    fun deletePinDigit() {
        if (_isConfirmingPin.value) {
            if (_confirmPin.value.isNotEmpty()) {
                _confirmPin.value = _confirmPin.value.dropLast(1)
            } else {
                // Go back to PIN entry
                _isConfirmingPin.value = false
            }
        } else {
            if (_pin.value.isNotEmpty()) {
                _pin.value = _pin.value.dropLast(1)
            }
        }
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Validate confirm PIN matches original PIN
     */
    private fun validateConfirmPin() {
        if (_pin.value == _confirmPin.value) {
            _uiState.value = _uiState.value.copy(pinSet = true, errorMessage = null)
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = "PINs don't match. Try again.")
            _confirmPin.value = ""
        }
    }

    /**
     * Reset PIN entry
     */
    fun resetPin() {
        _pin.value = ""
        _confirmPin.value = ""
        _isConfirmingPin.value = false
        _uiState.value = _uiState.value.copy(pinSet = false, errorMessage = null)
    }

    /**
     * Simulate passport enrollment
     * In production: NFC read, OCR, or manual entry
     * Raw passport data is processed locally only - NOT stored or sent to blockchain
     */
    fun simulatePassportEnrollment() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Simulate processing delay
            kotlinx.coroutines.delay(2000)

            // Mock extracted passport data
            _extractedName.value = "Alex Morgan"

            // Create mock credential (in production: generate from passport data)
            val mockCredential = """
                {
                    "type": "passport_credential",
                    "issuer": "local_wallet",
                    "issued_at": ${System.currentTimeMillis()},
                    "claims": {
                        "name_hash": "hashed_value",
                        "dob_commitment": "commitment_value",
                        "nationality_commitment": "commitment_value"
                    }
                }
            """.trimIndent()

            // Store credential locally (encrypted in production)
            walletDataStore.storeCredential(mockCredential)

            _enrollmentComplete.value = true
            _uiState.value = _uiState.value.copy(isLoading = false, enrollmentComplete = true)
        }
    }

    /**
     * Complete wallet creation
     * Initialize wallet with PIN and store state locally
     * NOTE: Do NOT write verification history during wallet creation
     */
    fun completeWalletCreation(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val success = walletDataStore.initializeWallet(
                pin = _pin.value,
                userName = _extractedName.value.ifEmpty { "User" }
            )

            if (success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    walletCreated = true
                )
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to create wallet"
                )
                onError("Failed to create wallet")
            }
        }
    }

    /**
     * Skip enrollment (for testing)
     */
    fun skipEnrollment() {
        _extractedName.value = "User"
        _enrollmentComplete.value = true
        _uiState.value = _uiState.value.copy(enrollmentComplete = true)
    }
}

data class WalletSetupUiState(
    val isLoading: Boolean = false,
    val pinSet: Boolean = false,
    val enrollmentComplete: Boolean = false,
    val walletCreated: Boolean = false,
    val errorMessage: String? = null
)
