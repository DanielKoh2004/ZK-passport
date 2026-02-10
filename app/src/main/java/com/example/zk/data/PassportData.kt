package com.example.zk.data

import android.graphics.Bitmap

/**
 * Data class representing passport information extracted from NFC chip
 *
 * IMPORTANT PRIVACY NOTES:
 * - This data is processed LOCALLY only
 * - Raw passport data is NEVER uploaded to cloud or blockchain
 * - Only derived credentials (hashes, commitments) are stored
 */
data class PassportData(
    // Personal Information (DG1)
    val documentType: String = "",
    val issuingCountry: String = "",
    val documentNumber: String = "",
    val nationality: String = "",
    val dateOfBirth: String = "",  // Format: YYMMDD
    val gender: String = "",
    val expiryDate: String = "",   // Format: YYMMDD
    val lastName: String = "",
    val firstName: String = "",

    // Photo (DG2) - optional
    val photo: Bitmap? = null,

    // MRZ data
    val mrzLine1: String = "",
    val mrzLine2: String = "",

    // Verification status
    val isAuthentic: Boolean = false,
    val activeAuthenticationSupported: Boolean = false
) {
    /**
     * Get full name
     */
    val fullName: String
        get() = "$firstName $lastName".trim()

    /**
     * Get formatted date of birth (DD/MM/YYYY)
     */
    val formattedDateOfBirth: String
        get() {
            if (dateOfBirth.length != 6) return dateOfBirth
            val year = dateOfBirth.substring(0, 2)
            val month = dateOfBirth.substring(2, 4)
            val day = dateOfBirth.substring(4, 6)
            // Assume 19xx for years > 30, 20xx otherwise
            val fullYear = if (year.toIntOrNull() ?: 0 > 30) "19$year" else "20$year"
            return "$day/$month/$fullYear"
        }

    /**
     * Get formatted expiry date (DD/MM/YYYY)
     */
    val formattedExpiryDate: String
        get() {
            if (expiryDate.length != 6) return expiryDate
            val year = expiryDate.substring(0, 2)
            val month = expiryDate.substring(2, 4)
            val day = expiryDate.substring(4, 6)
            val fullYear = "20$year"
            return "$day/$month/$fullYear"
        }

    /**
     * Calculate age from date of birth
     */
    fun calculateAge(): Int {
        if (dateOfBirth.length != 6) return 0
        val year = dateOfBirth.substring(0, 2).toIntOrNull() ?: return 0
        val fullYear = if (year > 30) 1900 + year else 2000 + year
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return currentYear - fullYear
    }

    /**
     * Check if passport is expired
     */
    fun isExpired(): Boolean {
        if (expiryDate.length != 6) return false
        val year = expiryDate.substring(0, 2).toIntOrNull() ?: return false
        val month = expiryDate.substring(2, 4).toIntOrNull() ?: return false
        val day = expiryDate.substring(4, 6).toIntOrNull() ?: return false
        val fullYear = 2000 + year

        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        return when {
            fullYear < currentYear -> true
            fullYear > currentYear -> false
            month < currentMonth -> true
            month > currentMonth -> false
            else -> day < currentDay
        }
    }
}

/**
 * MRZ (Machine Readable Zone) data for passport
 * Used to establish secure connection with passport chip
 */
data class MrzData(
    val documentNumber: String,
    val dateOfBirth: String,    // YYMMDD
    val expiryDate: String      // YYMMDD
) {
    /**
     * Validate MRZ data format
     */
    fun isValid(): Boolean {
        return documentNumber.isNotEmpty() &&
                dateOfBirth.length == 6 &&
                expiryDate.length == 6 &&
                dateOfBirth.all { it.isDigit() } &&
                expiryDate.all { it.isDigit() }
    }
}

/**
 * Passport reading state
 */
sealed class PassportReadingState {
    object Idle : PassportReadingState()
    object WaitingForNfc : PassportReadingState()
    object Connecting : PassportReadingState()
    object ReadingData : PassportReadingState()
    object VerifyingAuthenticity : PassportReadingState()
    data class Success(val passportData: PassportData) : PassportReadingState()
    data class Error(val message: String) : PassportReadingState()
}
