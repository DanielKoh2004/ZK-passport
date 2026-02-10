# ZK Wallet App - Frontend Architecture

## Overview

This is an Android SSI (Self-Sovereign Identity) / ZK (Zero-Knowledge) wallet app built with Kotlin and Jetpack Compose. The app follows a dark theme with cyan accents.

**IMPORTANT DESIGN PRINCIPLES:**
- **NO cloud account** - This is a local wallet only
- **NO blockchain storage of personal data** - Raw passport data is never uploaded
- **6-digit numeric PIN** - Used as LOCAL wallet unlock PIN (not a web password)
- **Local-first architecture** - All data stays on device
- **Real NFC passport scanning** - Reads passport chip data via NFC

---

## Passport Scanning Feature

### How It Works

The app uses **JMRTD** (Java Machine Readable Travel Documents) library to read passport NFC chips:

1. **MRZ Scanning** (Camera or Manual)
   - Scan the Machine Readable Zone (MRZ) at the bottom of passport photo page
   - Or manually enter: Document Number, Date of Birth, Expiry Date

2. **NFC Reading**
   - MRZ data is used for BAC (Basic Access Control) authentication
   - Phone communicates with passport's RFID chip
   - Extracts: Name, Nationality, DOB, Gender, Photo, Document Number

3. **Local Credential Creation**
   - Raw passport data is NEVER stored
   - Only derived credentials (hashes, commitments) are saved
   - No data is uploaded to cloud or blockchain

### Technical Components

```
app/src/main/java/com/example/zk/passport/
├── PassportNfcReader.kt    # NFC communication with passport chip
│                           # - BAC/PACE authentication
│                           # - Read DG1 (MRZ data), DG2 (Photo)
│                           # - Verify authenticity
│
└── MrzScanner.kt           # ML Kit text recognition for MRZ
                            # - Camera-based MRZ extraction
                            # - Parse document number, DOB, expiry
```

### Required Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

### Dependencies

```kotlin
// CameraX for MRZ scanning
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// ML Kit for text recognition
implementation("com.google.mlkit:text-recognition:16.0.0")

// JMRTD for NFC passport reading
implementation("org.jmrtd:jmrtd:0.7.34")
implementation("net.sf.scuba:scuba-sc-android:0.0.23")
implementation("com.madgag.spongycastle:prov:1.58.0.0")
implementation("edu.ucar:jj2000:5.4")
```

---

## Navigation Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        APP START                                │
│                     (WelcomeScreen)                             │
└─────────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┴─────────────────┐
            ▼                                   ▼
┌───────────────────────┐           ┌───────────────────────┐
│    CREATE WALLET      │           │       LOG IN          │
│    (New User)         │           │   (Existing User)     │
└───────────┬───────────┘           └───────────┬───────────┘
            │                                   │
            ▼                                   ▼
┌───────────────────────┐           ┌───────────────────────┐
│   Step 1: SetPinScreen│           │  UnlockWalletScreen   │
│   Set 6-digit PIN     │           │  Enter 6-digit PIN    │
│   + Confirm PIN       │           └───────────┬───────────┘
└───────────┬───────────┘                       │
            │                                   │
            ▼                                   │
┌───────────────────────┐                       │
│ Step 2: EnrollmentScreen                      │
│ Passport scan (mock)  │                       │
│ Local processing only │                       │
└───────────┬───────────┘                       │
            │                                   │
            ▼                                   │
┌───────────────────────┐                       │
│Step 3: WalletCreatedScreen                    │
│ Success confirmation  │                       │
└───────────┬───────────┘                       │
            │                                   │
            └───────────────┬───────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        HOME SCREEN                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ Generate    │  │   Profile   │  │  Settings   │              │
│  │ Proof       │  │             │  │             │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
└─────────┼────────────────┼────────────────┼─────────────────────┘
          │                │                │
          ▼                ▼                ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│GenerateProofScreen│  ProfileScreen  │  SettingsScreen  │
│ ZK proof creation │ - Credential    │ - Change PIN     │
│                   │   status card   │ - Logout         │
│                   │ - Change PIN    │                  │
│                   │   button        │                  │
└───────────────────┘ └───────┬───────┘ └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ ChangePinScreen │
                    │ 1. Current PIN  │
                    │ 2. New PIN      │
                    │ 3. Confirm PIN  │
                    └─────────────────┘
```

---

## Routes

| Route | Screen | Description |
|-------|--------|-------------|
| `welcome` | WelcomeScreen | Entry point with Create Wallet / Log in |
| `set_pin` | SetPinScreen | Step 1: Set 6-digit PIN |
| `enrollment` | EnrollmentScreen | Step 2: Passport enrollment (simulated) |
| `wallet_created` | WalletCreatedScreen | Step 3: Success state |
| `unlock_wallet` | UnlockWalletScreen | Login with existing PIN or biometric |
| `home` | HomeScreen | Main dashboard |
| `generate_proof` | GenerateProofScreen | ZK proof generation |
| `profile` | ProfileScreen | User profile & settings |
| `change_pin` | ChangePinScreen | Change wallet PIN |
| `activity` | HistoryScreen | Activity/history |
| `settings` | SettingsScreen | App settings (biometric toggle) |

---

## Biometric Authentication

### Overview
The app supports fingerprint and face recognition for unlocking the wallet as an alternative to PIN entry.

### How It Works

1. **Enable in Settings**
   - User navigates to Settings > Security & Preferences
   - Toggle "Biometric Login" switch
   - Only available if device has biometric hardware and user has enrolled fingerprint/face

2. **Login Flow**
   - If biometric is enabled, shows biometric prompt automatically on unlock screen
   - User can also tap fingerprint icon on number pad
   - On success: Wallet unlocks and navigates to home
   - On failure/cancel: Falls back to PIN entry

3. **Security Notes**
   - Biometric preference stored locally in DataStore
   - Uses Android BiometricPrompt API (BIOMETRIC_STRONG or BIOMETRIC_WEAK)
   - Does NOT store PIN/key in biometric - just uses it as unlock gate

### Technical Components

```
app/src/main/java/com/example/zk/util/
└── BiometricHelper.kt     # Biometric authentication helper
                           # - Check biometric availability
                           # - Show BiometricPrompt
                           # - Handle success/error/fallback callbacks
```

### Dependencies
```kotlin
implementation("androidx.biometric:biometric:1.1.0")
```

---

## Key Files

### Data Layer
```
app/src/main/java/com/example/zk/data/
└── WalletDataStore.kt     # Local wallet state using DataStore
                           # - PIN stored as salted SHA-256 hash
                           # - Credential storage (encrypted in production)
                           # - Key pair generation (mock)
```

### ViewModels
```
app/src/main/java/com/example/zk/viewmodel/
├── WalletSetupViewModel.kt  # Wallet creation flow
│                            # - PIN entry/confirmation
│                            # - Enrollment state
│                            # - Wallet initialization
│
└── AuthViewModel.kt         # Authentication operations
                             # - Wallet unlock with PIN
                             # - Change PIN flow
                             # - Session management
```

### Navigation
```
app/src/main/java/com/example/zk/navigation/
└── AppNav.kt               # Navigation Compose routes
```

### Screens
```
app/src/main/java/com/example/zk/ui/screens/
├── WelcomeScreen.kt        # Entry point
├── SetPinScreen.kt         # Set 6-digit PIN
├── EnrollmentScreen.kt     # Passport enrollment (simulated)
├── WalletCreatedScreen.kt  # Success confirmation
├── UnlockWalletScreen.kt   # Login with PIN
├── HomeScreen.kt           # Main dashboard
├── GenerateProofScreen.kt  # ZK proof generation
├── ProfileScreen.kt        # User profile
├── ChangePinScreen.kt      # Change PIN flow
├── SettingsScreen.kt       # App settings
└── HistoryScreen.kt        # Activity history
```

---

## Security Model

### PIN Storage
```
┌─────────────────────────────────────────────────────────────────┐
│                    PIN SECURITY                                 │
├─────────────────────────────────────────────────────────────────┤
│ 1. User enters 6-digit PIN                                      │
│ 2. Generate random 16-byte salt                                 │
│ 3. Hash PIN with salt using SHA-256                             │
│ 4. Store salt + hash in DataStore                               │
│ 5. NEVER store plain PIN                                        │
│                                                                 │
│ Production recommendations:                                      │
│ - Use PBKDF2 or Argon2 instead of SHA-256                       │
│ - Use Android Keystore for encryption                           │
│ - Use EncryptedSharedPreferences                                │
│ - Set hardware-backed keys if available                         │
└─────────────────────────────────────────────────────────────────┘
```

### Credential Storage
```
┌─────────────────────────────────────────────────────────────────┐
│                 CREDENTIAL STORAGE                              │
├─────────────────────────────────────────────────────────────────┤
│ • Raw passport data: NEVER stored, NEVER uploaded               │
│ • Only derived credentials (commitments, hashes) are stored     │
│ • Stored locally in encrypted DataStore                         │
│ • No blockchain storage of personal data                        │
│ • Verification history NOT written during wallet creation       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Theme

| Element | Color |
|---------|-------|
| Dark Background | `#0D1421` |
| Card Background | `#1A2332` |
| Accent Cyan | `#00D9FF` |
| Accent Green | `#4CAF50` |
| Error Red | `#FF5252` |

---

## Functional Requirements Checklist

### 1. Wallet Initialization ✅
- [x] Initialize local wallet state
- [x] Generate key pair (mock allowed)
- [x] Persist wallet state locally (DataStore)

### 2. Passport Enrollment ✅
- [x] Passport data processed locally only
- [x] Use for credential issuance (mock allowed)
- [x] Raw passport data NOT stored in history
- [x] Raw passport data NOT sent to blockchain

### 3. Credential Storage ✅
- [x] Store credential locally
- [x] Encryption support (placeholder for production)

### 4. History Rule ✅
- [x] Do NOT write verification history during wallet creation

---

## Building

```bash
# Sync Gradle
./gradlew build

# Run app
./gradlew installDebug
```

---

## Dependencies

```kotlin
// DataStore for local wallet state
implementation("androidx.datastore:datastore-preferences:1.0.0")

// ViewModel for Compose
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Navigation Compose
implementation("androidx.navigation:navigation-compose:2.7.7")
```
