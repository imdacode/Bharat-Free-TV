# Bharat Free TV

Bharat Free TV is a small Android app for browsing and playing the free HLS
streams listed in [`playlist.m3u`](playlist.m3u). It supports phones, tablets,
and Android TV remotes.

## Features

- Parses the bundled extended M3U playlist at build time
- Plays HLS streams with AndroidX Media3
- Keyboard, touch, and D-pad friendly channel list
- Restores the selected channel and playback position after backgrounding
- Shows useful loading and playback error states

## Build

The project requires JDK 17 and the Android SDK (API 35). The Gradle wrapper
downloads the repository's pinned Gradle version automatically.

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`.

## Add or update channels

Edit the root `playlist.m3u` file. Each channel needs an `#EXTINF` line followed
by its stream URL:

```m3u
#EXTINF:-1 group-title="News",Example News
https://example.com/live/playlist.m3u8
```

Gradle copies this file into the app during every build, so there is no second
playlist copy to keep in sync.
