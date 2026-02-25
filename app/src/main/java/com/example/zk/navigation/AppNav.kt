package com.example.zk.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.zk.LocalPassportViewModel
import com.example.zk.data.WalletDataStore
import com.example.zk.ui.screens.*
import com.example.zk.viewmodel.AuthViewModel
import com.example.zk.viewmodel.HistoryViewModel
import com.example.zk.viewmodel.HomeViewModel
import com.example.zk.viewmodel.PassportViewModel
import com.example.zk.viewmodel.ProfileViewModel
import com.example.zk.viewmodel.SettingsViewModel
import com.example.zk.viewmodel.WalletSetupViewModel

private val SplashDark = Color(0xFF0D1421)
private val SplashCyan = Color(0xFF00D9FF)

/**
 * Main Navigation for ZK Wallet App
 *
 * Production Auth Flow:
 * - splash: Auto-detects wallet state, routes to welcome or unlock_wallet
 * - welcome: First-time entry with Create Wallet / Login options
 * - set_pin: Step 1 - Set 6-digit PIN (Create Wallet flow)
 * - enrollment: Step 2 - Passport enrollment / NFC scan
 * - wallet_created: Step 3 - Success state
 * - unlock_wallet: PIN/Biometric unlock for returning users
 * - home: Main dashboard (authenticated)
 *
 * Security Features:
 * - Auto-lock when app goes to background
 * - PIN lockout after failed attempts (exponential backoff)
 * - Biometric authentication support
 * - Session-based locking with overlay (preserves navigation state)
 */
@Composable
fun AppNav() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val walletDataStore = remember { WalletDataStore(context) }

    // ==================== AUTO-LOCK ON BACKGROUND ====================
    // Screens that don't require re-authentication
    val unauthenticatedRoutes = remember {
        setOf("splash", "welcome", "set_pin", "enrollment", "wallet_created", "unlock_wallet")
    }
    var isSessionLocked by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != null && currentRoute !in unauthenticatedRoutes) {
                    isSessionLocked = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ==================== MAIN UI ====================
    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // ==================== SPLASH / AUTO-ROUTE ====================
        composable("splash") {
            var isInit by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                walletDataStore.isWalletInitialized.collect { value ->
                    isInit = value
                }
            }

            LaunchedEffect(isInit) {
                when (isInit) {
                    true -> navController.navigate("unlock_wallet") {
                        popUpTo("splash") { inclusive = true }
                    }
                    false -> navController.navigate("welcome") {
                        popUpTo("splash") { inclusive = true }
                    }
                    null -> { /* Still loading from DataStore */ }
                }
            }

            // Branded splash while loading
            SplashContent()
        }

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
            val userName by walletDataStore.userName.collectAsState(initial = "User")
            WalletCreatedScreen(
                onContinue = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userName = userName
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
                        popUpTo(0) { inclusive = true }
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
                isLockedOut = uiState.isLockedOut,
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
                hasCredential = homeState.hasCredential,
                totalProofs = homeState.totalProofs,
                successfulProofs = homeState.successfulProofs,
                lastProofTimestamp = homeState.lastProofTimestamp,
                passportExpiry = homeState.passportExpiry
            )
        }

        // Generate Proof Screen
        composable("generate_proof") {
            val homeViewModel: HomeViewModel = viewModel()
            val homeState by homeViewModel.homeState.collectAsState()

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
                },
                onNavigateToScanPassport = {
                    navController.navigate("scan_passport") {
                        launchSingleTop = true
                    }
                },
                onGenerateProof = { proofType, disclosureMask ->
                    navController.navigate("my_qr/$proofType/$disclosureMask") {
                        launchSingleTop = true
                    }
                },
                hasCredential = homeState.hasCredential
            )
        }

        // Profile Screen
        composable("profile") {
            val profileViewModel: ProfileViewModel = viewModel()
            val profileState by profileViewModel.profileState.collectAsState()

            ProfileScreen(
                onBackClick = { navController.popBackStack() },
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
                onScanPassport = {
                    navController.navigate("scan_passport") {
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
            val historyViewModel: HistoryViewModel = viewModel()
            val historyState by historyViewModel.historyState.collectAsState()

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
                },
                proofHistory = historyState.proofHistory,
                isLoading = historyState.isLoading,
                onClearHistory = { historyViewModel.clearHistory() }
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
                    navController.navigate("splash") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDeleteWallet = {
                    settingsViewModel.deleteWallet {
                        navController.navigate("splash") {
                            popUpTo(0) { inclusive = true }
                        }
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
                onDevBypassNfc = { passportViewModel.developerBypassNfc() },
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

        // ==================== ZK PROOF / QR ====================

        composable(
            route = "my_qr/{proofType}/{disclosureMask}",
            arguments = listOf(
                navArgument("proofType") { type = NavType.IntType; defaultValue = 0 },
                navArgument("disclosureMask") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val proofType = backStackEntry.arguments?.getInt("proofType") ?: 0
            val disclosureMask = backStackEntry.arguments?.getInt("disclosureMask") ?: 0
            MyQrScreen(
                proofType = proofType,
                disclosureMask = disclosureMask,
                onBack = { navController.popBackStack() }
            )
        }
    } // end NavHost

    // ==================== SESSION LOCK OVERLAY ====================
    if (isSessionLocked) {
        LockScreenOverlay(
            onUnlocked = { isSessionLocked = false }
        )
    }
    } // end Box
}

// ==================== SPLASH CONTENT ====================

@Composable
private fun SplashContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Shield icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                SplashCyan.copy(alpha = 0.3f),
                                Color(0xFF1A2332)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = SplashCyan,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ZK Passport",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Privacy-preserving digital identity",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = SplashCyan,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==================== LOCK SCREEN OVERLAY ====================

/**
 * Full-screen lock overlay that appears when the app returns from background.
 * Uses its own AuthViewModel instance to verify PIN independently.
 * Preserves the user's navigation state underneath.
 */
@Composable
private fun LockScreenOverlay(onUnlocked: () -> Unit) {
    val authViewModel: AuthViewModel = viewModel(key = "lock_overlay_auth")
    val uiState by authViewModel.uiState.collectAsState()
    val unlockPin by authViewModel.unlockPin.collectAsState()
    val isBiometricEnabled by authViewModel.isBiometricEnabled.collectAsState()
    val isBiometricAvailable = authViewModel.isBiometricAvailable

    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    // Reset state each time overlay appears
    LaunchedEffect(Unit) {
        authViewModel.resetUnlock()

        // Auto-trigger biometric after short delay
        kotlinx.coroutines.delay(300)
        val shouldShowBiometric = authViewModel.isBiometricEnabled.value &&
                authViewModel.isBiometricAvailable
        if (shouldShowBiometric && activity != null) {
            authViewModel.getBiometricHelper().authenticate(
                activity = activity,
                onSuccess = { authViewModel.unlockWithBiometric() },
                onError = { error -> authViewModel.setBiometricError(error) },
                onUsePin = { /* User chose to use PIN instead */ }
            )
        }
    }

    // Handle unlock success
    LaunchedEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) {
            authViewModel.resetUnlock()
            onUnlocked()
        }
    }

    // Block back button on lock screen
    BackHandler { /* Do nothing - can't dismiss lock screen */ }

    UnlockWalletScreen(
        onBackClick = { /* No back from lock screen */ },
        showBackButton = false,
        isLockedOut = uiState.isLockedOut,
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
                    onUsePin = { /* User chose PIN */ }
                )
            }
        }
    )
}