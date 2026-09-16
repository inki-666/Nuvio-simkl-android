# Verification report — Nuvio Simkl Android 1.1.0

Date: 2026-09-16

## Verified from authoritative/current documentation

- Nuvio Mobile is currently a Kotlin Multiplatform + Compose Multiplatform app with shared code in `composeApp` and Android entry points in `androidApp`.
- Stremio catalogues have stable `id`, `type`, and `name` fields. The companion therefore uses an explicit catalogue ID/name event instead of guessing from visible rows.
- Discord documents Rich Presence over unauthenticated RPC on Android starting with Social SDK 1.10+.
- Android 14+ requires an explicit foreground-service type; this app uses `specialUse` with the required manifest permission and service-level use-case property.

## Source-level checks performed locally

- AndroidManifest.xml parses as XML.
- All Kotlin source files were checked for balanced braces/parentheses and expected package declarations.
- CMake source was checked for a complete JNI bridge and the project contains the native library target.
- Boot receiver, foreground service, HTTP endpoints, event normalization, duplicate suppression, and state serialization are present in source.
- The ZIP was regenerated from the exact source directory after these changes.

## What could NOT be honestly claimed as verified here

- A real Android APK build/install: this environment has no Android SDK/Gradle installation.
- Real Discord presence delivery: Discord's official Social SDK binary is not redistributable and is not present in this source bundle.
- Exact upstream Nuvio source insertion line: the public Nuvio repository was inspected at repository/documentation level, but the exact revision-specific Home composable was not locally checked out, so no fake patch is claimed.

## Runtime contract

POST `http://127.0.0.1:7000/presence` with JSON events:

- HOME
- CATALOG_FOCUSED
- DETAIL
- PLAYBACK
- CLEAR

GET `/presence/state` reports the last accepted event.
GET `/health` reports service and Discord configuration state.
GET `/presence/clear` clears the activity.

The service starts as a foreground service and is configured to restart after boot when the app has previously been configured.
