# Discord Android Rich Presence

The Android Rich Presence part is wired as a native integration seam, but the official Discord Social SDK binaries are **not redistributed** in this repository.

Discord's current documentation says Android supports Rich Presence without authentication through RPC from Social SDK 1.10+, provided the Discord Android app is installed and signed in and your app has a valid Discord Application ID.

## 1. Create the Discord application

Create an application in the Discord Developer Portal and enable Discord Social SDK.

Official setup:
https://github.com/discord/discord-api-docs/blob/main/developers/discord-social-sdk/getting-started/partials/getting-started.mdx

## 2. Download the official Android SDK

From your Discord Developer Portal:

Discord Application → Discord Social SDK → Downloads → Android.

Do not use a random third-party binary.

## 3. Add the SDK to this project

Put the Android SDK package in:

`app/libs/`

Then connect its native library through `app/src/main/cpp/CMakeLists.txt` according to the exact SDK package version.

The JNI seam is:

`DiscordBridge.nativeSetPresence(...)`

It is deliberately kept isolated so SDK updates do not affect the rest of the application.

The native implementation should create a `discordpp::Client`, call:

- `SetApplicationId(applicationId)`
- construct `discordpp::Activity`
- `SetDetails(details)`
- `SetState(state)`
- `SetType(ActivityTypes::Playing)`
- `SetTimestamps(...)`
- `SetAssets(...)`
- `UpdateRichPresence(...)`

Discord documents this direct Android RPC flow in the official Rich Presence guide.

## 4. Desired Nuvio activity mapping

Home:
- details: `Browsing Nuvio`
- state: username/category
- large image: catalogue artwork
- small image: Nuvio profile avatar

Catalogue:
- details: `Browsing Popular Series`
- state: username

Detail:
- details: title
- state: `Series • 2026`

Playback:
- details: title
- state: `S02E07 • 42%`
- timestamps: playback start/end

The Android service exposes:

`GET /presence?details=...&state=...&large=...&small=...`

Nuvio itself should call this endpoint whenever its UI state changes. A remote Stremio addon cannot detect arbitrary scrolling on its own.
