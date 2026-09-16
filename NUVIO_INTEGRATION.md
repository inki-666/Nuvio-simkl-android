# Nuvio integration

The Android app can host the local bridge, but it cannot know what the Nuvio UI is displaying unless Nuvio sends events.

Add a tiny client in Nuvio:

```text
POST/GET http://127.0.0.1:7000/presence
```

Recommended events:

HOME
- details = "Browsing Nuvio"
- state = username

CATALOG
- details = "Browsing <catalogue name>"
- state = username
- large = currently focused item's artwork

DETAIL
- details = title
- state = "Movie" / "Series" / "Anime"
- large = TMDB/TVDB artwork
- small = Nuvio avatar

PLAYBACK
- details = title
- state = "S02E07 • 42%"
- large = title artwork
- small = Nuvio avatar

When leaving Nuvio:
- GET `/presence/clear`

The app intentionally does not attempt to scrape or guess Nuvio's screen state.
