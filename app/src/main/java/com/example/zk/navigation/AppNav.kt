package com.example.zk.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.zk.LocalPassportViewModel
import com.example.zk.ui.screens.*
import com.example.zk.viewmodel.AuthViewModel
import com.example.zk.viewmodel.HomeViewModel
import com.example.zk.viewmodel.PassportViewModel
import com.example.zk.viewmodel.ProfileViewModel
import com.example.zk.viewmodel.SettingsViewModel
import com.example.zk.viewmodel.WalletSetupViewModel

/**
 * Main Navigation for ZK Wallet App
 *
 * Routes:
 * - welcome: Entry point with Create Wallet / Login options
 * - set_pin: Step 1 - Set 6-digit PIN (Create Wallet flow)
 * - enrollment: Step 2 - Passport enrollment simulation
 * - wallet_created: Step 3 - Success state
 * - unlock_wallet: Login with existing PIN
 * - home: Main dashboard
 * - generate_proof: ZK proof generation
 * - profile: User profile with credential status
 * - change_pin: Change wallet PIN
 * - activity: Activity/history screen
 * - settings: App settings
 */
@Composable
fun AppNav() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        // ==================== ENTRY POINT ====================
        composable("welcome") {
            WelcomeScreen(
                onCreateWallet = {
                    navController.navigate("set_pin")
                },
                onLogin = {
                    navController.navigate("unlock_wallet")
                }
            )
        }

        // ==================== CREATE WALLET FLOW ====================

        // Step 1: Set PIN - Connected to WalletSetupViewModel
        composable("set_pin") {
            val walletSetupViewModel: WalletSetupViewModel = viewModel()
            val uiState by walletSetupViewModel.uiState.collectAsState()
            val pin by walletSetupViewModel.pin.collectAsState()
            val confirmPin by walletSetupViewModel.confirmPin.collectAsState()
            val isConfirming by walletSetupViewModel.isConfirmingPin.collectAsState()

            // Navigate when PIN is set - complete wallet creation first
            LaunchedEffect(uiState.pinSet) {
                if (uiState.pinSet && !uiState.walletCreated) {
                    // Complete wallet creation with the PIN
                    walletSetupViewModel.completeWalletCreation(
                        onSuccess = { },
                        onError = { }
                    )
                }
            }

            // Navigate after wallet is created
            LaunchedEffect(uiState.walletCreated) {
                if (uiState.walletCreated) {
                    navController.navigate("enrollment")
                }
            }

            SetPinScreen(
                onBack = {
                    walletSetupViewModel.resetPin()
                    navController.popBackStack()
                },
                onPinSet = { /* Handled by LaunchedEffect */ },
                externalPin = pin,
                externalConfirmPin = confirmPin,
                externalIsConfirming = isConfirming,
                errorMessage = uiState.errorMessage,
                onDigitClick = { digit -> walletSetupViewModel.enterPinDigit(digit) },
                onDeleteClick = { walletSetupViewModel.deletePinDigit() }
            )
        }

        // Step 2: Enrollment (Passport simulation)
        composable("enrollment") {
            EnrollmentScreen(
                onBack = { navController.popBackStack() },
                onEnrollmentComplete = {
                    navController.navigate("wallet_created")
                },
                onSkip = {
                    navController.navigate("wallet_created")
                },
                onScanPassport = {
                    navController.navigate("scan_passport")
                }
            )
        }

        // Step 3: Wallet Created Success
        composable("wallet_created") {
            WalletCreatedScreen(
                onContinue = {
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                userName = "User"
            )
        }

        // ==================== LOGIN FLOW ====================

        // Unlock wallet with PIN or Biometric
        composable("unlock_wallet") {
            val authViewModel: AuthViewModel = viewModel()
            val uiState by authViewModel.uiState.collectAsState()
            val unlockPin by authViewModel.unlockPin.collectAsState()
            val isBiometricEnabled by authViewModel.isBiometricEnabled.collectAsState()
            val isBiometricAvailable = authViewModel.isBiometricAvailable

            // Get the activity for biometric prompt
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? androidx.fragment.app.FragmentActivity

            // Track if biometric prompt has been shown this session
            var biometricPromptShown by remember { mutableStateOf(false) }

            // Auto-show biometric prompt after a short delay to allow state to load
            LaunchedEffect(Unit) {
                // Wait for DataStore to load the biometric preference
                kotlinx.coroutines.delay(300)
                
                // Re-check the current state after delay
                val shouldShowBiometric = authViewModel.isBiometricEnabled.value && 
                                         authViewModel.isBiometricAvailable
                
                if (shouldShowBiometric && !biometricPromptShown && activity != null) {
                    biometricPromptShown = true
                    authViewModel.getBiometricHelper().authenticate(
                        activity = activity,
                        onSuccess = { authViewModel.unlockWithBiometric() },
                        onError = { error -> authViewModel.setBiometricError(error) },
                        onUsePin = { /* User chose to use PIN instead */ }
                    )
                }
            }

            // Navigate to home when unlocked
            LaunchedEffect(uiState.isUnlocked) {
                if (uiState.isUnlocked) {
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            }

            UnlockWalletScreen(
                onBackClick = {
                    authViewModel.resetUnlock()
                    navController.popBackStack()
                },
                onUnlockSuccess = {
                    // This callback is only used for external state success
                    // The LaunchedEffect above handles navigation
                },
                externalPin = unlockPin,
                errorMessage = uiState.errorMessage,
                isLoading = uiState.isLoading,
                onDigitClick = { digit -> authViewModel.enterUnlockDigit(digit) },
                onDeleteClick = { authViewModel.deleteUnlockDigit() },
                isBiometricEnabled = isBiometricEnabled,
                isBiometricAvailable = isBiometricAvailable,
                onBiometricClick = {
                    if (activity != null) {
                        authViewModel.getBiometricHelper().authenticate(
                            activity = activity,
                            onSuccess = { authViewModel.unlockWithBiometric() },
                            onError = { error -> authViewModel.setBiometricError(error) },
                            onUsePin = { /* User chose to use PIN instead */ }
                        )
                    }
                }
            )
        }

        // Legacy login route - redirect to unlock_wallet for proper verification
        composable("login") {
            // Redirect to unlock_wallet which has proper ViewModel integration
            LaunchedEffect(Unit) {
                navController.navigate("unlock_wallet") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }

        // ==================== MAIN SCREENS ====================

        // Home Screen
        composable("home") {
            val homeViewModel: HomeViewModel = viewModel()
            val homeState by homeViewModel.homeState.collectAsState()

            HomeScreen(
                onNavigateToActivity = {
                    navController.navigate("activity") {
                        launchSingleTop = true
                    }
                },
                onNavigateToGenerateProof = {
                    navController.navigate("generate_proof") {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
                userName = homeState.userName,
                hasCredential = homeState.hasCredential
            )
        }

        // Generate Proof Screen
        composable("generate_proof") {
            GenerateProofScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToActivity = {
                    navController.navigate("activity") {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Profile Screen
        composable("profile") {
            val profileViewModel: ProfileViewModel = viewModel()
            val profileState by profileViewModel.profileState.collectAsState()

            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onSaveClick = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToActivity = {
                    navController.navigate("activity") {
                        launchSingleTop = true
                    }
                },
                onNavigateToGenerateProof = {
                    navController.navigate("generate_proof") {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
                },
                onNavigateToChangePin = {
                    navController.navigate("change_pin") {
                        launchSingleTop = true
                    }
                },
                // Pass passport data from ViewModel
                hasCredential = profileState.hasCredential,
                userName = profileState.fullName,
                passportFullName = profileState.fullName,
                passportNationality = profileState.nationality,
                passportDateOfBirth = profileState.dateOfBirth,
                passportGender = profileState.gender,
                passportDocNumber = profileState.documentNumber,
                passportExpiry = profileState.expiryDate,
                passportIssuingCountry = profileState.issuingCountry
            )
        }

        // Change PIN Screen
        composable("change_pin") {
            val authViewModel: AuthViewModel = viewModel()
            val uiState by authViewModel.uiState.collectAsState()
            val changePinStep by authViewModel.changePinStep.collectAsState()
            val currentPin by authViewModel.currentPin.collectAsState()
            val newPin by authViewModel.newPin.collectAsState()
            val confirmPin by authViewModel.confirmNewPin.collectAsState()

            // Map AuthViewModel step to UI step
            val externalStep = when (changePinStep) {
                com.example.zk.viewmodel.ChangePinStep.CURRENT -> ChangePinStepUI.CURRENT
                com.example.zk.viewmodel.ChangePinStep.NEW -> ChangePinStepUI.NEW
                com.example.zk.viewmodel.ChangePinStep.CONFIRM -> ChangePinStepUI.CONFIRM
            }

            // Get current PIN entry based on step
            val pinEntry = when (changePinStep) {
                com.example.zk.viewmodel.ChangePinStep.CURRENT -> currentPin
                com.example.zk.viewmodel.ChangePinStep.NEW -> newPin
                com.example.zk.viewmodel.ChangePinStep.CONFIRM -> confirmPin
            }

            ChangePinScreen(
                onBackClick = {
                    authViewModel.resetChangePin()
                    navController.popBackStack()
                },
                onPinChanged = {
                    authViewModel.resetChangePin()
                    navController.popBackStack()
                },
                externalCurrentStep = externalStep,
                externalPinEntry = pinEntry,
                errorMessage = uiState.errorMessage,
                isLoading = uiState.isLoading,
                pinChanged = uiState.pinChanged,
                onDigitClick = { digit -> authViewModel.enterChangePinDigit(digit) },
                onDeleteClick = { authViewModel.deleteChangePinDigit() }
            )
        }

        // Activity/History Screen
        composable("activity") {
            HistoryScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToGenerateProof = {
                    navController.navigate("generate_proof") {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Settings Screen
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.settingsState.collectAsState()

            SettingsScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToActivity = {
                    navController.navigate("activity") {
                        launchSingleTop = true
                    }
                },
                onNavigateToGenerateProof = {
                    navController.navigate("generate_proof") {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
                onNavigateToChangePassword = {
                    navController.navigate("change_pin") {
                        launchSingleTop = true
                    }
                },
                onLogout = {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userName = settingsState.userName,
                passportExpiry = settingsState.passportExpiry,
                hasCredential = settingsState.hasCredential,
                isBiometricEnabled = settingsState.isBiometricEnabled,
                isBiometricAvailable = settingsState.isBiometricAvailable,
                biometricUnavailableReason = settingsState.biometricUnavailableReason,
                onBiometricToggle = { enabled ->
                    settingsViewModel.setBiometricEnabled(enabled)
                },
                currentLanguage = settingsState.currentLanguage,
                onLanguageChange = { languageCode ->
                    settingsViewModel.setLanguage(languageCode)
                }
            )
        }

        // Legacy change_password route (redirect to change_pin)
        composable("change_password") {
            ChangePasswordScreen(
                onBackClick = { navController.popBackStack() },
                onUpdatePassword = { _, _ -> navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToActivity = {
                    navController.navigate("activity") {
                        launchSingleTop = true
                    }
                },
                onNavigateToGenerateProof = {
                    navController.navigate("generate_proof") {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
                }
            )
        }

        // ==================== PASSPORT SCANNING FLOW ====================

        composable("scan_passport") {
            // Use the shared PassportViewModel from MainActivity
            val passportViewModel = LocalPassportViewModel.current
            val uiState by passportViewModel.uiState.collectAsState()
            val readingState by passportViewModel.readingState.collectAsState()
            val passportData by passportViewModel.passportData.collectAsState()
            val documentNumber by passportViewModel.documentNumber.collectAsState()
            val dateOfBirth by passportViewModel.dateOfBirth.collectAsState()
            val expiryDate by passportViewModel.expiryDate.collectAsState()

            ScanPassportScreen(
                onBack = {
                    passportViewModel.reset()
                    navController.popBackStack()
                },
                onCapture = {
                    // Passport successfully read, go to wallet created
                    navController.navigate("wallet_created") {
                        popUpTo("enrollment") { inclusive = true }
                    }
                },
                onGallery = { /* Not used */ },
                onFlash = { /* Not used */ },
                uiState = uiState,
                readingState = readingState,
                passportData = passportData,
                documentNumber = documentNumber,
                dateOfBirth = dateOfBirth,
                expiryDate = expiryDate,
                onDocumentNumberChange = { passportViewModel.updateDocumentNumber(it) },
                onDateOfBirthChange = { passportViewModel.updateDateOfBirth(it) },
                onExpiryDateChange = { passportViewModel.updateExpiryDate(it) },
                onConfirmMrz = { passportViewModel.confirmMrzData() },
                onEnterManually = { passportViewModel.enterMrzManually() },
                onBackToScan = { passportViewModel.backToMrzScan() },
                onRetryNfc = { passportViewModel.retryNfcScan() },
                onComplete = {
                    navController.navigate("wallet_created") {
                        popUpTo("enrollment") { inclusive = true }
                    }
                }
            )
        }

        composable("face_verification") {
            FaceVerificationScreen(
                onBack = { navController.popBackStack() },
                onCapture = {
                    // Face verification complete - go to wallet created
                    navController.navigate("wallet_created") {
                        // Clear the passport scan stack
                        popUpTo("enrollment") { inclusive = true }
                    }
                },
                onHelp = { /* TODO: Show help */ },
                onSwitch = { /* TODO: Switch camera */ }
            )
        }
    }
}
