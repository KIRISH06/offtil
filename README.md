# OfflinePay — Android starter project

Kotlin + Jetpack Compose Android app with QR scanning, UPI ID/mobile entry, local transaction history, first-run setup, and a SIM preference setting.

## Important payment safety note

This project is **not a production-ready bank/payment app**. A SIM preference is saved, but Android/operator support determines whether a specific SIM can be used for USSD. Recipient-name verification and bank transaction confirmation require an authorized UPI/bank integration. Never treat a locally generated reference ID as a bank transaction ID. Never enter a UPI PIN into an app-owned field; use only the official bank/UPI confirmation screen.

The Accessibility automation service has been removed from the app manifest so the app does not automatically operate or hide bank confirmation dialogs. The existing USSD implementation may need further changes before payment flows can be safely enabled. Google Sign-In and app-lock authentication still require implementation/configuration and are not represented as completed features.

## Open in Android Studio

1. Extract this ZIP.
2. Android Studio → **Open** → select the `OfflinePay` folder containing `settings.gradle.kts`.
3. Let Gradle sync. Install the Android SDK/JDK version requested by Android Studio if prompted.
4. Connect an Android phone with USB debugging enabled and authorized.
5. Select the phone and press **Run ▶**.

## Build an APK

Android Studio → **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**. If successful, the debug APK is usually at `app/build/outputs/apk/debug/app-debug.apk`.

The Gradle wrapper JAR is not included in this archive, so use Android Studio's Gradle integration or install/configure Gradle if the wrapper command does not run.
