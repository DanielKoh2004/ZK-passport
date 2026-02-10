package com.example.zk.viewmodel

import android.app.Application
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zk.data.MrzData
import com.example.zk.data.PassportData
import com.example.zk.data.PassportReadingState
import com.example.zk.data.WalletDataStore
import com.example.zk.passport.MrzScanner
import com.example.zk.passport.PassportNfcReader
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for passport scanning and wallet creation
 *
 * Flow:
 * 1. Scan MRZ (camera) to get document number, DOB, expiry
 * 2. Read NFC chip using MRZ data for authentication
 * 3. Extract passport data
 * 4. Create local credential (no upload)
 */
class PassportViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PassportViewModel"
    }

    private val walletDataStore = WalletDataStore(application)
    private val passportReader = PassportNfcReader()
    private val mrzScanner = MrzScanner()

    // UI State
    private val _uiState = MutableStateFlow(PassportUiState())
    val uiState: StateFlow<PassportUiState> = _uiState.asStateFlow()

    // MRZ Data (from camera or manual input)
    private val _mrzData = MutableStateFlow<MrzData?>(null)
    val mrzData: StateFlow<MrzData?> = _mrzData.asStateFlow()

    // Passport Data (from NFC)
    private val _passportData = MutableStateFlow<PassportData?>(null)
    val passportData: StateFlow<PassportData?> = _passportData.asStateFlow()

    // NFC reading state
    val readingState: StateFlow<PassportReadingState> = passportReader.readingState

    // Manual MRZ input fields
    private val _documentNumber = MutableStateFlow("")
    val documentNumber: StateFlow<String> = _documentNumber.asStateFlow()

    private val _dateOfBirth = MutableStateFlow("")
    val dateOfBirth: StateFlow<String> = _dateOfBirth.asStateFlow()

    private val _expiryDate = MutableStateFlow("")
    val expiryDate: StateFlow<String> = _expiryDate.asStateFlow()

    init {
        // Observe reading state changes
        viewModelScope.launch {
            passportReader.readingState.collect { state ->
                when (state) {
                    is PassportReadingState.Success -> {
                        _passportData.value = state.passportData
                        _uiState.value = _uiState.value.copy(
                            step = PassportScanStep.COMPLETE,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    is PassportReadingState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = state.message
                        )
                    }
                    PassportReadingState.Connecting,
                    PassportReadingState.ReadingData,
                    PassportReadingState.VerifyingAuthenticity -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    else -> {}
                }
            }
        }
    }

    // NOTE: Camera-based MRZ scanning is not yet implemented
    // For now, users must enter MRZ data manually
    // To implement: add ML Kit text recognition for real-time MRZ detection

    /**
     * Update document number (manual input)
     */
    fun updateDocumentNumber(value: String) {
        _documentNumber.value = value.uppercase().filter { it.isLetterOrDigit() }
    }

    /**
     * Update date of birth (manual input, format: YYMMDD)
     */
    fun updateDateOfBirth(value: String) {
        _dateOfBirth.value = value.filter { it.isDigit() }.take(6)
    }

    /**
     * Update expiry date (manual input, format: YYMMDD)
     */
    fun updateExpiryDate(value: String) {
        _expiryDate.value = value.filter { it.isDigit() }.take(6)
    }

    /**
     * Confirm MRZ data and proceed to next step
     * From MANUAL_MRZ -> CONFIRM_MRZ
     * From CONFIRM_MRZ -> SCAN_NFC
     */
    fun confirmMrzData() {
        val currentStep = _uiState.value.step

        val mrz = mrzScanner.createMrzData(
            documentNumber = _documentNumber.value,
            dateOfBirth = _dateOfBirth.value,
            expiryDate = _expiryDate.value
        )

        if (mrz == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Invalid MRZ data. Please check and try again."
            )
            return
        }

        _mrzData.value = mrz

        when (currentStep) {
            PassportScanStep.MANUAL_MRZ -> {
                // Go to confirm step
                _uiState.value = _uiState.value.copy(
                    step = PassportScanStep.CONFIRM_MRZ,
                    errorMessage = null
                )
            }
            PassportScanStep.CONFIRM_MRZ -> {
                // Go to NFC scan step
                _uiState.value = _uiState.value.copy(
                    step = PassportScanStep.SCAN_NFC,
                    errorMessage = null
                )
                passportReader.waitForNfc()
            }
            else -> {
                // Default: go to NFC scan
                _uiState.value = _uiState.value.copy(
                    step = PassportScanStep.SCAN_NFC,
                    errorMessage = null
                )
                passportReader.waitForNfc()
            }
        }
    }

    /**
     * Skip MRZ camera scan and enter manually
     */
    fun enterMrzManually() {
        _uiState.value = _uiState.value.copy(
            step = PassportScanStep.MANUAL_MRZ,
            errorMessage = null
        )
    }

    /**
     * Go back to camera MRZ scan
     */
    fun backToMrzScan() {
        _uiState.value = _uiState.value.copy(
            step = PassportScanStep.SCAN_MRZ,
            errorMessage = null
        )
    }

    /**
     * Handle NFC tag discovered
     * Called from Activity when NFC tag is detected
     */
    fun handleNfcTag(tag: Tag) {
        Log.d(TAG, "NFC Tag received! Tag: $tag")
        Log.d(TAG, "Current UI step: ${_uiState.value.step}")

        val mrz = _mrzData.value
        if (mrz == null) {
            Log.e(TAG, "MRZ data is null - cannot authenticate with passport")
            _uiState.value = _uiState.value.copy(
                errorMessage = "MRZ data not available. Please enter passport MRZ first."
            )
            return
        }

        Log.d(TAG, "MRZ data available: docNum=${mrz.documentNumber}, dob=${mrz.dateOfBirth}, exp=${mrz.expiryDate}")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            Log.d(TAG, "Starting passport read...")

            val result = passportReader.readPassport(tag, mrz)

            if (result != null) {
                Log.d(TAG, "Passport read successful: ${result.fullName}")
                _passportData.value = result
                // Store credential locally
                createCredentialFromPassport(result)
            } else {
                Log.e(TAG, "Passport read failed")
            }
        }
    }

    /**
     * Handle NFC intent from Activity
     */
    fun handleNfcIntent(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            handleNfcTag(tag)
        }
    }

    /**
     * Create local credential from passport data
     *
     * IMPORTANT: Raw passport data is NOT stored
     * Only derived credentials (hashes, commitments) are saved
     */
    private suspend fun createCredentialFromPassport(passport: PassportData) {
        Log.d(TAG, "Creating credential from passport data...")
        Log.d(TAG, "Name: ${passport.fullName}")
        Log.d(TAG, "Nationality: ${passport.nationality}")
        Log.d(TAG, "DOB: ${passport.formattedDateOfBirth}")
        Log.d(TAG, "Gender: ${passport.gender}")
        Log.d(TAG, "Doc Number: ${passport.documentNumber}")

        // Save passport data to profile
        walletDataStore.savePassportData(
            fullName = passport.fullName,
            nationality = passport.nationality,
            dateOfBirth = passport.formattedDateOfBirth,
            gender = passport.gender,
            documentNumber = passport.documentNumber,
            expiryDate = passport.formattedExpiryDate,
            issuingCountry = passport.issuingCountry
        )

        // Create credential JSON with only necessary derived data
        // In production: create proper ZK commitments
        val credentialJson = """
            {
                "type": "passport_credential",
                "issuer": "local_wallet",
                "issued_at": ${System.currentTimeMillis()},
                "claims": {
                    "name_hash": "${hashValue(passport.fullName)}",
                    "nationality": "${passport.nationality}",
                    "dob_year": "${passport.dateOfBirth.take(2)}",
                    "is_adult": ${passport.calculateAge() >= 18},
                    "document_valid": ${!passport.isExpired()},
                    "authentic": ${passport.isAuthentic}
                },
                "metadata": {
                    "document_country": "${passport.issuingCountry}",
                    "created_locally": true,
                    "no_raw_data_stored": true
                }
            }
        """.trimIndent()

        walletDataStore.storeCredential(credentialJson)

        Log.d(TAG, "Passport data saved to profile successfully!")

        _uiState.value = _uiState.value.copy(
            step = PassportScanStep.COMPLETE,
            isLoading = false,
            credentialCreated = true
        )
    }

    /**
     * Simple hash function for demo
     * In production: use proper cryptographic hash
     */
    private fun hashValue(value: String): String {
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }

    /**
     * Reset to initial state
     */
    fun reset() {
        _mrzData.value = null
        _passportData.value = null
        _documentNumber.value = ""
        _dateOfBirth.value = ""
        _expiryDate.value = ""
        passportReader.reset()
        _uiState.value = PassportUiState()
    }

    /**
     * Retry NFC scan
     */
    fun retryNfcScan() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            isLoading = false
        )
        passportReader.waitForNfc()
    }

    override fun onCleared() {
        super.onCleared()
        mrzScanner.close()
    }
}

/**
 * Passport scanning steps
 */
enum class PassportScanStep {
    SCAN_MRZ,      // Camera scanning for MRZ
    MANUAL_MRZ,    // Manual MRZ entry
    CONFIRM_MRZ,   // Confirm scanned/entered MRZ
    SCAN_NFC,      // Waiting for NFC tap
    COMPLETE       // Passport read successfully
}

/**
 * UI State for passport scanning
 */
data class PassportUiState(
    val step: PassportScanStep = PassportScanStep.SCAN_MRZ,
    val isLoading: Boolean = false,
    val isProcessingFrame: Boolean = false,
    val errorMessage: String? = null,
    val credentialCreated: Boolean = false
)
