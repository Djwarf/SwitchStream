# SwitchStream

A modern Android client for [Jellyfin](https://jellyfin.org/) media servers, built with Kotlin and Jetpack Compose.

Browse, search, and stream your personal media library from your Jellyfin server — on your phone, tablet, or Android TV.

![SwitchStream Home Screen](screenshot.png)

## Features

- **Connect to your Jellyfin server** — enter your server URL and sign in
- **Browse libraries** — view your media collections with filtering and sorting
- **Search** — full-text search across all content
- **Stream video** — adaptive HLS and direct playback via ExoPlayer
- **Playback controls** — speed adjustment, audio/subtitle track selection, seek controls
- **Series support** — episode browsing with auto-play next episode
- **Favorites & History** — bookmark media and resume where you left off
- **Android TV** — optimized for Leanback with focus-based navigation
- **Privacy-first** — no analytics, no tracking, no data collection

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Jellyfin SDK** (1.6.3)
- **Media3 / ExoPlayer** (1.5.1) with HLS support
- **Coil** for image loading
- **DataStore** for encrypted local persistence
- **MVVM** architecture with Repository pattern

## Requirements

- Android 7.0+ (API 24)
- A Jellyfin media server

## Building

Clone the repo and open in Android Studio, or build from the command line:

```bash
./gradlew assembleDebug
```

For a signed release build, set the following environment variables:

```
SWITCHSTREAM_KEYSTORE_FILE
SWITCHSTREAM_KEYSTORE_PASSWORD
SWITCHSTREAM_KEY_ALIAS
SWITCHSTREAM_KEY_PASSWORD
```

Then run:

```bash
./gradlew assembleRelease
```

## Privacy

SwitchStream does not collect any personal data. All credentials and preferences are stored locally on your device. The app communicates only with the Jellyfin server you configure.

See the full [Privacy Policy](PRIVACY_POLICY.md).

## Contact

- **Developer**: Djwar Fettah
- **Organization**: SwitchSides
- **Email**: hello@switchsides.co.uk

## License

All rights reserved.
