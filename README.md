# Harmoni Launcher

A gesture-first Android launcher. No app drawer and no icon grid: a wallpaper, one clock block,
and three gestures on the empty space around it.

- Tap summons a ring of eight fixed apps around your finger.
- Long press summons a ring of eight contextual apps.
- Drawing a letter opens Graffiti search over every installed app.

[GDD.md](GDD.md) is the design document and the source of truth for behaviour. Work is tracked in
[issues](https://github.com/wpinrui/harmoni-launcher/issues).

## Status

Early. Harmoni can be set as the home app and draws the clock block over the wallpaper: time,
date, battery, notification badges, the music element and the YouTube link. Tapping the empty
surface summons the ring of eight, which picks on the next tap and dismisses on the centre or
anywhere outside. Long press summons the contextual ring instead. A tap too near an edge flashes
and is refused. Double tap opens every installed app. Drawing a letter opens the same view
filtered by it, and further letters are drawn in the space below the grid, with a right to left
swipe to erase.

Badge counts and now-playing text need notification access, granted by hand under Settings,
Notifications, Device and app notifications.

## Graffiti alphabet

The letter shapes are not printed letters. Each one is a single stroke, captured by hand and
shipped in `app/src/main/assets/graffiti.json`, and matching is nearest-template rather than an
average of the samples.

To draw your own, open **Graffiti Setup** from the app list. It steps through the alphabet five
draws per letter and writes to the app's external files directory. Any letter recaptured there
replaces the bundled samples for that letter alone, so a single shape can be fixed without
redrawing the rest. To make a capture the shipped default:

```
adb pull /sdcard/Android/data/com.wpinrui.harmoni/files/graffiti.json app/src/main/assets/
```

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

## Credits

Badge icons from [Flaticon](https://www.flaticon.com), used under the Flaticon free licence:

- [Telegram icons](https://www.flaticon.com/free-icons/telegram) created by Magnific
- [WhatsApp icons](https://www.flaticon.com/free-icons/whatsapp) created by Fathema Khanom
- [Instagram icons](https://www.flaticon.com/free-icons/instagram) created by Grow studio

Type is [Karla](https://github.com/googlefonts/karla) by the Karla Project Authors, under the SIL
Open Font License. Full text in [licenses/Karla-OFL.txt](licenses/Karla-OFL.txt).

These credits also have to appear on the launcher app screen, tracked in
[#6](https://github.com/wpinrui/harmoni-launcher/issues/6).

## Checks

```
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```
