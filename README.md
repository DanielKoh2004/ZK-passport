<div align="center">

<a href="https://github.com/DanielKoh2004/ZK-passport">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:070B14,45:102A43,100:00D9FF&height=260&section=header&text=ZK%20PASSPORT&fontSize=64&fontColor=FFFFFF&animation=fadeIn&fontAlignY=40&desc=Prove%20what%20matters.%20Reveal%20what%27s%20necessary.&descAlignY=64&descSize=18" width="100%"/>
</a>

<br>

### 🔐 **Privacy-Preserving Digital Identity**

**An Android self-sovereign identity wallet for passport credentials and zero-knowledge proofs.**

<br>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=android&logoColor=white)](https://developer.android.com/compose)
[![Android](https://img.shields.io/badge/Android-SDK%2035-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![ZK](https://img.shields.io/badge/Cryptography-zkSNARK-00D9FF)](#)
[![NFC](https://img.shields.io/badge/Passport-NFC%20%2B%20MRZ-111827)](#)
[![SSI](https://img.shields.io/badge/Identity-SSI-8B5CF6)](#)

<br><br>

> **Your passport contains a lot of information. A verifier usually needs very little of it.**
>
> ZK Passport explores how to turn a physical passport into a local digital credential and prove selected claims without unnecessarily exposing the underlying identity data.

<br>

</div>

---

## 🪪 What Is ZK Passport?

A passport is one of the most information-dense identity documents a person carries.

But identity verification rarely needs everything inside it.

A service may only need to know:

```text
Are you over 18?
Are you a Malaysian citizen?
Is this credential valid?
```

Instead of:

```text
Full Name
Passport Number
Date of Birth
Nationality
Expiry Date
Gender
```

ZK Passport is built around the idea that the wallet should let a person **prove a claim without unnecessarily revealing the source data**.

```text
Physical Passport
        ↓
    MRZ + NFC
        ↓
 Local Credential
        ↓
 Selective Disclosure
        ↓
  Zero-Knowledge Proof
        ↓
      QR Code
        ↓
     Verifier
        ↓
   ✅ Claim Verified
```

### **Prove the fact. Keep the rest private.**

---

# 🌐 Why Zero-Knowledge Identity?

Traditional digital identity often looks like this:

```text
IDENTITY
   ↓
UPLOAD DOCUMENT
   ↓
REVEAL EVERYTHING
   ↓
VERIFY
```

ZK identity aims for:

```text
IDENTITY
   ↓
PROVE A CLAIM
   ↓
REVEAL ONLY WHAT IS NECESSARY
   ↓
VERIFY
```

That shift is the core idea behind this project.

---

# ✨ The Four-Step Experience

<table>
<tr>
<td width="25%" align="center">

### 📷
## SCAN

MRZ + NFC

</td>
<td width="25%" align="center">

### 🪪
## STORE

Local credential

</td>
<td width="25%" align="center">

### 🧮
## PROVE

ZK / selective disclosure

</td>
<td width="25%" align="center">

### ✅
## VERIFY

Claim only

</td>
</tr>
</table>

---

# 🧠 System Architecture

```text
┌──────────────────────────────────────────────────────────────┐
│                         CITIZEN                             │
│                                                              │
│                  Android ZK Wallet                          │
│                                                              │
│  Passport → Credential → ZK Proof → QR                     │
└────────────────────────────┬─────────────────────────────────┘
                             │
                             │ QR / proof payload
                             ▼
┌──────────────────────────────────────────────────────────────┐
│                         VERIFIER                             │
│                                                              │
│              Separate verifier application                  │
│                                                              │
│        Scan → Verify → Present verification result          │
└────────────────────────────┬─────────────────────────────────┘
                             │
                             │ Optional session status
                             ▼
┌──────────────────────────────────────────────────────────────┐
│                     ISSUER BACKEND                          │
│                                                              │
│ Credential issuance · Government public key · Session state │
└──────────────────────────────────────────────────────────────┘
```

The repository contains the Android **prover wallet** and a companion Node.js **issuer backend**. The verifier is maintained as a separate application.

---

# 🔐 Privacy-First Data Flow

```text
                     PASSPORT
                         │
             ┌───────────┴───────────┐
             ▼                       ▼
        📷 CAMERA                 📡 NFC
          MRZ OCR              ePassport Read
             │                       │
             └───────────┬───────────┘
                         ▼
                Passport Processing
                         │
                         ▼
               Derived Identity Data
                         │
                         ▼
              Verifiable Credential
                         │
                         ▼
                  LOCAL WALLET
                         │
                         ▼
               SELECTIVE DISCLOSURE
                         │
                         ▼
                ZERO-KNOWLEDGE PROOF
                         │
                         ▼
                       QR CODE
                         │
                         ▼
                     VERIFIER
```

### Local by design

The current architecture does **not** use a cloud account or blockchain storage for personal data. The wallet is designed as a local device wallet.

---

# 📷 Passport Ingestion

## MRZ Scanning

The wallet uses **CameraX + ML Kit text recognition** to detect and process the Machine Readable Zone of a passport.

## NFC Reading

For supported e-passports, the application uses Android NFC capabilities together with **JMRTD** to read passport data.

The application is designed to derive:

```text
Full Name
Nationality
Date of Birth
Gender
Document Number
Expiry Date
Issuing Country
```

Raw MRZ/NFC material is not intended to be kept as an activity history; the wallet stores processed identity fields locally.

---

# 🧾 Credential Layer

The companion issuer service builds a passport credential in a W3C Verifiable Credential-style format.

Conceptually:

```json
{
  "type": [
    "VerifiableCredential",
    "PassportCredential"
  ],
  "issuer": {
    "id": "did:gov:passport-authority"
  },
  "credentialSubject": {
    "id": "did:example:citizen",
    "name": "...",
    "dateOfBirth": "...",
    "passportNumber": "...",
    "nationality": "..."
  }
}
```

The current issuer implementation generates an **Ed25519** key pair at startup and signs issued credentials with the issuer private key.

---

# 🧮 Zero-Knowledge Proofs

The central demonstration is that a verifier can receive a proof of a statement rather than the underlying private value.

### Example — Age Proof

```text
PRIVATE
────────────────────────────
Date of Birth = 2004-XX-XX

            │
            ▼

ZK CIRCUIT
────────────────────────────
Does DOB satisfy Age ≥ 18?

            │
            ▼

PUBLIC RESULT
────────────────────────────
✅ Holder is 18 or older
```

The Android app includes a bundled zkSNARK proving key:

```text
app/src/main/assets/passport_final.zkey
```

---

# 🎯 Proof Templates

The current wallet exposes proof flows around:

| Proof | Purpose |
|---|---|
| **Age ≥ 18** | Prove adulthood without exposing exact DOB |
| **Nationality** | Prove a nationality claim |
| **Credential Validity** | Prove the credential satisfies the required validity condition |

Selective-disclosure controls are also available for identity attributes such as:

```text
Photo
Name
Nationality
Gender
```

---

# 📱 Android Wallet

The prover application is built using **Kotlin + Jetpack Compose + Material 3**.

### Wallet lifecycle

```text
WELCOME
   │
   ├───────────────┐
   ▼               ▼
CREATE WALLET    UNLOCK WALLET
   │               │
   ▼               ▼
SET 6-DIGIT PIN   BIOMETRIC / PIN
   │               │
   └───────┬───────┘
           ▼
          HOME
           │
     ┌─────┼─────────────┐
     ▼     ▼             ▼
 PROFILE ACTIVITY   GENERATE PROOF
                       │
                       ▼
                    QR CODE
```

### Main screens

```text
🏠 Home
📜 Activity
🧮 Generate Proof
👤 Profile
⚙️ Settings
🔑 Change PIN
🌍 Language
```

---

# 🔒 Local Wallet Security

The wallet currently uses a local authentication model.

```text
6-digit PIN
     +
Random salt
     +
SHA-256 hash
     +
Optional biometric authentication
     +
Auto-lock behaviour
```

The wallet never persists the plaintext PIN.

For a production identity wallet, the repository recommends stronger protections such as:

```text
Android Keystore-backed encryption
Stronger password derivation
Hardware-backed protection where available
Encrypted credential storage
Hardened issuer authentication
TLS-secured deployment
```

This distinction is intentional: **the repository demonstrates the architecture; production identity infrastructure requires significantly more hardening.**

---

# 🏛️ Issuer Backend

The issuer backend is a small **Node.js + Express** service.

### Credential issuance

```text
POST /api/issue-passport
```

```text
Passport / identity payload
           ↓
       Validation
           ↓
      Build VC object
           ↓
       Ed25519 sign
           ↓
      Signed credential
```

### Public key

```text
GET /api/public-key
```

Returns the issuer's Ed25519 public key.

### Verification session status

```text
POST /api/session/:nonce/status
GET  /api/session/:nonce/status
```

Session entries are short-lived and automatically expire after 60 seconds in the current implementation.

---

# 🔄 End-to-End Scenario

### 01 — Create the wallet

```text
Install app
   ↓
Create Wallet
   ↓
Set PIN
```

### 02 — Read the passport

```text
Camera → MRZ
        +
NFC → ePassport
        ↓
Derived identity data
```

### 03 — Issue the credential

```text
Identity data
     ↓
Issuer backend
     ↓
Signed Verifiable Credential
     ↓
Stored locally
```

### 04 — Choose what to prove

```text
Age ≥ 18
Nationality
Credential Validity
```

### 05 — Generate the proof

```text
Private credential
        ↓
ZK circuit
        ↓
Proof
        ↓
QR code
```

### 06 — Verify

```text
Verifier scans QR
        ↓
Proof verification
        ↓
✅ Valid claim
```

---

# 🧩 Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Mobile | **Kotlin** | Android application |
| UI | **Jetpack Compose + Material 3** | Modern wallet UI |
| Navigation | **Navigation Compose** | Single-activity navigation |
| Local State | **DataStore Preferences** | Wallet persistence |
| Authentication | **AndroidX Biometric** | Fingerprint / face unlock |
| Camera | **CameraX** | MRZ capture |
| OCR | **ML Kit** | MRZ text recognition |
| NFC | **JMRTD** | ePassport reading |
| Networking | **Retrofit + OkHttp** | Issuer API communication |
| QR | **ZXing** | Proof QR generation |
| Issuer | **Node.js + Express** | Credential issuance service |
| Crypto | **Ed25519** | Credential signatures |
| ZK | **zkSNARK / Circom artifacts** | Privacy-preserving proofs |

---

# 📂 Repository Structure

```text
ZK-passport/
│
├── app/
│   ├── src/main/java/com/example/zk/
│   │   ├── data/
│   │   │   └── WalletDataStore.kt
│   │   ├── navigation/
│   │   │   └── AppNav.kt
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   └── theme/
│   │   ├── util/
│   │   │   └── BiometricHelper.kt
│   │   └── viewmodel/
│   │
│   └── src/main/assets/
│       └── passport_final.zkey
│
├── issuer-backend/
│   └── index.js
│
├── SYSTEM_DOCUMENTATION.md
├── FRONTEND_WORKFLOW.md
└── README.md
```

---

# 🛠️ Getting Started

## Prerequisites

```text
Android Studio
Android SDK 35
Java / JDK
Node.js 18+
npm
```

The Android project targets **SDK 35** and supports devices from **min SDK 24** upward.

---

## 1. Clone

```bash
git clone https://github.com/DanielKoh2004/ZK-passport.git
cd ZK-passport
```

---

## 2. Check the proving key

Make sure this file exists:

```text
app/src/main/assets/passport_final.zkey
```

---

## 3. Build the Android app

### Windows

```powershell
.\gradlew.bat assembleDebug
```

### macOS / Linux

```bash
./gradlew assembleDebug
```

Then open the project in Android Studio and run the `app` configuration.

For real passport flows, use an Android device with NFC support.

---

## 4. Start the issuer

```bash
cd issuer-backend
npm install
node index.js
```

The issuer listens on port `3000` by default unless `PORT` is configured.

---

# 🌍 Localization

The wallet currently supports eight languages:

```text
🇬🇧 English
🇨🇳 中文
🇲🇾 Bahasa Melayu
🇮🇳 தமிழ்
🇯🇵 日本語
🇰🇷 한국어
🇪🇸 Español
🇫🇷 Français
```

Language preferences are persisted locally and applied through Android's locale APIs.

---

# 🎨 Visual Showcase

The wallet uses a dark interface with cyan trust signals and Material 3 components.

When adding screenshots to the repository, a clean product strip works especially well:

```text
/docs/images/
├── welcome.png
├── home.png
├── passport-scan.png
├── proof-selection.png
├── zk-proof.png
└── verification.png
```

Then embed them in the README:

```markdown
<p align="center">
  <img src="docs/images/home.png" width="30%">
  <img src="docs/images/passport-scan.png" width="30%">
  <img src="docs/images/zk-proof.png" width="30%">
</p>
```

Actual app screenshots are recommended over generic passport stock images because they show the system rather than merely the concept.

---

# 🔭 Future Direction

## Multi-Credential Wallet

Expand beyond passports:

```text
Passport
   +
National ID
   +
Driver's License
   +
University Credential
   +
Professional Certificate
```

## Trust Registry

Establish a verifiable relationship between credential issuers, public keys and verifiers.

```text
Issuer
  ↓
Trusted Registry
  ↓
Public Key / Status
  ↓
Verifier
```

## Offline Verification

A future architecture can move more of the verification experience toward local, offline-capable exchange:

```text
Wallet
  ↓
QR / NFC
  ↓
Verifier
  ↓
Local verification
```

## Richer ZK Claims

Move from simple claims toward more expressive predicates such as:

```text
Age ≥ 18
Age within a range
Passport not expired
Nationality = X
Credential issued by trusted authority
```

---

# 🧭 The Bigger Idea

The project is not really about making a digital passport.

It is about changing the primitive used for identity verification.

```text
OLD
────────────────────────────
"Show me your document."
           ↓
     Reveal everything
```

versus:

```text
NEW
────────────────────────────
"Prove the claim."
           ↓
   Reveal what is necessary
```

### **Identity should be yours. Verification should be minimal.**

---

# 📚 Documentation

For deeper implementation details:

- [`SYSTEM_DOCUMENTATION.md`](./SYSTEM_DOCUMENTATION.md) — system architecture, screens, data flow and security considerations
- [`FRONTEND_WORKFLOW.md`](./FRONTEND_WORKFLOW.md) — UI and user-flow documentation

---

# ⚠️ Security & Prototype Disclaimer

This repository is a **research / prototype implementation**.

It should not be treated as a production government identity system or production passport wallet without substantial additional work in areas including:

```text
Cryptographic review
Secure key management
Hardware-backed storage
Issuer trust infrastructure
Credential revocation
Production authentication
Transport security
Regulatory / compliance requirements
```

The current repository documentation explicitly identifies these areas as production-hardening requirements.

---

<div align="center">

<br>

### **Privacy is not hiding who you are.**
### **It is choosing what you need to prove.**

<br>

**ZK Passport**

*An experimental self-sovereign identity wallet powered by zero-knowledge proofs.*

<br><br>

<a href="https://github.com/DanielKoh2004/ZK-passport">View Repository →</a>

<br><br>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:00D9FF,100:070B14&height=120&section=footer" width="100%"/>

</div>
