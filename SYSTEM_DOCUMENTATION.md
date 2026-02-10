# ZK Wallet - System Documentation

## Overview

ZK Wallet is an Android Self-Sovereign Identity (SSI) and Zero-Knowledge (ZK) wallet application built with Kotlin and Jetpack Compose. The app follows a dark theme design with cyan accent colors. It enables users to create a secure local wallet, scan their passport via NFC to store credentials locally, and generate zero-knowledge proofs to verify their identity without sharing sensitive personal data.

**Important Design Principles:**
- **No Cloud Account**: This app has NO cloud account and NO blockchain storage of personal data.
- **Local Wallet**: "Create Wallet" means initializing a LOCAL wallet on the device.
- **Local PIN Authentication**: Uses a 6-digit numeric PIN as a LOCAL WALLET UNLOCK PIN (NOT a web password).
- **No Username/Email Registration**: Authentication is purely local.
- **Secure PIN Storage**: PIN is stored as a salted hash using SHA-256 (with Keystore-backed encryption recommended for production).

---

## System Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation**: Navigation Compose
- **Local Storage**: DataStore Preferences
- **Biometric**: AndroidX Biometric Library
- **NFC**: Android NFC API with JMRTD for passport reading
- **Camera**: CameraX with ML Kit for MRZ text recognition
- **Theme**: Dark theme with cyan (#00D9FF) accent color

### Project Structure
```
com.example.zk/
├── MainActivity.kt          # Main entry point with NFC handling
├── ZKApplication.kt         # Application class for language initialization
├── data/
│   └── WalletDataStore.kt   # Local data persistence
├── navigation/
│   └── AppNav.kt            # Navigation routes and graph
├── ui/
│   ├── screens/             # All UI screens
│   └── theme/               # App theming
├── util/
│   └── BiometricHelper.kt   # Biometric authentication helper
└── viewmodel/               # ViewModels for state management
```

---

## Features and Screens

### 1. Welcome Screen
**Purpose**: Entry point for new and returning users.

**Features**:
- App logo and branding display
- "Create Wallet" button for new users
- "Log in" button for existing users

**Navigation**:
- Create Wallet → Set PIN Screen
- Log in → Unlock Wallet Screen

---

### 2. Create Wallet Flow

#### 2.1 Set PIN Screen
**Purpose**: Allow users to set a 6-digit numeric PIN for their local wallet.

**Features**:
- 6-digit PIN input with visual dots
- Numeric keypad (0-9) with backspace
- Two-step process: Enter PIN → Confirm PIN
- PIN validation (must match)
- Error message if PINs don't match

**Security**:
- PIN is hashed with a random salt using SHA-256
- Salt and hash are stored in DataStore
- Plain PIN is never stored

**Navigation**:
- On successful PIN set → Scan Passport Screen

#### 2.2 Scan Passport Screen
**Purpose**: Scan user's passport to create wallet credentials.

**Features**:
- Camera view for MRZ (Machine Readable Zone) scanning
- ML Kit text recognition for automatic MRZ detection
- Manual entry option for passport details:
  - Document Number
  - Date of Birth (YYMMDD format)
  - Expiry Date (YYMMDD format)
- NFC passport reading using JMRTD library
- Real-time NFC reading status display
- Support for Malaysian passports and other ICAO-compliant passports

**Passport Data Extracted**:
- Full Name
- Nationality
- Date of Birth
- Gender
- Document Number
- Expiry Date
- Issuing Country

**Security**:
- Raw passport data is processed locally only
- Only essential derived data is stored
- Raw MRZ data is NOT stored in history or sent anywhere

**Navigation**:
- On successful scan → Wallet Created Screen

#### 2.3 Wallet Created Screen
**Purpose**: Confirm successful wallet creation.

**Features**:
- Success animation/icon
- Confirmation message
- "Continue to Home" button

**Navigation**:
- Continue → Home Screen

---

### 3. Unlock Wallet Flow

#### 3.1 Unlock Wallet Screen
**Purpose**: Authenticate existing users to access their wallet.

**Features**:
- 6-digit PIN input
- Numeric keypad
- Biometric authentication option (if enabled)
- Error message for incorrect PIN
- Fingerprint/Face unlock integration

**Security**:
- PIN is verified against stored hash
- Biometric prompt uses AndroidX BiometricPrompt
- Failed attempts are tracked

**Navigation**:
- On successful unlock → Home Screen

---

### 4. Home Screen
**Purpose**: Main dashboard after authentication.

**Features**:
- Personalized greeting ("Hello, [First Name]")
- Credential status badge (Active/Not Added)
- Identity card visual
- Generate Proof card with description and action button
- Quick action cards (Profile, Support)
- Bottom navigation bar

**Navigation**:
- Generate Proof button → Generate Proof Screen
- Profile card → Profile Screen
- Bottom nav: Home, Activity, Generate Proof, Settings

---

### 5. Generate Proof Screen
**Purpose**: Create zero-knowledge proofs for identity verification.

**Features**:
- Proof type selection
- Proof generation process
- QR code display for generated proof
- Proof sharing options

**Navigation**:
- Back → Home Screen
- Bottom navigation available

---

### 6. Activity Screen
**Purpose**: Display history of wallet activities.

**Features**:
- List of past proof generations
- Verification history
- Activity timestamps
- Activity status indicators

**Navigation**:
- Bottom navigation available

---

### 7. Profile Screen
**Purpose**: View and manage user profile information.

**Features**:
- Profile avatar with edit option
- Credential status card
- Change PIN button

**Read-Only Fields** (from passport, cannot be edited):
- Full Name (with verified checkmark)
- Passport Number (with verified checkmark)
- Nationality (with verified checkmark)
- Date of Birth (with verified checkmark)
- Gender (with verified checkmark)

**Editable Fields**:
- Contact Number

**Actions**:
- Change PIN → navigates to Change PIN screen
- Save button in top bar
- Scan Passport to Connect (if no credential)

**Visual Indicators**:
- Green checkmark icons for verified passport fields
- Lock icons indicating read-only fields
- Green border for verified fields

**Navigation**:
- Back → Previous screen
- Change PIN → Change PIN Screen
- Bottom navigation available

---

### 8. Settings Screen
**Purpose**: Configure app settings and preferences.

**Features**:
- User profile summary with avatar
- Passport validity display

**Account Section**:
- Profile → Profile Screen
- Change Password → Change PIN Screen

**Security & Preferences Section**:
- Biometric Login toggle (enable/disable fingerprint/face unlock)
- Change Language (supports 8 languages)
- FAQ

**Actions**:
- Logout button
- Delete Account button

**Navigation**:
- Profile → Profile Screen
- Change Password → Change PIN Screen
- Logout → Welcome Screen
- Bottom navigation available

---

### 9. Change PIN Screen
**Purpose**: Allow users to change their wallet PIN.

**Features**:
- Current PIN verification
- New PIN entry (6 digits)
- Confirm new PIN
- Validation and error handling

**Process**:
1. Enter current PIN
2. If correct, enter new PIN
3. Confirm new PIN
4. If match, update PIN hash in DataStore

**Navigation**:
- Back → Previous screen
- On success → Previous screen with confirmation

---

### 10. Language Settings
**Purpose**: Change app display language.

**Supported Languages**:
1. English (en) - Default
2. Chinese (zh) - 中文
3. Malay (ms) - Bahasa Melayu
4. Tamil (ta) - தமிழ்
5. Japanese (ja) - 日本語
6. Korean (ko) - 한국어
7. Spanish (es) - Español
8. French (fr) - Français

**Implementation**:
- Uses AppCompatDelegate.setApplicationLocales()
- Language preference saved to DataStore
- Restored on app startup via ZKApplication class
- All screens use stringResource() for localization

---

## Data Flow and Storage

### WalletDataStore
Central data persistence layer using DataStore Preferences.

**Stored Data**:
- `wallet_initialized`: Boolean - Whether wallet has been created
- `pin_hash`: String - SHA-256 hash of PIN
- `pin_salt`: String - Random salt for PIN hashing
- `public_key`: String - Mock key pair for wallet
- `credential_stored`: Boolean - Whether passport credential exists
- `user_name`: String - User's display name
- `biometric_enabled`: Boolean - Biometric login preference
- `language_code`: String - Selected language code

**Passport Data**:
- `passport_full_name`: String
- `passport_nationality`: String
- `passport_dob`: String
- `passport_gender`: String
- `passport_doc_number`: String
- `passport_expiry`: String
- `passport_issuing_country`: String

---

## Workflows

### Workflow 1: First-Time User (Create Wallet)
```
Welcome Screen
    ↓ [Create Wallet]
Set PIN Screen
    ↓ [Enter 6-digit PIN]
    ↓ [Confirm PIN]
Scan Passport Screen
    ↓ [Scan MRZ with camera]
    ↓ [Hold passport to NFC]
    ↓ [Read passport data]
Wallet Created Screen
    ↓ [Continue]
Home Screen
```

### Workflow 2: Returning User (Unlock Wallet)
```
Welcome Screen
    ↓ [Log in]
Unlock Wallet Screen
    ↓ [Enter 6-digit PIN] OR [Use Biometric]
Home Screen
```

### Workflow 3: Generate Proof
```
Home Screen
    ↓ [Generate Proof button]
Generate Proof Screen
    ↓ [Select proof type]
    ↓ [Generate]
    ↓ [View QR code]
    ↓ [Share/Done]
Home Screen
```

### Workflow 4: Edit Profile
```
Home Screen
    ↓ [Profile card] OR Settings → Profile
Profile Screen
    ↓ [Edit Contact Number]
    ↓ [Save]
Profile Screen (updated)
```

### Workflow 5: Change PIN
```
Settings Screen OR Profile Screen
    ↓ [Change PIN/Password]
Change PIN Screen
    ↓ [Enter current PIN]
    ↓ [Enter new PIN]
    ↓ [Confirm new PIN]
Previous Screen (with success message)
```

### Workflow 6: Enable Biometric Login
```
Settings Screen
    ↓ [Toggle Biometric Login ON]
    ↓ [System biometric prompt]
    ↓ [Authenticate with fingerprint/face]
Settings Screen (biometric enabled)
```

### Workflow 7: Change Language
```
Settings Screen
    ↓ [Change Language]
Language Selection Dialog
    ↓ [Select language]
App reloads in selected language
```

### Workflow 8: Logout
```
Settings Screen
    ↓ [Logout]
Welcome Screen
```

---

## Security Considerations

### PIN Security
- PIN is never stored in plain text
- Uses salted SHA-256 hashing
- Salt is randomly generated for each wallet
- In production, should use PBKDF2 or Argon2 for better security
- Consider Android Keystore for additional encryption

### Biometric Security
- Uses AndroidX BiometricPrompt
- Requires BIOMETRIC_STRONG authentication
- Falls back to device credential if needed
- Availability checked before enabling

### Passport Data Security
- Passport data processed locally only
- No cloud upload or blockchain storage
- Raw MRZ/NFC data not persisted
- Only derived identity fields stored
- Credential stored locally with encryption recommended

### Local Storage Security
- DataStore Preferences used for persistence
- Consider EncryptedSharedPreferences for production
- Consider Tink library for credential encryption

---

## Navigation Graph

```
welcome (Start)
    ├── set_pin
    │   └── scan_passport
    │       └── wallet_created
    │           └── home
    └── unlock_wallet
        └── home
            ├── activity
            ├── generate_proof
            ├── profile
            │   └── change_pin
            └── settings
                ├── profile
                └── change_pin
```

---

## Localization

All user-facing strings are externalized to `res/values/strings.xml` and translated in:
- `res/values-zh/strings.xml` (Chinese)
- `res/values-ms/strings.xml` (Malay)
- `res/values-ta/strings.xml` (Tamil)
- `res/values-ja/strings.xml` (Japanese)
- `res/values-ko/strings.xml` (Korean)
- `res/values-es/strings.xml` (Spanish)
- `res/values-fr/strings.xml` (French)

Per-app language is configured via:
- `res/xml/locales_config.xml`
- AndroidManifest.xml `android:localeConfig` attribute
- AppCompatDelegate.setApplicationLocales() for runtime changes

---

## Future Enhancements

1. **Enhanced Security**: Implement Keystore-backed encryption for all sensitive data
2. **Cloud Backup**: Optional encrypted backup to user's cloud storage
3. **Multiple Credentials**: Support for multiple passport/ID documents
4. **Proof History**: Detailed history of all generated proofs
5. **Verifier Mode**: Ability to verify proofs from other users
6. **Blockchain Integration**: Optional blockchain anchoring for proof verification
7. **Document Updates**: Re-scan passport when expired
8. **Export/Import**: Secure wallet export and import functionality

---

## Version Information

- **App Name**: ZK Wallet
- **Package**: com.example.zk
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Kotlin Version**: 2.2.10
- **Compose BOM**: 2024.02.00

---

*This documentation describes the ZK Wallet Android application as of the current implementation. Features and workflows may be updated in future versions.*
