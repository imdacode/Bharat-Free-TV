# Bharat Free TV

Bharat Free TV is a DTH-style IPTV player for Android phones, tablets, and TV
boxes. It loads a user-supplied M3U, M3U8, or JSON channel guide and plays
streams with AndroidX Media3 ExoPlayer.

## Features

- Dedicated 2.5-second branded splash activity
- Full-window Media3 player using fill resize mode
- Fixed-width semi-transparent DTH guide over the playing video
- Four-second channel information banner with logo, category, and quality
- Automatic News, Movies, Entertainment, Sports, Music, and Regional categories
- Search, channel numbers, quality labels, remote logos, and persistent favorites
- Remote playlist caching with an offline fallback
- Touch, keyboard, and Android TV D-pad navigation

## Build

The project requires JDK 17 and the Android SDK (API 35). The Gradle wrapper
downloads the repository's pinned and checksum-verified Gradle version.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

The installable debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`. The optimized release APK is
written under `app/build/outputs/apk/release/` and must be signed with the
publisher's production keystore before distribution.

## Configure a playlist

Open **Settings** in the channel guide and paste a direct HTTP(S) playlist URL.
No demonstration channels are hardcoded or bundled. Extended M3U entries can
include channel numbers, logos, categories, and quality:

```m3u
#EXTINF:-1 tvg-chno="101" tvg-logo="https://example.com/logo.png" group-title="News" quality="HD",Example News
https://example.com/live/playlist.m3u8
```

JSON playlists can be an array or a `{ "channels": [...] }` object. Supported
field aliases include `name`/`title`, `url`/`streamUrl`/`stream_url`,
`group`/`category`, `logo`/`logoUrl`, `number`/`channelNumber`, and `quality`.

The root [`playlist.m3u`](playlist.m3u) is retained as an intentionally empty
offline fallback and copied into the app during each build.

## TV remote controls

- **OK / Center / Menu:** open the channel guide
- **Channel Up/Down or D-pad Up/Down:** change channels while the guide is closed
- **Back:** close the guide, then exit the app
