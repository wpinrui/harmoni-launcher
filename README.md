# Harmoni Launcher

A gesture-first Android launcher. No app drawer and no icon grid: a wallpaper, one clock block,
and three gestures on the empty space around it.

- Tap summons a ring of eight fixed apps around your finger.
- Long press summons a ring of eight contextual apps.
- Drawing a letter opens Graffiti search over every installed app.

[GDD.md](GDD.md) is the design document and the source of truth for behaviour. Work is tracked in
[issues](https://github.com/wpinrui/harmoni-launcher/issues).

## Status

Early. The shell is in place: Harmoni can be set as the home app, draws an empty surface over the
wallpaper, and keeps a live index of installed apps. Nothing is drawn on that surface yet.

## Requirements

- A device running Android 15 (API 35) or newer
- JDK 17 or newer
- Android SDK platform 37

## Build

```
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

If Gradle cannot find your SDK, point `local.properties` at it. On Windows the drive colon has to
be escaped:

```
sdk.dir=C\:/Users/you/AppData/Local/Android/Sdk
```

## Install

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then choose Harmoni under Settings, Apps, Default apps, Home app.

## Checks

```
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```
