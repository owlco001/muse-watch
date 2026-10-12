# MuseWatch

Muse client for H19 4G Android smartwatch (GeckoView-based).

## Why GeckoView?

The watch's system WebView is Chrome 59 (too old for muse.ai).
Bundles Gecko 157 engine directly in the APK.

## Build

Requires Android SDK with `platforms;android-34`, Gradle 8.7, AGP 8.5.2.

GeckoView AAR is not in this repo (too large). Download from
https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/
and place in a local Maven repo, or add `maven { url "https://maven.mozilla.org/maven2/" }`.

See `app/build.gradle` for the exact version.

## Versions

- v2.4: Round screen UI (466x466), floating nav buttons
- v2.3: Watch UI (font boost, tap targets)
- v2.2: Performance (no animations, system fonts, splash)
- v2.1: Size optimized (94MB)
- v2.0: GeckoView rewrite
- v1.x: Legacy WebView shell (Chrome 59, login broken)
