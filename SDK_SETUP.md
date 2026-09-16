# SDK setup note

The official Discord Social SDK is intentionally not redistributed.

This project has an isolated JNI boundary so the rest of the Android app can be developed independently. When you obtain the official Android SDK from the Discord Developer Portal, add the supplied AAR/native artifacts under `app/libs/` and update CMake to link the exact ABI libraries shipped with that SDK version.

Do not substitute a random binary from the internet.
