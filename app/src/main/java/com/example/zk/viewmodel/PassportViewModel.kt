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
import com.example.zk.network.IssuerApiClient
import com.example.zk.network.IssuePassportRequest
import com.example.zk.passport.MrzScanner
import com.example.zk.passport.PassportNfcReader
import com.example.zk.util.CryptoManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

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
     * Create credential from passport data by calling the Issuer Backend API.
     *
     * 1. Uses CryptoManager to get or generate the device DID.
     * 2. Converts passport fields to ZK-friendly integers.
     * 3. Calls the Issuer API on Dispatchers.IO.
     * 4. Stores the full signed Verifiable Credential JSON locally.
     */
    private suspend fun createCredentialFromPassport(passport: PassportData) {
        Log.d(TAG, "Creating credential from passport data...")
        Log.d(TAG, "Name: ${passport.fullName}")
        Log.d(TAG, "Nationality: ${passport.nationality}")
        Log.d(TAG, "DOB: ${passport.formattedDateOfBirth}")
        Log.d(TAG, "Gender: ${passport.gender}")
        Log.d(TAG, "Doc Number: ${passport.documentNumber}")

        // Save passport data to profile (display purposes)
        walletDataStore.savePassportData(
            fullName = passport.fullName,
            nationality = passport.nationality,
            dateOfBirth = passport.formattedDateOfBirth,
            gender = passport.gender,
            documentNumber = passport.documentNumber,
            expiryDate = passport.formattedExpiryDate,
            issuingCountry = passport.issuingCountry
        )

        try {
            // 1. Get or generate device DID from Android KeyStore
            val did = withContext(Dispatchers.IO) {
                CryptoManager.getDeviceDid()
            }
            Log.d(TAG, "Device DID: $did")

            // 2. Convert passport fields to ZK-friendly integers
            val dobInt = formatDobToInt(passport.dateOfBirth)   // YYMMDD -> YYYYMMDD
            val passportNumberInt = stringToInt(passport.documentNumber)
            val nationalityInt = stringToInt(passport.nationality)

            Log.d(TAG, "ZK inputs -> dob=$dobInt, passport#=$passportNumberInt, nat=$nationalityInt")

            // 3. Call Issuer API on background thread
            val response = withContext(Dispatchers.IO) {
                IssuerApiClient.api.issuePassport(
                    IssuePassportRequest(
                        did = did,
                        name = passport.fullName,
                        dateOfBirth = dobInt,
                        passportNumber = passportNumberInt,
                        nationality = nationalityInt
                    )
                )
            }

            // 4. Serialize full VC to JSON and persist
            val vcJson = Gson().toJson(response.verifiableCredential)
            walletDataStore.storeCredential(vcJson)

            Log.d(TAG, "Verifiable Credential stored successfully!")

            _uiState.value = _uiState.value.copy(
                step = PassportScanStep.COMPLETE,
                isLoading = false,
                credentialCreated = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain Verifiable Credential from issuer", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Could not reach issuer backend: ${e.localizedMessage}"
            )
        }
    }

    // ── Helper: YYMMDD → YYYYMMDD integer ────────────────────────────────────
    private fun formatDobToInt(dob: String): Int {
        if (dob.length != 6) return 0
        val yy = dob.substring(0, 2).toIntOrNull() ?: return 0
        val fullYear = if (yy > 30) 1900 + yy else 2000 + yy
        val mmdd = dob.substring(2) // "MMDD"
        return "$fullYear$mmdd".toIntOrNull() ?: 0
    }

    // ── Helper: deterministic string → positive Int ──────────────────────────
    private fun stringToInt(value: String): Int {
        // Try direct parse first (e.g. already numeric passport numbers)
        value.toIntOrNull()?.let { return abs(it) }
        // Deterministic hash for alpha-numeric values like nationality codes
        return abs(value.hashCode()) % 1_000_000_000
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
