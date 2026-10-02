# Game spec

Every game in `games/<slug>/index.html` follows the same rules. `games/shape-sorting/index.html`
is the reference implementation: copy its skeleton and replace only the game-specific parts.

## Audience

Children aged 2 to 6, homeschooling, played on an Android tablet/phone inside the WebView of the
Android app in `android-app/` (also opened straight in desktop browsers for testing).

## Technical rules

- One self-contained `index.html`. CSS in `<style>`, JS in `<script>`. No external files,
  libraries, fonts, images or audio files. No frameworks or game engines.
- Vanilla HTML5 `<canvas>` + `requestAnimationFrame` main loop.
- Procedural graphics only (Canvas 2D paths, or inline SVG). Characters, animals, objects are all drawn in code.
- Canvas fills the window and re-lays out on `resize` / `orientationchange`; must work in portrait and
  landscape, phone and tablet. Use `html, body { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; }`.
- Handle both mouse (`mousedown/mousemove/mouseup`) and touch (`touchstart/touchmove/touchend/touchcancel`,
  `{ passive: false }` + `e.preventDefault()`), one finger at a time. DPR-aware canvas (cap DPR at 2.5).
- Wrap all game code in an IIFE with `'use strict'`. A `CONFIG` object at the top holds the tunables.
- `console.log('[<Tag>]', ...)` on key state changes: game loaded, round started (with level), item
  placed / answer accepted, item rejected, level completed, difficulty change, session finished, progress saved.
- Read-only debug hook on `window.<GameName>` exposing `progress()` and `state()` (copy pattern from the reference).
  `state()` should expose enough (phase, round, level, and the positions/ids of tappable or draggable things
  plus which one is correct) that an automated test could play the game.

## Pedagogy and UI

- **Coherence:** only the elements needed for the current task. No score counter, no menus, no decorative
  animation, no background music unless it *is* the task.
- **No text instructions on screen.** Kids can't read. Guide with a gentle pulse on what to do next when
  the child is idle (~1.5 s). Letters/numbers/words that *are* the learning content are fine.
- **Modality:** short spoken prompts via the Web Speech API (`say()` from the reference). Speech is often
  missing in Android WebView, so every game must still be playable with no speech: the visuals alone must
  make the task clear, and Web Audio tones carry the feedback. Do not show text that duplicates what is spoken.
- **Temporal contiguity:** the visual feedback and its sound fire on the same frame.
- **Control of error (Montessori):** a wrong answer is physically rejected (dragged item glides back home;
  a wrong tap gives a soft wobble/shake of that item and nothing else). Never a buzzer, red X or "wrong" text.
- **Forgiving hitboxes:** drop zones and tap targets at least 30% larger than the visible object
  (the reference uses far more). Tap targets at least ~56 CSS px.
- **Growth mindset:** praise effort and process ("You kept trying, and you did it!"), never "you're smart".
- **Scaffolding / DDA:** a `TIERS` ladder of 3–4 difficulty steps. Quick and clean round → one step up; a real
  struggle → one step down. After 2 slips on the same item, give a visual hint (glow the right target).
  In the first round, if the child hasn't found the mechanic after ~5 s, show the hint.
- Calm palette: warm off-white background `#F6F1E7`, ink `#4A4036`, soft pastels (reuse `COLORS` from the
  reference where it fits). Gentle motion; respect `prefers-reduced-motion`.

## Ethics

- No timers or countdowns the child can see or feel, no urgency, no lives/health bars.
- No random/variable rewards: the same success always gets the same celebration.
- Progress only through the child's deliberate actions; nothing auto-advances except the brief
  celebration after a completed round, which leads back to the tap-the-star screen.
- **Natural stopping point:** exactly 5 rounds per session. Between rounds, the big pulsing star screen with
  5 progress stars (copy from the reference). After round 5, the static end screen: smiling star, grass,
  "Great Job!" / "Time to play outside!". No "Play again" / "Next level" button. Stop the rAF loop.
- **Home button (transparent wayfinding):** every screen shows the same small home button in the top-right
  corner (snippet below). Keep game targets clear of it.

## Local progress (parents)

- `localStorage` key `<slug>.v1`, versioned JSON, wrapped in try/catch so the game works when storage is
  blocked. Store games completed, rounds completed, per-session rounds with level / attempts / slips / seconds,
  last played, keep the last 30 sessions.
- Hidden parent view: press and hold the top-left corner for 3 s (copy the `#parent` overlay from the
  reference) showing totals and a recent-sessions table relevant to that game.

## Home button snippet (identical in every game)

Place right after the `<canvas>`:

```html
<a id="home" href="../../index.html" aria-label="Home">
  <svg viewBox="0 0 24 24" width="26" height="26" aria-hidden="true"><path d="M4 11.5 12 5l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5h-5v5H5a1 1 0 0 1-1-1z" fill="none" stroke="#4A4036" stroke-width="2" stroke-linejoin="round"/></svg>
</a>
```

CSS:

```css
#home {
  position: fixed; top: max(10px, env(safe-area-inset-top)); right: max(10px, env(safe-area-inset-right));
  width: 52px; height: 52px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: rgba(255, 253, 248, 0.85); box-shadow: 0 2px 8px rgba(74, 64, 54, 0.15);
  -webkit-tap-highlight-color: transparent; z-index: 5;
}
#home:active { transform: scale(0.94); }
```

In layout code, reserve the top-right corner (about 72×72 px) so nothing interactive sits under the button.
