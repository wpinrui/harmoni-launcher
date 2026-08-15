# Hʌrmoni Launcher — Design Document

---

## Section 1 — Home Surface

**At rest**

Wallpaper, plus one composed block. No app icons. Remaining space is left free for wallpaper, Graffiti input, and ring taps.

**Clock block**

Single visual unit:

- Clock (large)
- Date, battery (small)
- Notification count badges: Telegram, Instagram, WhatsApp
- Music element
- YouTube link

Badges, the music element, and the YouTube link are tappable.

The music element always opens YouTube Music and is always present. Two states, each with its own icon:

- Playing — playing icon plus now-playing text
- Idle — idle icon, no text

**Bindings**

The badged apps and the YouTube target are named in source. This set is independent of the ring's eight.

**Open**

- Block position
- Typography
- Whether strokes crossing the block register

---

## Section 2 — Ring

**Invocation**

Tap on empty surface. Eight app icons appear in a ring centred on the touch point.

If the touch point is close enough to an edge that the full ring cannot be shown, the tap does not register.

**Selection**

Two-stage: tap to summon, tap to pick. Tapping the centre dismisses.

**Contents**

Eight apps, fixed in source. Same apps, same positions, every time.

**Icons**

Icon pack drawables where available, otherwise the app's own icon.

**Open**

- Edge margin
- Ring radius and icon size

---

## Section 3 — Contextual Ring

**Invocation**

Long-press on empty surface. Same ring geometry and edge rule as Section 2.

**Contents**

Eight contextually relevant apps. Rules defined in source, to be specified.

**Selection**

Tap to pick, centre to dismiss.

---

## Section 4 — Graffiti Search

**Input**

Custom Graffiti alphabet, to be specified. Strokes are drawn on the empty surface. A tap has no path and a letter does, so ring and search do not collide.

**Search view**

On the first stroke, a dark tint with blur fades in over the home surface. The search view sits on top and contains, top to bottom:

- The search string
- A 4×2 grid of app icons, horizontally paginated
- Space for Graffiti input

**Results**

Filtered from all installed apps. Same query produces the same layout every time. Tap to launch.

**Input area**

Strokes register only in the space below the grid.

**Backspace**

Horizontal swipe, right to left. Not started from the screen edge.

**Matching**

Case-insensitive subsequence match against app name and aliases — characters in order, not necessarily adjacent. Ranked by word-boundary hits, then consecutive runs, then name length. Edit distance as a fallback tier when subsequence returns no results.

Aliases defined in source, to be specified.

---

## Section 5 — Launcher App Screen

Shown when the launcher is opened as an app, rather than via the home gesture. Read-only.

**Contents**

- Ring bindings — the eight positions and what is on each
- Graffiti alphabet — stroke chart
- Contextual rules, as currently defined
- Permission health — whether notification listener and media session access are live
- Build info — version, build date, commit
- Diagnostics
- Full app list, with the names and aliases that resolve to each

**Diagnostics**

Logged from input events and the user's next action:

- Strokes followed by backspace, grouped by letter
- Ring summons dismissed without a pick
- Taps rejected for being too near an edge
