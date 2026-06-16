# PIS Android TV URL Player — Phase 1

Kiosk-mode Android TV app that boots into fullscreen, fetches a video playlist from a
remote JSON URL, and loops it continuously. Caches played video to disk for offline
playback, and self-heals on crash.

This is **Phase 1** (Core Behavior + Playback) of the PRD. Backend management, MQTT
remote commands, device registration/heartbeat, subtitles, and images are **not** in
this build — see the PRD for the full roadmap.

## Features in this build
- **Auto-launch on boot** — `BootReceiver` starts the player on `BOOT_COMPLETED`.
- **Fullscreen kiosk** — single immersive activity, no controls, screen kept on.
- **Crash recovery** — uncaught exceptions reschedule a relaunch via `AlarmManager`;
  ExoPlayer errors skip to the next item instead of crashing.
- **Remote playlist** — playlist JSON fetched from a configurable HTTP(S) URL.
- **Caching + offline** — video served through a 512 MB `SimpleCache`; on network
  failure the last cached playlist JSON is replayed.
- **Video + HLS** — progressive MP4 and `.m3u8` streams via Media3/ExoPlayer.

## Configure the playlist URL
Default URL is a `BuildConfig` field in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "DEFAULT_PLAYLIST_URL", "\"https://your.host/playlist.json\"")
```

It can be overridden at runtime without a rebuild by writing to `SharedPreferences`
(`tvplayer_config` / `playlist_url`) — see `data/RemoteConfig.kt`. The expected JSON
shape is in `sample-playlist.json`.

## Build & install
```powershell
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.pis.tvplayer/.ui.PlayerActivity
```

> The Gradle wrapper jar is not committed. Generate it once with a local Gradle:
> `gradle wrapper --gradle-version 8.9`, or open the project in Android Studio which
> provisions it automatically.

## True kiosk (boot straight into the player, no exit)
The activity already declares the `HOME` + `LEANBACK_LAUNCHER` intent filters. To make
the device boot into it as the launcher:

```powershell
# Optional: set as default HOME on a provisioned/kiosk device
adb shell cmd package set-home-activity com.pis.tvplayer/.ui.PlayerActivity
```

On managed/enterprise deployments, pair this with Lock Task mode (COSU/Device Owner) for
a fully locked kiosk. That provisioning is out of scope for Phase 1.

## Verify
- Launch → fullscreen playback, auto-advance, infinite loop.
- `adb shell am force-stop com.pis.tvplayer` → app relaunches (crash/restart recovery).
- `adb reboot` → app comes up on boot.
- Disable network after one play-through → cached video still plays.
