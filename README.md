# Zero-Cloud Vault

> **100% Air-gapped biometric cryptographic vault with AES-256-GCM hardware security.**

An enterprise-grade, privacy-first Android application developed in modern Kotlin and Jetpack Compose/Views. Built with strict adherence to modern Android architecture, lifecycle awareness, and hardware sensor efficiency.

---

## 🚀 Key Features

- **100% Air-Gapped**: Manifest strictly strips `android.permission.INTERNET` (zero network leaks).
- **Hardware-backed encryption**: AES-256-GCM with keys secured inside Android Keystore / StrongBox.
- **Biometric Authentication**: Unlocked via Fingerprint / Face BiometricPrompt CryptoObject.
- **Secure memory sanitization**: Plaintext keys wiped immediately from memory after use.
- Anti-tamper & anti-screenshot protections (`FLAG_SECURE`).

---

## 🛠️ Hardware & Sensor Architecture

* **Sensors & APIs:** `BiometricPrompt, AndroidKeyStore, Android Keystore StrongBox`
* **Declared Permissions:** `USE_BIOMETRIC, USE_FINGERPRINT (ZERO INTERNET PERMISSIONS)`
* **Target SDK:** Android 14 (API 34)
* **Minimum SDK:** Android 8.0 (API 26+)
* **Architecture:** MVVM / Unidirectional Data Flow (UDF) with Coroutines & StateFlow

---

## 🔒 Privacy & Permission Transparency

This application is engineered under the **Zero Unnecessary Permissions** doctrine:
* **No Hidden Trackers:** Contains zero advertising SDKs or third-party behavioral analytics.
* **No Accessibility Privilege Misuse:** All background operations utilize sanctioned Android APIs (`WorkManager`, `ForegroundService`, `JobScheduler`).
* **Air-Gapped Operation:** Runs locally on your device hardware without requiring external cloud servers.

---

## 📦 Building and Running

### Prerequisites
* Android Studio Iguana / Jellyfish / Ladybug or newer
* Android SDK 34
* JDK 17+

### Build APK via CLI
```bash
# Clone the repository
git clone https://github.com/naik2571/Zero-Cloud-Vault.git
cd Zero-Cloud-Vault

# Build Debug APK
./gradlew assembleDebug

# Install on connected device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
Licensed under the [Apache License 2.0](LICENSE).
