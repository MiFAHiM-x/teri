# FAHIM WEB INJECTOR

**Developer: FAHIM**

A premium dark-themed native Android application that embeds a full-featured WebView browser and allows the user to inject a configurable JavaScript bookmarklet into the currently loaded page.

## Features

- Loads the real website `https://aincradmods.com/getkey` (configurable)
- Full WebView support: JavaScript, DOM Storage, cookies, sessions, redirects, Cloudflare challenges, forms, etc.
- Editable JavaScript bookmarklet with permanent persistence (SharedPreferences)
- Default bookmarklet is pre-loaded and can be reset at any time
- Prominent **⚡ APPLY JAVASCRIPT** button that executes the saved script on the *current* page without reloading
- Browser controls: Back, Forward, Reload, Home
- Android system back button navigates WebView history first
- Loading progress indicator
- Network error handling with Retry
- Secure SSL error handling (user decision)
- Material Design 3 dark theme with cyan/green accents

## Project Structure

```
FahimWebInjector/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/fahim/webinjector/
│       │   ├── MainActivity.kt
│       │   └── ScriptManager.kt
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/ (colors, strings, themes)
│           ├── drawable/
│           ├── mipmap-anydpi-v26/
│           └── xml/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
```

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer (recommended)
- JDK 17
- Android SDK 34
- Min SDK 24

## How to Build

### Option 1 – Android Studio (recommended)

1. Open Android Studio.
2. **File → Open** and select the `FahimWebInjector` folder.
3. Wait for Gradle sync to finish.
4. Connect a device or start an emulator (API 24+).
5. Click **Run** (green play button) or press `Shift+F10`.

### Option 2 – Command line

```bash
cd FahimWebInjector

# Debug APK
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease
```

APKs will be located at:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

### Signing a release APK (optional)

```bash
# Generate a keystore (one-time)
keytool -genkey -v -keystore fahim-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias fahim

# Then configure signingConfigs in app/build.gradle.kts and run:
./gradlew assembleRelease
```

## Configuration

- **Home URL**: Change the constant `homeUrl` inside `MainActivity.kt`.
- **Default JavaScript**: Located in `ScriptManager.DEFAULT_SCRIPT`. Do not alter the base64-encoded URL unless you intentionally want a different remote script.

## Usage Flow

1. App launches and loads `https://aincradmods.com/getkey`.
2. User interacts with the website normally (Continue buttons, Cloudflare, redirects, etc.).
3. When ready, user presses **⚡ APPLY JAVASCRIPT**.
4. The currently saved script is injected into the active page via `WebView.evaluateJavascript()`.
5. Script can be edited, saved, or reset from the top panel at any time.

## Important Notes

- The application does **not** automatically bypass CAPTCHA, Cloudflare, or any anti-bot protection. Users must complete verifications manually.
- Cookies and local/session storage are preserved across navigations.
- The JavaScript is executed in the context of the currently loaded origin.
- On first launch the default bookmarklet is written to SharedPreferences and survives app restarts.

## License

Private project – FAHIM WEB INJECTOR  
Developer: FAHIM
