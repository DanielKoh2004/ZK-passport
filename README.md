
ZK Passport – Prover App & Issuer Backend
=========================================

This project contains the main **ZK Passport** prover Android app and the accompanying **issuer backend** used to issue credentials and generate proofs.

The prover app is an Android wallet that:

- Creates a local wallet protected by a 6‑digit PIN (with lockout + biometric login).
- Scans a passport (MRZ + NFC), derives minimal data, and stores only processed fields locally.
- Generates zero‑knowledge proofs (ZKPs) on‑device using a Circom/zkSNARK circuit (`passport_final.zkey`).
- Shows QR codes that can be verified by a separate Verifier app (in a different repo).

The issuer backend is a small Node/Express service that simulates an issuer for credential generation.


Project Structure
-----------------

- `app/` – Android prover app (Jetpack Compose, Material 3)
	- `app/src/main/java/com/example/zk/` – App code
		- `ui/` – Screens and theming
		- `viewmodel/` – ViewModels (wallet setup, auth, home, settings, history, profile, passport)
		- `data/WalletDataStore.kt` – DataStore wrapper for PIN, passport data, credentials, proof history, preferences
		- `navigation/AppNav.kt` – Single‑activity navigation graph and session lock overlay
	- `app/src/main/assets/passport_final.zkey` – zkSNARK proving key used by the prover
- `issuer-backend/` – Node.js backend (Express) for issuing credentials
- `SYSTEM_DOCUMENTATION.md` – High‑level system design
- `FRONTEND_WORKFLOW.md` – UI/UX and flow notes


Prerequisites
-------------

- Android Studio (latest stable) with:
	- Android SDK 35
	- Kotlin/Compose support
- Java 17 (recommended for Gradle 9.x)
- Node.js 18+ and npm (for the issuer backend)


Android Prover App
------------------

**Key features**

- **Production‑grade auth**
	- Splash screen auto‑routes: first‑time users → welcome, returning users → unlock.
	- PIN lockout with exponential backoff and clear error copy.
	- Optional biometric unlock (fingerprint/face) with explicit enable/disable and device checks.
	- Auto‑lock when the app goes to background, with an overlay lock screen that preserves navigation state.

- **Passport & credential handling**
	- MRZ scan + NFC read to derive:
		- Full name, nationality, date of birth, gender, document number, expiry, issuing country.
	- Only derived fields stored locally in `WalletDataStore` – no raw passport image/bytes are persisted.
	- A boolean `hasCredential` flag gates any proof generation.

- **Home screen (dashboard)**
	- Shows user initial avatar and basic identity summary.
	- Proof stats card: total proofs, successful proofs, last activity, and passport expiry status.
	- Primary actions:
		- Generate Proof (disabled until a credential exists).
		- Activity (history), Settings, Profile.

- **Profile screen**
	- Read‑only view of passport‑derived data (name, passport number, nationality, DOB, gender).
	- Clear credential status card (active/not added).
	- “Change PIN” action wired to the change‑PIN flow.
	- Optional “Scan Passport” call‑to‑action when no credential is present.

- **Settings screen**
	- Language selection (using `AppCompatDelegate` + DataStore), with preconfigured locales.
	- Biometric login toggle with device‑capability checks and user‑friendly error messages.
	- Profile navigation + link to the change‑PIN flow.
	- Logout button with confirmation dialog (returns to splash/unlock).
	- Delete Wallet button with a strong confirmation dialog that calls `clearWallet()` and resets to splash.
	- FAQ dialog explaining ZK proofs, storage guarantees, and deletion semantics.

- **History screen (Activity)**
	- Backed by `HistoryViewModel` reading `WalletDataStore.proofHistory`.
	- Summary cards for total/success/failed proofs.
	- Filter chips (All / Success / Failed).
	- Proof cards show label, timestamp, success badge, proof size, and disclosure details.
	- “Clear history” action with confirmation, implemented via `clearProofHistory()`.

- **Generate Proof screen**
	- Template selection:
		- Prove Age ≥ 18
		- Prove Nationality
		- Prove Credential Valid
	- Selective disclosure toggles (photo, name, nationality, gender) encoded into a bitmask.
	- If **no credential**:
		- Top warning banner explaining that a passport must be scanned first.
		- “Scan Passport” button navigating to the scanning flow.
		- “Generate Proof” button disabled with “Credential Required” label.


Running the Android App
------------------------

From the project root:

1. Ensure the zkSNARK proving key exists:

	 - `app/src/main/assets/passport_final.zkey`
	 - If you regenerated it elsewhere, copy it into `app/src/main/assets/`.

2. Build from the command line (optional, Android Studio can also handle this):

	 - `./gradlew assembleDebug` (macOS/Linux)
	 - `.\u0067radlew.bat assembleDebug` (Windows)

3. Open the project in Android Studio and run the **app** configuration on a device/emulator that supports NFC (for real NFC flows) or use the dev bypass path if configured.


Issuer Backend (Node.js)
------------------------

The issuer backend is a minimal Express server that issues credentials and/or participates in proof generation flows for the prover app.

### Setup

From the project root:

```bash
cd issuer-backend
npm install
```

### Running

```bash
cd issuer-backend
node index.js
```

By default, the server typically listens on `http://localhost:3000` (check `issuer-backend/index.js` for the exact port and routes). The Android app should be configured to point to this backend when issuing credentials.


Security Notes
--------------

- PINs are stored as **salted SHA‑256 hashes** in `WalletDataStore`; plaintext PINs are never persisted.
- In a production deployment you should:
	- Use Android Keystore‑backed keys for encrypting credential blobs.
	- Consider hardening the DataStore and integrating OS‑level encryption and hardware‑backed attestation.
	- Host the issuer backend behind TLS and strong authentication.
- All ZK and credential flows in this repo are for demonstration/prototyping and should be reviewed before real‑world use.


Related Repos
-------------

- **Verifier app** (separate Android project) scans the QR codes generated by this prover app and verifies the ZK proof.


Troubleshooting
---------------

- **Gradle deprecation warnings** – The project currently builds against Gradle 9.x and may emit deprecation warnings. These do not affect functionality but should be addressed before upgrading Gradle major versions.
- **Issuer backend not running** – Make sure `node index.js` is started from the `issuer-backend/` directory, and that the Android app’s base URL matches the backend URL (especially when testing on device vs emulator).

