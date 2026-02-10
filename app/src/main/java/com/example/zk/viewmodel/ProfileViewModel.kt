package com.example.zk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zk.data.WalletDataStore
import kotlinx.coroutines.flow.*

/**
 * ViewModel for Profile screen
 * Loads and manages user profile data from passport scan
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val walletDataStore = WalletDataStore(application)

    // Combined profile state
    data class ProfileState(
        val fullName: String = "",
        val nationality: String = "",
        val dateOfBirth: String = "",
        val gender: String = "",
        val documentNumber: String = "",
        val expiryDate: String = "",
        val issuingCountry: String = "",
        val hasCredential: Boolean = false
    )

    // Use nested combines to work around the 5-parameter limit
    private val personalInfo: Flow<Triple<String, String, String>> = combine(
        walletDataStore.passportFullName,
        walletDataStore.passportNationality,
        walletDataStore.passportDateOfBirth
    ) { name, nationality, dob ->
        Triple(name, nationality, dob)
    }

    private val documentInfo: Flow<Triple<String, String, String>> = combine(
        walletDataStore.passportGender,
        walletDataStore.passportDocNumber,
        walletDataStore.passportExpiry
    ) { gender, docNum, expiry ->
        Triple(gender, docNum, expiry)
    }

    private val additionalInfo: Flow<Pair<String, Boolean>> = combine(
        walletDataStore.passportIssuingCountry,
        walletDataStore.hasCredential
    ) { country, hasCred ->
        Pair(country, hasCred)
    }

    val profileState: StateFlow<ProfileState> = combine(
        personalInfo,
        documentInfo,
        additionalInfo
    ) { personal, document, additional ->
        ProfileState(
            fullName = personal.first,
            nationality = personal.second,
            dateOfBirth = personal.third,
            gender = document.first,
            documentNumber = document.second,
            expiryDate = document.third,
            issuingCountry = additional.first,
            hasCredential = additional.second
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProfileState()
    )
}
