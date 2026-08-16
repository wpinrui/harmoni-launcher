# Hʌrmoni Launcher

A gesture-first Android launcher. No app drawer and no icon grid: a wallpaper, one clock block,
and everything else reached by a gesture on the empty space around it.

Apps live under your finger rather than in a list. Tap for the eight you always use, hold for the
eight you probably want right now, write a letter for anything else.

## Gestures

On the empty surface:

| Gesture | What it does |
| --- | --- |
| Tap | A ring of eight fixed apps, centred on your finger |
| Long press | A ring of eight apps scored for the moment |
| Double tap | Every app, less hidden ones and the ring's eight |
| Write a letter, lower half | Search, filtered by what you wrote |
| Swipe down, upper half | Notification shade |
| Swipe up | A bound app shortcut |
| Two finger swipe up | A second bound app shortcut |

Both rings pick on the next tap and dismiss on the centre or anywhere outside. Touch near an edge
and the ring centres itself at the closest point where all eight still fit, rather than refusing
the tap.

The ring's eight are left out of the app views by default, since they are already one tap away; a
toggle on the launcher app screen puts them back.

In the search view, further letters are drawn in the space below the grid, a right to left swipe
erases one, tapping an icon launches it and holding one opens its page in Settings. Tapping away
from the grid closes the view, as does coming home. The writing area is the exception: a tap
there does nothing, so a letter that came out too small cannot dismiss everything.

On the clock block, the time opens the clock, the date opens the calendar at today, the battery
opens battery settings, and the badges, music element and YouTube link open their apps.

## The launcher app screen

Opening Harmoni from the app list gives a screen that reports what the launcher is doing and lets
you change the parts that are yours:

- **Ring bindings.** Any of the eight positions can be rebound to another app, or reset to the one
  in source. A toggle here decides whether the ring's apps also appear in search.
- **Gesture shortcuts.** Either swipe up can be bound to an app shortcut, the kind other launchers
  show on a long press of an icon.
- **Hidden apps.** Checked means hidden. A hidden app never appears anywhere Harmoni offers an app,
  and a ring position holding one is left blank rather than closing the gap.
- **Graffiti alphabet.** The stroke chart, and a way to redraw any letter.
- **Permission health.** What is live now, each row tapping through to the Settings page behind it.
- **Contextual rules and build info.** Read-only. Diagnostics too, apart from resetting them.

Settings save on the tap and take effect the next time you come home, which is when Harmoni
restarts to pick them up.

## Permissions

None are required to launch apps. Each one buys one feature, and refusing it costs that feature
and nothing else.

| Permission | Granted where | Buys |
| --- | --- | --- |
| Notification listener | Settings, Notifications, Device and app notifications | Badge counts, and now-playing text once the service binds |
| Usage access | Settings, Apps, Special app access | Launch history for the contextual ring |
| Motion | Prompted on first launch | The transit rules |
| All files access | Settings, Apps, Special app access | Reading the wallpaper, so the search view can blur it |
| Accessibility | Settings, Accessibility | Swipe down for the notification shade |

The accessibility service takes no events and does not ask for window content, so it cannot read
what is on screen. Its entire capability is the one global action that opens the shade, which is
the only supported way an app can do that.

## Graffiti alphabet

The letters are not printed letters. Each is a single stroke with no pen lift, captured by hand and
shipped in `app/src/main/assets/graffiti.json`. Matching is nearest template rather than an average
of the samples, so a sloppy sample is one that never wins and a letter written two ways keeps both
forms.

To draw your own, open Harmoni from the app list and tap **Redraw the alphabet**. It steps through
the alphabet five draws per letter and writes to the app's external files directory. Any letter
recaptured there replaces the bundled samples for that letter alone, so one shape can be fixed
without redrawing the rest. To make a capture the shipped default:

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

## Checks

```
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

## Layout

| Path | What is in it |
| --- | --- |
| `app/src/main/java/com/wpinrui/harmoni/home/` | The home surface, gesture classifier, clock block and rings |
| `app/src/main/java/com/wpinrui/harmoni/search/` | The search view, matcher and wallpaper blur |
| `app/src/main/java/com/wpinrui/harmoni/graffiti/` | The alphabet, its recogniser and the capture tool |
| `app/src/main/java/com/wpinrui/harmoni/context/` | The contextual ring's rules and the signals feeding them |
| `app/src/main/java/com/wpinrui/harmoni/app/` | The launcher app screen and its pickers |
| `app/src/main/java/com/wpinrui/harmoni/apps/` | The installed-app index, icons and the hidden list |
| `app/src/main/java/com/wpinrui/harmoni/shortcuts/` | App shortcut querying and gesture bindings |
| `app/src/main/java/com/wpinrui/harmoni/system/` | Notifications, media sessions, clock and battery, accessibility, the shortcut buzz |
| `app/src/main/java/com/wpinrui/harmoni/settings/` | The one shared-preferences accessor every store uses |
| `app/src/main/java/com/wpinrui/harmoni/diagnostics/` | Diagnostic counters |
| `app/src/main/java/com/wpinrui/harmoni/ui/theme/` | Colours, type and the shared no-ripple modifier |

[GDD.md](GDD.md) describes the behaviour. The launcher is the source of truth and the document
follows it. Work is tracked in [issues](https://github.com/wpinrui/harmoni-launcher/issues).

## Credits

Badge icons from [Flaticon](https://www.flaticon.com), used under the Flaticon free licence:

- [Telegram icons](https://www.flaticon.com/free-icons/telegram) created by Magnific
- [WhatsApp icons](https://www.flaticon.com/free-icons/whatsapp) created by Fathema Khanom
- [Instagram icons](https://www.flaticon.com/free-icons/instagram) created by Grow studio

Type is [Karla](https://github.com/googlefonts/karla) by the Karla Project Authors and
[Inter](https://github.com/rsms/inter) by the Inter Project Authors, both under the SIL Open Font
License. Full texts in [licenses/](licenses/).

Stroke matching follows the $1 unistroke recogniser, Wobbrock, Wilson and Li, UIST 2007.

These credits also appear on the launcher app screen, which is where they have to be to satisfy the
licences of the assets shipped in the APK. Keep the two in step.
