# Nuvio Mobile integration contract

Research verified that current Nuvio Mobile is a Kotlin Multiplatform/Compose Multiplatform app, with shared UI under `composeApp` and Android-specific code under `androidApp`. The public repository is `NuvioMedia/NuvioMobile`.

Stremio itself models each catalogue with a stable `id`, `type`, and human-readable `name`; therefore the reporter must send `catalogId` + `catalogName`, not infer a catalogue from which rows happen to be visible.

## Required event points

1. **Home opened**
   - `event=HOME`
2. **Catalogue actively browsed/focused**
   - `event=CATALOG_FOCUSED`
   - send the actual catalogue `id` and `name`
   - optionally send the currently focused/selected item's poster as `artwork`
3. **Detail page opened**
   - `event=DETAIL`
   - send title, media type and artwork
4. **Playback started / episode changed**
   - `event=PLAYBACK`
   - send title, season, episode, progress and artwork
5. **Nuvio leaves the relevant screen / closes**
   - call `clear()`

## Important

Do NOT implement this using accessibility scraping, screenshot analysis, or "most visible row" heuristics. The Nuvio app already owns the catalogue/navigation state, so it should explicitly report it.

The companion's Android service has been implemented and tested at the HTTP-contract level for these events. The exact upstream source insertion point is intentionally not fabricated here because it depends on the Nuvio Mobile revision being integrated.
