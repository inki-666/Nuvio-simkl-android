# Nuvio Simkl Android 1.1.0

Android companion for the Nuvio Simkl project. It replaces the manual Termux process with an Android foreground service and provides an explicit Nuvio -> Discord Rich Presence event bridge.

## What is implemented

- Persistent Android configuration
- Foreground local HTTP service
- Boot receiver that restores the configured service after reboot
- Nuvio presence events: Home, catalogue-focused, detail, playback, clear
- Stable catalogue identity via `catalogId` + `catalogName`
- Large artwork and Nuvio avatar support
- Episode/progress formatting
- Duplicate event suppression
- Presence state inspection endpoint
- Discord Social SDK native integration seam

## Important architecture point

The companion does **not** inspect screenshots, accessibility nodes, or visible rows. Multiple catalogue rows can be visible at once on a phone. The Nuvio app must explicitly report which catalogue is being interacted with/focused. The integration contract and reporter are in `NUVIO_INTEGRATION/`.

Current Nuvio Mobile is a Kotlin Multiplatform/Compose Multiplatform app. Its public repository documents `composeApp` as the shared app code and `androidApp` as the Android entry point. The exact hook must be applied to the Nuvio revision being built; this project deliberately does not invent a file/line that was not verified.

## Discord

Discord's official documentation currently states that Rich Presence via RPC is supported on Android from Social SDK 1.10+, without authentication, using a running signed-in Discord Android client. The official SDK binary must be obtained from Discord and linked locally. See `DISCORD_SDK.md`.

## Metadata

This Android companion does **not** return fake metadata from `/meta`. It returns 501 until the verified Simkl/provider resolver is ported or connected. The existing Node project remains the reference implementation for metadata resolution.

Simkl's current developer documentation explicitly says to use the original TMDB/TVDB APIs when metadata APIs are needed, while Simkl is used for identity/tracking workflows. The architecture therefore keeps Simkl as the identity authority and TMDB/TVDB as presentation providers.

## Build

Open the project in Android Studio with the Android SDK installed and run `app > build > assembleDebug`.

The environment used to prepare this bundle did not contain Android SDK/Gradle, so an APK build is **not claimed as verified** here.

## Install

After a successful Android Studio build, install `app/build/outputs/apk/debug/app-debug.apk` on the phone.

1. Open Nuvio Simkl.
2. Configure the service and Discord Application ID.
3. Enable Discord Rich Presence if desired.
4. Tap Save & Start once.
5. The persistent notification confirms the foreground service is active.
6. On subsequent phone reboots, the receiver attempts to restore the configured service.

See `VERIFICATION.md` for exactly what was and was not verified.


## Phone-only APK build
This source includes `.github/workflows/build-apk.yml`. Upload the project to a GitHub repository, then open **Actions → Build Android APK → Run workflow**. The resulting debug APK is published as a workflow artifact.

Playback events accept `positionSeconds` and `durationSeconds`; the presence state displays `position / duration`, and the Discord bridge carries an end timestamp for a live elapsed/remaining display when the official Discord SDK is linked.
