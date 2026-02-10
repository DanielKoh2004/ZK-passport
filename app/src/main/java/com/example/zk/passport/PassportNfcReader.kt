package com.example.zk.passport

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.example.zk.data.MrzData
import com.example.zk.data.PassportData
import com.example.zk.data.PassportReadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import net.sf.scuba.data.Gender
import net.sf.scuba.smartcards.CardService
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.iso19794.FaceImageInfo
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream

/**
 * Service for reading passport data via NFC
 *
 * Uses JMRTD library to communicate with passport RFID chip
 * Supports BAC (Basic Access Control) authentication
 *
 * PRIVACY: All data is processed locally, never uploaded
 */
class PassportNfcReader {

    companion object {
        private const val TAG = "PassportNfcReader"
    }

    private val _readingState = MutableStateFlow<PassportReadingState>(PassportReadingState.Idle)
    val readingState: StateFlow<PassportReadingState> = _readingState

    /**
     * Read passport data from NFC tag
     *
     * @param tag NFC tag from the passport
     * @param mrzData MRZ data for BAC authentication
     * @return PassportData if successful, null otherwise
     */
    suspend fun readPassport(tag: Tag, mrzData: MrzData): PassportData? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting passport read...")
        Log.d(TAG, "MRZ Data: docNum=${mrzData.documentNumber}, dob=${mrzData.dateOfBirth}, exp=${mrzData.expiryDate}")

        try {
            _readingState.value = PassportReadingState.Connecting
            Log.d(TAG, "State: Connecting")

            // Get IsoDep interface
            val isoDep = IsoDep.get(tag)
            if (isoDep == null) {
                Log.e(TAG, "IsoDep is null - not a passport NFC tag")
                _readingState.value = PassportReadingState.Error("NFC tag is not a passport")
                return@withContext null
            }

            Log.d(TAG, "IsoDep obtained, connecting...")

            isoDep.timeout = 10000 // 10 seconds timeout
            isoDep.connect()
            Log.d(TAG, "IsoDep connected successfully")

            // Create card service
            Log.d(TAG, "Creating card service...")
            val cardService = CardService.getInstance(isoDep)
            cardService.open()
            Log.d(TAG, "Card service opened")

            // Create passport service
            Log.d(TAG, "Creating passport service...")
            val passportService = PassportService(
                cardService,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.DEFAULT_MAX_BLOCKSIZE,
                false,
                false
            )
            passportService.open()
            Log.d(TAG, "Passport service opened")

            // Try BAC authentication directly (most passports including Malaysian use BAC)
            Log.d(TAG, "Attempting BAC authentication...")
            Log.d(TAG, "BAC Key: docNum='${mrzData.documentNumber}', dob='${mrzData.dateOfBirth}', exp='${mrzData.expiryDate}'")

            try {
                passportService.sendSelectApplet(false)
                val bacKey = getBACKey(mrzData)
                passportService.doBAC(bacKey)
                Log.d(TAG, "BAC authentication successful!")
            } catch (e: Exception) {
                Log.e(TAG, "BAC authentication failed: ${e.message}", e)
                _readingState.value = PassportReadingState.Error(
                    "Authentication failed. Please check:\n" +
                    "• Document number is correct\n" +
                    "• Date of birth (YYMMDD)\n" +
                    "• Expiry date (YYMMDD)"
                )
                try { isoDep.close() } catch (ignored: Exception) {}
                return@withContext null
            }

            _readingState.value = PassportReadingState.ReadingData

            // Read DG1 (MRZ data)
            val dg1 = try {
                val dg1In = passportService.getInputStream(PassportService.EF_DG1)
                DG1File(dg1In)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read DG1: ${e.message}")
                _readingState.value = PassportReadingState.Error("Failed to read passport data")
                return@withContext null
            }

            val mrzInfo = dg1.mrzInfo

            // Read DG2 (Photo) - optional
            var photo: Bitmap? = null
            try {
                val dg2In = passportService.getInputStream(PassportService.EF_DG2)
                val dg2 = DG2File(dg2In)
                val faceInfos = dg2.faceInfos
                if (faceInfos.isNotEmpty()) {
                    val faceInfo = faceInfos[0]
                    val faceImageInfos = faceInfo.faceImageInfos
                    if (faceImageInfos.isNotEmpty()) {
                        val faceImageInfo = faceImageInfos[0]
                        photo = decodeImage(faceImageInfo)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read photo: ${e.message}")
                // Photo is optional, continue without it
            }

            _readingState.value = PassportReadingState.VerifyingAuthenticity

            // TODO: Implement Active Authentication and Passive Authentication
            // For now, we consider the passport authentic if BAC succeeded

            val passportData = PassportData(
                documentType = mrzInfo.documentCode ?: "",
                issuingCountry = mrzInfo.issuingState ?: "",
                documentNumber = mrzInfo.documentNumber ?: "",
                nationality = mrzInfo.nationality ?: "",
                dateOfBirth = mrzInfo.dateOfBirth ?: "",
                gender = when (mrzInfo.gender) {
                    Gender.MALE -> "Male"
                    Gender.FEMALE -> "Female"
                    else -> "Unknown"
                },
                expiryDate = mrzInfo.dateOfExpiry ?: "",
                lastName = mrzInfo.primaryIdentifier?.replace("<", " ")?.trim() ?: "",
                firstName = mrzInfo.secondaryIdentifier?.replace("<", " ")?.trim() ?: "",
                photo = photo,
                mrzLine1 = mrzInfo.toString().split("\n").getOrNull(0) ?: "",
                mrzLine2 = mrzInfo.toString().split("\n").getOrNull(1) ?: "",
                isAuthentic = true,
                activeAuthenticationSupported = false
            )

            _readingState.value = PassportReadingState.Success(passportData)

            // Close connections
            try {
                passportService.close()
                cardService.close()
                isoDep.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing connections: ${e.message}")
            }

            return@withContext passportData

        } catch (e: Exception) {
            Log.e(TAG, "Error reading passport: ${e.message}", e)
            _readingState.value = PassportReadingState.Error("Error reading passport: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Create BAC key from MRZ data
     */
    private fun getBACKey(mrzData: MrzData): BACKeySpec {
        return BACKey(
            mrzData.documentNumber,
            mrzData.dateOfBirth,
            mrzData.expiryDate
        )
    }

    /**
     * Decode face image from passport
     */
    private fun decodeImage(faceImageInfo: FaceImageInfo): Bitmap? {
        return try {
            val imageLength = faceImageInfo.imageLength
            val dataIn = DataInputStream(faceImageInfo.imageInputStream)
            val buffer = ByteArray(imageLength)
            dataIn.readFully(buffer, 0, imageLength)
            val inputStream: InputStream = ByteArrayInputStream(buffer, 0, imageLength)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode image: ${e.message}")
            null
        }
    }

    /**
     * Reset reading state
     */
    fun reset() {
        _readingState.value = PassportReadingState.Idle
    }

    /**
     * Set waiting for NFC state
     */
    fun waitForNfc() {
        _readingState.value = PassportReadingState.WaitingForNfc
    }
}
