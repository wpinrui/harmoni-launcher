# Hʌrmoni Launcher, Design Document

This document described the launcher before it was built. The launcher is now the source of truth,
and this has been corrected to match it. Where something is still undecided it says so.

---

## Section 1 — Home Surface

**At rest**

Wallpaper, plus one composed block. No app icons. Remaining space is left free for wallpaper,
Graffiti input, and ring taps.

**Clock block**

Single visual unit, top left:

- Clock (large)
- Date, battery (small)
- Notification count badges: Telegram, Instagram, WhatsApp
- Music element
- YouTube link

Everything in the block is tappable, and each element opens what it is about:

- Clock, the clock app, by `ACTION_SHOW_ALARMS` rather than by package
- Date, the calendar at today, addressed through `CalendarContract` rather than by package
- Battery and the percentage, battery settings
- Badges, their app
- Music element, YouTube Music
- YouTube link, YouTube

The music element always opens YouTube Music and is always present. Two states, each with its own
icon:

- Playing, playing icon plus now-playing text
- Idle, idle icon, no text

The block is drawn twice, once as a black silhouette behind the real pass, which is what makes
small text legible over an arbitrary wallpaper. The shadow pass takes no touches. The clock's
share of the silhouette is dialled back, since at its size the same shadow reads as a smear.

**Bindings**

The badged apps and the YouTube target are named in source. This set is independent of the ring's
eight.

**Strokes crossing the block**

Settled: they do not register. The block's elements consume the press, and Graffiti is confined to
the lower half of the surface in any case.

---

## Section 2 — Ring

**Invocation**

Tap on empty surface. Eight app icons appear in a ring centred on the touch point, with a light
haptic.

If the touch point is close enough to an edge that the full ring cannot be shown, the centre moves
to the nearest point where it fits. Only the axis short of room moves, so a touch near the left
edge slides right and not down. The tap is never refused.

**Selection**

Two-stage: tap to summon, tap to pick. Tapping the centre dismisses, and so does tapping anywhere
outside an icon.

The ring is inert for the length of the double-tap window, so the second half of a double tap is
not taken by an icon travelling under the finger.

**Contents**

Eight positions, clockwise from the top, each holding one app. The defaults are named in source;
any position can be rebound from the launcher app screen, and reset back to the default. A
position may also hold a pinned web app opened in a named browser, with an icon from the repo.

A position whose app is hidden is left blank. The eight are positional, so closing the gap would
move every app after it and cost the muscle memory the ring is for.

**Icons**

`IconResolver` exists as the seam for icon packs. Nothing parses a pack yet, so every icon is the
app's own.

The mockups blur the wallpaper behind each icon. Compose cannot blur a backdrop without capturing
it first, so the icons are translucent white instead. Still open.

**Geometry**

Radius 104dp, icon 62dp. The edge margin is derived, not a separate figure: radius plus half an
icon plus 7dp, which is the distance at which the outermost icon would touch the screen edge.

---

## Section 3 — Contextual Ring

**Invocation**

Long-press on empty surface, at two thirds of the system's long-press timeout. The ring appears at
the moment the threshold passes rather than on release, with a heavier haptic than the tap ring's,
since the finger is still down and that is the only signal that waiting has paid off.

Same ring geometry and edge behaviour as Section 2.

**Contents**

Eight contextually relevant apps, scored at the moment of the press from launch history, motion,
notifications, USB state and the date. The rules are data in source and are listed in full on the
launcher app screen.

Excluded from scoring: anything already one gesture away on the fixed ring or the clock block, and
anything hidden.

**Selection**

Tap to pick, centre to dismiss.

---

## Section 4 — Graffiti Search

**Input**

A custom single-stroke alphabet, 26 letters, captured by hand rather than authored. Five samples
per letter, all kept, shipped at `app/src/main/assets/graffiti.json`. Any letter redrawn on the
device replaces the bundled samples for that letter alone.

Matching is the $1 unistroke recogniser (Wobbrock, Wilson and Li) with two departures: scaling is
uniform rather than to a square, since squaring up a straight vertical magnifies jitter into the
whole shape; and there is no rotation normalisation, since rotation invariance makes M match W and
N match Z. Nearest template wins, with a floor of 0.70.

Strokes register only in the lower half of the surface, the same place the search view puts its
input area. Only the stroke's first point is tested, so a letter that runs up out of the area is
still a letter. Nothing marks the boundary.

**Telling the gestures apart**

A tap has no path and a letter does, so the ring and search never compete. Beyond that, path shape
and starting point separate the rest:

- A straight run upward, at least twice as vertical as horizontal, is a shortcut gesture. Measured
  over the captured alphabet, every letter ending above where it started is an arch or a curve and
  scores 0.34 or below on straightness; the threshold is 0.85.
- A straight run downward is the notification shade, but only in the upper half. Shape alone cannot
  separate it from the letter `i`, a straight vertical at 0.99, so where it starts is what does.
- Two fingers doing anything other than an upward swipe registers as nothing. Nobody writes with
  two.

**Search view**

Opens on the first stroke, carrying that letter, or on a double tap with nothing typed at all,
which makes it the all apps view. A dark tint over a blurred copy of the wallpaper fades in over
half a second, along with everything on top of it.

The blur is drawn rather than asked for. The window manager can only blur what is behind a window,
and the launcher's own window is where the wallpaper is drawn, so the launcher blurs its own copy.
Reading the wallpaper is gated on all-files access; without it the view keeps its tint alone.

Top to bottom:

- The search string, with a caret
- The result count
- A 4x2 grid of app icons, horizontally paginated
- Space for Graffiti input

**Results**

Filtered from all installed apps. Same query produces the same layout every time. Tap to launch,
hold to open that app's page in Settings.

Hidden apps never appear. The fixed ring's eight are also excluded by default, since they are
already one tap away, with a toggle on the launcher app screen to put them back.

**Leaving**

Tapping anywhere that is not the grid, the query or the input area. Coming home. System back does
nothing, and neither does a tap in the input area, which would otherwise close the view out from
under a letter that came out too small.

**Backspace**

Horizontal swipe, right to left, in the input area. Not started from the screen edge, which is
automatic: an edge swipe is taken by the system and never arrives. Checked against the captured
alphabet, no letter trips the same test.

**Matching**

Case-insensitive subsequence match against app name and aliases, characters in order, not
necessarily adjacent. Ranked by word-boundary hits, then consecutive runs, then name length. Edit
distance as a fallback tier when subsequence returns no results.

Aliases are still to be specified. Nothing implements them, so an app resolves by its name alone.

---

## Section 5 — Gesture Bindings

Two gestures on the empty surface carry an app shortcut, the kind other launchers show on a long
press of an icon:

- Swipe up
- Two finger swipe up

Both are bound from the launcher app screen, from a flat list of every shortcut published on the
device. Reading that list requires Harmoni to be the active home app; the system refuses it to
anything else.

Swipe down in the upper half opens the notification shade. Android has no supported way for an app
to do this other than an accessibility service, so Harmoni ships one whose entire capability is
that single global action: it takes no events and cannot read screen content. That service is
used whenever it is bound, and the undocumented `StatusBarManager` route is the fallback, so the
gesture may work before the grant.

---

## Section 6 — Launcher App Screen

Shown when the launcher is opened as an app, rather than via the home gesture. The home activity
has no launcher entry, since reaching the home surface from a list of apps is a way of going where
you already are.

**Contents**

- Ring bindings, the eight positions and what is on each. Editable.
- Gesture shortcuts, what each swipe up runs. Editable.
- Hidden apps, opening its own page. Editable.
- Graffiti alphabet, a stroke chart drawn from the templates in use, and a way to redraw them
- Permission health
- Attributions
- Contextual rules, as written in source. Read-only.
- Diagnostics
- Build info, version, build date, commit

Ring bindings, the alphabet chart, permission health and attributions are open when the screen
appears. Gesture shortcuts, hidden apps, the contextual rules, diagnostics and build info start
collapsed.

**Hidden apps**

Checked means hidden. A hidden app never appears anywhere Harmoni offers an app: not in search,
not in the all apps view, not among the contextual ring's candidates, and not in the list a ring
position is bound from. It is still installed and still launchable by other means.

**Permission health**

Each row reports whether the thing is live now, not merely granted, and taps through to the
Settings page behind it. Re-read on every return to the screen, since these are changed elsewhere.

- Notification listener, for badge counts
- Media sessions, which need the listener actually bound and so lag the grant
- Usage access, for launch history
- Motion, for the transit rules
- All files access, for reading the wallpaper
- Notification shade, the accessibility service

**Settings**

Everything saves on the tap. Nothing propagates to a running home surface, so Harmoni restarts the
next time the home surface comes to the front, which is where a restart costs nothing.

**Diagnostics**

Counted rather than logged, and kept across restarts. All of them are the same shape of evidence:
an input the launcher accepted and the user then undid.

- Letters erased immediately after being drawn, grouped by letter, which is the shape to redraw
- Ring summons dismissed without a pick

Taps refused near an edge were counted until Section 2 stopped refusing them. That counter is
gone.
