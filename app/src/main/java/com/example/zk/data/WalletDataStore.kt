package com.example.zk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

/**
 * DataStore for local wallet state.
 *
 * SECURITY NOTES:
 * - PIN is stored as salted SHA-256 hash (never plain text)
 * - In production, use Android Keystore for additional encryption
 * - Consider using EncryptedSharedPreferences or Tink for credential storage
 */
private val Context.walletDataStore: DataStore<Preferences> by preferencesDataStore(name = "wallet_prefs")

class WalletDataStore(private val context: Context) {

    companion object {
        // Wallet state keys
        private val WALLET_INITIALIZED = booleanPreferencesKey("wallet_initialized")
        private val PIN_HASH = stringPreferencesKey("pin_hash")
        private val PIN_SALT = stringPreferencesKey("pin_salt")
        private val PUBLIC_KEY = stringPreferencesKey("public_key")
        private val CREDENTIAL_STORED = booleanPreferencesKey("credential_stored")
        private val CREDENTIAL_DATA = stringPreferencesKey("credential_data")
        private val USER_NAME = stringPreferencesKey("user_name")

        // Passport data keys (derived from NFC scan - raw data NOT stored)
        private val PASSPORT_FULL_NAME = stringPreferencesKey("passport_full_name")
        private val PASSPORT_NATIONALITY = stringPreferencesKey("passport_nationality")
        private val PASSPORT_DOB = stringPreferencesKey("passport_dob")
        private val PASSPORT_GENDER = stringPreferencesKey("passport_gender")
        private val PASSPORT_DOC_NUMBER = stringPreferencesKey("passport_doc_number")
        private val PASSPORT_EXPIRY = stringPreferencesKey("passport_expiry")
        private val PASSPORT_ISSUING_COUNTRY = stringPreferencesKey("passport_issuing_country")

        // Biometric preference key
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")

        // Language preference key
        private val LANGUAGE_CODE = stringPreferencesKey("language_code")

        // Generate a secure random salt
        private fun generateSalt(): ByteArray {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            return salt
        }

        // Hash PIN with salt using SHA-256
        // NOTE: In production, use PBKDF2 or Argon2 for better security
        fun hashPin(pin: String, salt: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(salt)
            val hashedBytes = md.digest(pin.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
        }
    }

    // Check if wallet is initialized
    val isWalletInitialized: Flow<Boolean> = context.walletDataStore.data.map { prefs ->
        prefs[WALLET_INITIALIZED] ?: false
    }

    // Check if credential is stored
    val hasCredential: Flow<Boolean> = context.walletDataStore.data.map { prefs ->
        prefs[CREDENTIAL_STORED] ?: false
    }

    // Get user name
    val userName: Flow<String> = context.walletDataStore.data.map { prefs ->
        prefs[USER_NAME] ?: "User"
    }

    // Get public key (mock)
    val publicKey: Flow<String?> = context.walletDataStore.data.map { prefs ->
        prefs[PUBLIC_KEY]
    }

    // Passport data flows
    val passportFullName: Flow<String> = context.walletDataStore.data.map { prefs ->
        prefs[PASSPORT_FULL_NAME] ?: ""
    }

    val passportNationality: Flow<String> = context.walletDataStore.data.map { prefs ->
        prefs[PASSPORT_NATIONALITY] ?: ""
    }

    val passportDateOfBirth: Flow<String> = context.walletDataStore.data.map { prefs ->
        prefs[PASSPORT_DOB] ?: ""
    }

    val passportGender: Flow<String> = context.walletDataStore.data.map { prefs ->
        prefs[PASSPORT_GENDER] ?: ""
    }

    val passportDocNumber: Flow<String> = context.walletDataStore.data.map { prefs ->
        prefs[PASSPORT_DOC_NUMBER] ?: ""
    }

    val passportExpiry: Flow<String> = context.walletDataStore.data.map { prefs ->
        prefs[PASSPORT_EXPIRY] ?: ""
    }

    val passportIssuingCountry: Flow<String> = context.walletDataStore.data.map { prefs ->
        prefs[PASSPORT_ISSUING_COUNTRY] ?: ""
    }

    // Biometric login preference
    val isBiometricEnabled: Flow<Boolean> = context.walletDataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED] ?: false
    }

    // Language preference
    val languageCode: Flow<String> = context.walletDataStore.data.map { prefs ->
        prefs[LANGUAGE_CODE] ?: "en"
    }

    /**
     * Set language preference
     */
    suspend fun setLanguage(languageCode: String) {
        context.walletDataStore.edit { prefs ->
            prefs[LANGUAGE_CODE] = languageCode
        }
    }

    /**
     * Enable or disable biometric login
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.walletDataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED] = enabled
        }
    }

    /**
     * Save passport data from NFC scan
     * NOTE: Only essential derived data is stored, raw passport data is NOT saved
     */
    suspend fun savePassportData(
        fullName: String,
        nationality: String,
        dateOfBirth: String,
        gender: String,
        documentNumber: String,
        expiryDate: String,
        issuingCountry: String
    ) {
        context.walletDataStore.edit { prefs ->
            prefs[PASSPORT_FULL_NAME] = fullName
            prefs[PASSPORT_NATIONALITY] = nationality
            prefs[PASSPORT_DOB] = dateOfBirth
            prefs[PASSPORT_GENDER] = gender
            prefs[PASSPORT_DOC_NUMBER] = documentNumber
            prefs[PASSPORT_EXPIRY] = expiryDate
            prefs[PASSPORT_ISSUING_COUNTRY] = issuingCountry
            prefs[USER_NAME] = fullName
            prefs[CREDENTIAL_STORED] = true
        }
    }

    /**
     * Initialize wallet with PIN and generate key pair (mock)
     * This creates the local wallet state - NO cloud account, NO blockchain storage
     */
    suspend fun initializeWallet(pin: String, userName: String = "User"): Boolean {
        return try {
            val salt = generateSalt()
            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val pinHash = hashPin(pin, salt)

            // Generate mock key pair (in production, use Android Keystore)
            val mockPublicKey = generateMockKeyPair()

            context.walletDataStore.edit { prefs ->
                prefs[WALLET_INITIALIZED] = true
                prefs[PIN_HASH] = pinHash
                prefs[PIN_SALT] = saltBase64
                prefs[PUBLIC_KEY] = mockPublicKey
                prefs[USER_NAME] = userName
                prefs[CREDENTIAL_STORED] = false
                // NOTE: Do NOT write verification history during wallet creation
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verify PIN for wallet unlock
     */
    suspend fun verifyPin(pin: String): Boolean {
        return try {
            val prefs = context.walletDataStore.data.first()
            val storedHash = prefs[PIN_HASH]
            val storedSaltBase64 = prefs[PIN_SALT]

            if (storedHash != null && storedSaltBase64 != null) {
                val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
                val inputHash = hashPin(pin, salt)
                storedHash == inputHash
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Change PIN (requires verification of old PIN first)
     */
    suspend fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) {
            return false
        }

        return try {
            val salt = generateSalt()
            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val pinHash = hashPin(newPin, salt)

            context.walletDataStore.edit { prefs ->
                prefs[PIN_HASH] = pinHash
                prefs[PIN_SALT] = saltBase64
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Store credential locally (encrypted in production)
     * Raw passport data is NOT stored - only processed credential
     */
    suspend fun storeCredential(credentialJson: String): Boolean {
        return try {
            // In production: encrypt with Keystore-backed key
            context.walletDataStore.edit { prefs ->
                prefs[CREDENTIAL_STORED] = true
                prefs[CREDENTIAL_DATA] = credentialJson
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get stored credential
     */
    val credential: Flow<String?> = context.walletDataStore.data.map { prefs ->
        prefs[CREDENTIAL_DATA]
    }

    /**
     * Update user name
     */
    suspend fun updateUserName(name: String) {
        context.walletDataStore.edit { prefs ->
            prefs[USER_NAME] = name
        }
    }

    /**
     * Clear all wallet data (for logout/reset)
     */
    suspend fun clearWallet() {
        context.walletDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    /**
     * Generate mock key pair - in production use Android Keystore
     *
     * Production implementation should:
     * 1. Generate key pair in Android Keystore
     * 2. Use hardware-backed keys if available
     * 3. Set authentication required for private key access
     */
    private fun generateMockKeyPair(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return "pk_" + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
