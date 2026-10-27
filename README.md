# Riftbound Counter

An Android score counter for Riftbound games. Track one or two players, manage a game timer, and customize player colors.

## Features

- One-player and two-player modes, with the second player's panel flipped for face-to-face play.
- Scores from 0 to 8; reaching 8 points triggers a victory animation.
- Configurable timer with start, pause, reset, and overtime display.
- Custom colors for each player, persisted between app launches.
- User interface built with Jetpack Compose and Material 3.

## Requirements

- Android Studio and JDK 17.
- Android SDK 37 to build; the app supports Android 7.0 (API 24) and later.

## Build and test

From the project root on Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

The debug APK is generated in `app/build/outputs/apk/debug/`.

## Build a signed release

Release signing is optional and configured locally through `local.properties`, which should not be shared or committed. Add the following properties with the paths and secrets for your keystore:

```properties
RELEASE_STORE_FILE=path/to/keystore.jks
RELEASE_STORE_PASSWORD=keystore-password
RELEASE_KEY_ALIAS=key-alias
RELEASE_KEY_PASSWORD=key-password
```

Then run:

```powershell
.\gradlew.bat assembleRelease
```

If no keystore is configured, Gradle generates an unsigned release (`RiftboundCounter-release-unsigned.apk`). With a keystore, the output is `app/build/outputs/apk/release/RiftboundCounter-release.apk`.

## Deploy to a phone

Connect a phone with USB debugging enabled, then run the script to build the release and install it with `adb`:

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy.ps1
```

If installation fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (for example, because a version with a different signature is already installed), add `-Reinstall` to uninstall the app before installing it. This will erase its data, including saved colors.

The script requires a signed release (see above) and uses `adb` from the Android SDK (`%LOCALAPPDATA%\Android\Sdk\platform-tools`) or, if unavailable, from `PATH`.

## Code organization

- `game/GameState.kt`: immutable game state, score rules, and timer formatting.
- `ui/CounterViewModel.kt`: UI actions, observable state, and color persistence.
- `ui/CounterScreen.kt`: main screen and connection to the ViewModel.
- `ui/Components.kt`: Compose timer, player panels, settings, and animations.
- `ui/Dice3D.kt`: 3D twenty-sided die rendering and interaction.
- `MainActivity.kt`: Android entry point.

## License

See [LICENSE](LICENSE).
