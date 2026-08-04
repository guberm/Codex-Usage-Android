# Codex Usage for Android

An unofficial Android companion for tracking OpenAI Codex usage limits.

## Features

- remaining percentage for the primary Codex usage limit;
- date and local time of the next reset;
- additional limits, such as GPT-5.3-Codex-Spark, when available for the account;
- Quick Settings tile with tap-to-refresh;
- compact 2×1 Home screen widget with light/dark appearance, remaining percentage, reset time, and tap-to-refresh;
- periodic background monitoring and a persistent usage notification that resume after a device restart;
- readable remaining percentage in the notification, Quick Settings tile, and Home screen widget;
- Android 16+ status-bar percentage chip after enabling promoted notifications in the app;
- automatic refresh when the app opens, the phone unlocks, or connectivity returns;
- notifications when the remaining limit changes;
- fixed 15-minute background checks and 1% change notifications;
- native light and dark themes that follow the device setting;
- secure ChatGPT device login without an API key or copied browser cookies.

Defaults: check every hour and notify on a `±1%` change.

## Sign-in and security

The app uses the Codex device-authorization flow and retrieves data from the
same ChatGPT endpoint used by Codex CLI:

`GET https://chatgpt.com/backend-api/wham/usage`

OAuth tokens are encrypted locally with a key stored in Android Keystore.
Application backup is disabled. Browser cookies and OpenAI API keys are not used.

This endpoint is part of an internal ChatGPT/Codex contract and may change.
This app is not an official OpenAI product.

## Build

Requirements: JDK 17+ and Android SDK 36.

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat test assembleDebug
```

Release signing configuration is read only from environment variables:

- `CODEX_USAGE_KEYSTORE_PATH`
- `CODEX_USAGE_KEYSTORE_PASSWORD`
- `CODEX_USAGE_KEY_ALIAS`
- `CODEX_USAGE_KEY_PASSWORD`

GitHub Actions uses the corresponding repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## Android limitations

Android may delay periodic background checks to save battery. On Android 16+, use
the `Enable % in status bar` button once to allow the readable percentage chip.
The device manufacturer may also force-stop the app or restrict network access.
The Quick Settings tile, Home screen widget refresh button, and the `Refresh now`
button always perform a manual check. Change notifications show the actual
difference between two successful checks.

## License

MIT
