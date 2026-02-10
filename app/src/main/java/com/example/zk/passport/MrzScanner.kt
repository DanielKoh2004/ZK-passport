package com.example.zk.passport

import android.util.Log
import com.example.zk.data.MrzData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * MRZ (Machine Readable Zone) Scanner using ML Kit
 *
 * Extracts document number, date of birth, and expiry date from passport MRZ
 * This data is required to authenticate with the passport NFC chip
 */
class MrzScanner {

    companion object {
        private const val TAG = "MrzScanner"

        // MRZ line patterns
        // TD3 (Passport): 2 lines of 44 characters each
        // Line 1: P<ISOCOUNTRY<<LASTNAME<<FIRSTNAME<<<<<<<<<<<<
        // Line 2: DOCNUMBER<CHECK<<YYMMDD<CHECK<GENDER<YYMMDD<CHECK<NATIONALITY<<<<<<<<CHECK

        private val MRZ_LINE_1_PATTERN = Regex("^[A-Z<]{44}$")
        private val MRZ_LINE_2_PATTERN = Regex("^[A-Z0-9<]{44}$")

        // Simplified pattern for detection
        private val MRZ_CHAR_PATTERN = Regex("[A-Z0-9<]+")
    }

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Scan image for MRZ data
     */
    suspend fun scanMrz(image: InputImage): MrzData? = suspendCancellableCoroutine { continuation ->
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val mrzData = extractMrzData(visionText.text)
                continuation.resume(mrzData)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed: ${e.message}")
                continuation.resume(null)
            }
    }

    /**
     * Extract MRZ data from recognized text
     */
    private fun extractMrzData(text: String): MrzData? {
        Log.d(TAG, "Recognized text: $text")

        // Split into lines and clean up
        val lines = text.split("\n")
            .map { it.replace(" ", "").replace("«", "<").uppercase() }
            .filter { it.length >= 30 } // MRZ lines are at least 30 chars

        Log.d(TAG, "Cleaned lines: $lines")

        // Find MRZ lines (look for patterns)
        var mrzLine1: String? = null
        var mrzLine2: String? = null

        for (i in lines.indices) {
            val line = lines[i]

            // Check if this looks like MRZ line 1 (starts with P< for passport)
            if (line.startsWith("P<") || line.startsWith("P0") || line.contains("<<")) {
                mrzLine1 = padOrTrimToLength(line, 44)

                // Next line should be MRZ line 2
                if (i + 1 < lines.size) {
                    mrzLine2 = padOrTrimToLength(lines[i + 1], 44)
                }
                break
            }

            // Alternative: look for line with document number pattern
            if (line.length >= 44 && line.matches(MRZ_LINE_2_PATTERN)) {
                mrzLine2 = line
                if (i > 0) {
                    mrzLine1 = padOrTrimToLength(lines[i - 1], 44)
                }
                break
            }
        }

        if (mrzLine2 == null || mrzLine2.length < 44) {
            Log.w(TAG, "Could not find valid MRZ data")
            return null
        }

        // Extract data from MRZ line 2
        // Format: DOCNUMBER<CHECK<<YYMMDD<CHECK<GENDER<YYMMDD<CHECK<...
        // Positions: 0-8 = doc number, 9 = check, 13-18 = DOB, 19 = check, 21-26 = expiry, 27 = check

        try {
            val documentNumber = extractDocumentNumber(mrzLine2)
            val dateOfBirth = mrzLine2.substring(13, 19).replace("<", "0")
            val expiryDate = mrzLine2.substring(21, 27).replace("<", "0")

            // Validate extracted data
            if (documentNumber.isEmpty()) {
                Log.w(TAG, "Invalid document number")
                return null
            }

            if (!isValidDate(dateOfBirth) || !isValidDate(expiryDate)) {
                Log.w(TAG, "Invalid dates: DOB=$dateOfBirth, Expiry=$expiryDate")
                return null
            }

            Log.d(TAG, "Extracted MRZ: docNum=$documentNumber, dob=$dateOfBirth, exp=$expiryDate")

            return MrzData(
                documentNumber = documentNumber,
                dateOfBirth = dateOfBirth,
                expiryDate = expiryDate
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse MRZ: ${e.message}")
            return null
        }
    }

    /**
     * Extract document number from MRZ line 2
     * Document number is in positions 0-8, followed by check digit at position 9
     */
    private fun extractDocumentNumber(line: String): String {
        val docNumberSection = line.substring(0, 9)
        // Remove trailing < characters
        return docNumberSection.replace("<", "").trim()
    }

    /**
     * Validate date format (YYMMDD)
     */
    private fun isValidDate(date: String): Boolean {
        if (date.length != 6) return false
        if (!date.all { it.isDigit() }) return false

        val month = date.substring(2, 4).toIntOrNull() ?: return false
        val day = date.substring(4, 6).toIntOrNull() ?: return false

        return month in 1..12 && day in 1..31
    }

    /**
     * Pad or trim string to specified length
     */
    private fun padOrTrimToLength(str: String, length: Int): String {
        return when {
            str.length > length -> str.substring(0, length)
            str.length < length -> str.padEnd(length, '<')
            else -> str
        }
    }

    /**
     * Manually create MRZ data (for when camera scanning fails)
     *
     * For Malaysian passports:
     * - Document number format: A12345678 (1 letter + 8 digits) or similar
     * - Make sure to include the check digit if present in the MRZ
     */
    fun createMrzData(
        documentNumber: String,
        dateOfBirth: String,  // YYMMDD
        expiryDate: String    // YYMMDD
    ): MrzData? {
        // Clean up the document number
        var cleanDocNum = documentNumber.uppercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("O", "0") // Common OCR mistake

        // Clean up dates
        val cleanDob = dateOfBirth.replace("/", "").replace("-", "").replace(" ", "")
        val cleanExp = expiryDate.replace("/", "").replace("-", "").replace(" ", "")

        Log.d(TAG, "Creating MRZ data:")
        Log.d(TAG, "  Document Number: '$cleanDocNum' (length: ${cleanDocNum.length})")
        Log.d(TAG, "  Date of Birth: '$cleanDob' (length: ${cleanDob.length})")
        Log.d(TAG, "  Expiry Date: '$cleanExp' (length: ${cleanExp.length})")

        val mrzData = MrzData(
            documentNumber = cleanDocNum,
            dateOfBirth = cleanDob,
            expiryDate = cleanExp
        )

        val isValid = mrzData.isValid()
        Log.d(TAG, "MRZ data valid: $isValid")

        if (!isValid) {
            Log.e(TAG, "MRZ validation failed:")
            if (cleanDocNum.isEmpty()) Log.e(TAG, "  - Document number is empty")
            if (cleanDob.length != 6) Log.e(TAG, "  - DOB length is ${cleanDob.length}, expected 6")
            if (cleanExp.length != 6) Log.e(TAG, "  - Expiry length is ${cleanExp.length}, expected 6")
            if (!cleanDob.all { it.isDigit() }) Log.e(TAG, "  - DOB contains non-digit characters")
            if (!cleanExp.all { it.isDigit() }) Log.e(TAG, "  - Expiry contains non-digit characters")
        }

        return if (isValid) mrzData else null
    }

    /**
     * Close resources
     */
    fun close() {
        textRecognizer.close()
    }
}
