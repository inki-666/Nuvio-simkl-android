# Nuvio WeTrakr — Android TV foundation

This patch removes the WeTrakr-specific configuration and branding and renames the Android package to `com.nuvio.wetrakr.android`. It prepares the existing companion for Android TV / Google TV while retaining the Nuvio local service and Discord Rich Presence.

## What this stage changes

- Replaces WeTrakr-specific names and references with WeTrakr naming.
- Removes the WeTrakr client-ID setting and preference model.
- Renames the Kotlin package, Gradle namespace, and Android application ID to `com.nuvio.wetrakr.android`. This is a new Android package identity, so Android will treat it as a separate app from an older installed build.
- Adds optional Leanback support and a Leanback launcher entry while keeping phone support.
- Uses larger, D-pad-focusable settings on Android TV / Google TV.
- Keeps the current local Nuvio service and Discord Rich Presence bridge.
- Updates the GitHub Actions build artifact to `nuvio-wetrakr-debug`.

## Intentionally not implemented yet

The WeTrakr API, authentication, syncing, and tracker-specific features are not implemented in this stage. The local `/meta/...` endpoint remains an explicit HTTP 501 placeholder until the final integration stage. TMDB/TVDB fields remain placeholders and do not currently enable metadata lookup.

No artificial free-request quota was found in the inspected local server or cloud relay code. Respect provider-side rate limits when the WeTrakr integration is implemented.

## Applying this patch from an Android phone

This ZIP includes a one-time rename script because the project package is spread across multiple Kotlin files.

1. Make a branch/backup before applying the rename.
2. Extract the ZIP into the repository root, preserving paths. The replacement files should land under `app/src/main/java/com/nuvio/wetrakr/android/`.
3. Run `python apply_wetrakr_rename.py` from the repository root using a terminal or Codespace. The script updates remaining source/docs, moves the rest of the Kotlin package, and deletes itself when done. Do not run it from a folder other than the repository root.
4. Review the diff, commit the changes, and open the GitHub Actions workflow.
5. Download the `nuvio-wetrakr-debug` artifact only after the workflow succeeds.

The GitHub Actions build has not yet been run for this patch; do not treat it as verified until the workflow succeeds.
