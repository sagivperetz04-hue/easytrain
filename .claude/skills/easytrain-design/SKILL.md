---
name: easytrain-design
description: Visual and interaction design guidance for EasyTrain screens — the Material 3 token system (palette, type, spacing), the coach vs trainee visual language, gym-floor usability rules for the workout logger, and the signature components (SetRow, RestTimer, AdherenceRing, TraineeCard). Use before building or restyling any screen, and when adding a component to core/designsystem.
---

# EasyTrain design

Approach this as the design lead for a small, opinionated fitness product. The app is used in two
very different contexts: a coach on a couch reviewing ten people, and a trainee between sets with
sweaty hands, a racked bar, and one signal bar. Design for the second one first — it is the
moment that decides whether the app gets used.

## Design brief (fixed — don't re-decide per screen)

**Subject**: strength training between two people who trust each other. The vocabulary is the
gym's: plates, racks, chalk, logbooks, the pen tucked in a notebook. Not "wellness", not neon
gamer HUDs, not a corporate dashboard.

**Palette** (defined once in `core/designsystem/Tokens.kt`; every screen uses theme roles, never these hex values):

| Token | Light | Dark | Role |
|---|---|---|---|
| `primary` | `#1F3A5F` (deep steel blue) | `#9BB8E0` | app identity, primary buttons, active nav |
| `secondary` | `#C8552D` (chalk-orange) | `#F2A07B` | *action on the floor*: complete-set check, rest timer, PR highlights |
| `tertiary` | `#3E7D5A` (plate green) | `#8FCFA9` | achieved targets, adherence "done" segments |
| `surface` | `#F7F5F2` (paper) | `#121417` | backgrounds; warm off-white, never pure white |
| `surfaceContainer` | `#ECE8E2` | `#1C1F24` | cards, set rows |
| `error` | Material default | | |

Dynamic color is **off**: a coach's roster should look the same on every phone. Both themes are
mandatory; test every screen in dark mode — gyms are often dim and trainees prefer it.

**Coach vs trainee**: same palette, different emphasis. Coach screens are `primary`-led,
information-dense, list-first. Trainee screens are `secondary`-led, one thing at a time, large
targets. The role never changes the theme; it changes which color carries the action.

**Type**: Material 3 type scale with two faces — a compact, slightly condensed grotesk for display
and numerals (`displayMedium` for the current weight/reps in the logger, tabular figures on), the
system default (`Roboto`) for body. Numbers are the hero of this app: they get the biggest type,
tabular alignment, and enough weight to read from an arm's length away.

**Spacing**: 4-dp grid; `Spacing.xs=4, sm=8, md=16, lg=24, xl=32`. Touch targets ≥ 48 dp; on the
logger ≥ 56 dp. Corner radius `12dp` cards, `999dp` chips; no glassmorphism, no gradients.

**Signature**: the **set row**. `prev · target · actual` on one line, the check on the right in
`secondary`; completing a set fills the row's background with a 10% `secondary` tint and the
numbers go bold. A whole workout reads as a logbook page filling up. Spend the design budget here.

## Gym-floor rules (trainee screens)

- One-handed, thumb-zone: primary action in the bottom third; never require the top-left.
- Numeric input: `KeyboardType.Decimal` for weight, `Number` for reps; `±2.5 kg` / `±1 rep`
  steppers of ≥ 48 dp beside the field; the field pre-fills from the previous set, then the previous
  workout, then the target.
- Zero spinners for local actions. Sync state is a small dot in the top bar, never a blocking dialog.
- Rest timer: full-width bar under the current exercise, big countdown, "+30 s" and "skip";
  keeps counting while backgrounded and posts a notification.
- Dark mode contrast ≥ 4.5:1 for all numerals. Test at 200% font scale — the logger must still fit
  three set rows on screen.
- Motion: only the set-complete fill (150 ms) and the timer. Respect the system reduce-motion setting.

## Coach screens

- `TraineeCard`: avatar, name, adherence ring (done/planned this week), last workout relative
  time, next session, unread badge. Scannable in a list of 30 — no more than two lines of text.
- `TraineeDetail` tabs are `PrimaryTabRow`, content is list-first; editing opens a full screen,
  not an inline form.
- `ProgramEditor`: days as a vertical list, exercises as reorderable rows with a drag handle on the
  left and targets summarized (`4 × 8–12 · 80 kg · 90 s`) on the right; tapping opens the target sheet.
- Empty states are instructions with one button ("Invite your first trainee"), never illustrations of sad clouds.

## Process for a new screen

1. State the screen's single job in one sentence and which role uses it in which context.
2. Sketch an ASCII wireframe in the plan; name the primary action and where it sits.
3. List which `core/designsystem` components it uses; add a component only if two screens need it.
4. Build; screenshot in light + dark + large font (Robolectric screenshot or emulator); remove one thing.

## Copy

Sentence case, plain verbs, the gym's words: "Log set", "Finish workout", "Assign plan",
"Schedule session". Buttons say what happens ("Save plan" → "Plan saved"). Errors say what to do
("You're offline — this will send when you reconnect"). No exclamation marks, no motivational filler.
