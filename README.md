# Early Learning Games

20 calm, ad-free learning games for children aged 2 to 6, built for homeschooling.

**Play:** https://gokul11122.github.io/early-learning-games/

Each game is one self-contained `index.html` (HTML5 canvas and plain JavaScript, every graphic drawn in
code, every sound made with Web Audio). They run in any browser and drop straight into an Android
WebView.

## Games

| # | Game | Learning area (ELOF) | Folder |
|---|------|----------------------|--------|
| 1 | Shape Sorting Sensory Bins | Cognition: classification · fine motor | [`games/shape-sorting`](games/shape-sorting/index.html) |
| 2 | Number Hopscotch | Cognition: counting, numerals, simple sums | [`games/number-hopscotch`](games/number-hopscotch/index.html) |
| 3 | Alphabet Scavenger Hunt | Literacy: beginning sounds | [`games/alphabet-scavenger-hunt`](games/alphabet-scavenger-hunt/index.html) |
| 4 | Storytelling Dice | Literacy: oral language, narrative | [`games/storytelling-dice`](games/storytelling-dice/index.html) |
| 5 | Musical Color Freeze | Motor / Cognition: colors, listening, self-control | [`games/musical-color-freeze`](games/musical-color-freeze/index.html) |
| 6 | Animal Sound Match | Cognition: auditory association | [`games/animal-sound-match`](games/animal-sound-match/index.html) |
| 7 | Sorting and Counting Beads | Cognition: sorting, one-to-one counting | [`games/sorting-counting-beads`](games/sorting-counting-beads/index.html) |
| 8 | Memory Card Flip | Approaches to Learning: working memory | [`games/memory-card-flip`](games/memory-card-flip/index.html) |
| 9 | Pattern Sequencing | Cognition: patterns | [`games/pattern-sequencing`](games/pattern-sequencing/index.html) |
| 10 | Block Stacking | Cognition: size order, reasoning | [`games/block-stacking`](games/block-stacking/index.html) |
| 11 | Emotion Recognition | Social and Emotional | [`games/emotion-recognition`](games/emotion-recognition/index.html) |
| 12 | Phonics Sound Blending | Literacy: phonics, CVC words | [`games/phonics-sound-blending`](games/phonics-sound-blending/index.html) |
| 13 | Animal Habitats | Cognition: science, living things | [`games/animal-habitats`](games/animal-habitats/index.html) |
| 14 | Daily Routine Sequencing | Approaches to Learning: sequencing, life skills | [`games/daily-routine-sequencing`](games/daily-routine-sequencing/index.html) |
| 15 | Letter Tracing | Literacy · Perceptual-Motor: writing strokes | [`games/letter-tracing`](games/letter-tracing/index.html) |
| 16 | Sharing and Cooperating | Social and Emotional: fairness | [`games/sharing-cooperating`](games/sharing-cooperating/index.html) |
| 17 | Five Senses Sorting | Cognition: science, the senses | [`games/five-senses-sorting`](games/five-senses-sorting/index.html) |
| 18 | Coloring and Creative Arts | Perceptual-Motor: fine motor, colors | [`games/coloring-creative-arts`](games/coloring-creative-arts/index.html) |
| 19 | Simple Measurements | Cognition: longer, taller, heavier | [`games/simple-measurements`](games/simple-measurements/index.html) |
| 20 | Music and Rhythm Maker | Creative: rhythm, pattern memory | [`games/music-rhythm-maker`](games/music-rhythm-maker/index.html) |

## How every game behaves

The full rules are in [SPEC.md](SPEC.md). In short:

- **Five rounds, then stop.** Between rounds a big star shows five progress stars filling up. After round
  five a static "Great Job! Time to play outside!" screen appears, with no "play again" button.
- **No timers, scores, ads or random rewards.** The child works at their own pace, and the same success
  always gets the same celebration.
- **Mistakes correct themselves.** A wrong piece glides back home or a wrong choice wobbles gently. There
  are no buzzers, red crosses or "wrong" messages.
- **No reading needed.** An idle pulse shows what to do next, and short spoken prompts use the browser's
  speech engine when it has one. Every game still works without speech.
- **Difficulty adapts.** A quick, clean round steps up one level and a hard round steps down one. After
  two slips on the same item the right target glows as a hint.
- **Home button** in the top-right corner of every screen.
- **Parent view:** press and hold the top-left corner for 3 seconds. Progress is kept in `localStorage`
  on the device and never sent anywhere.

## Run locally

Any static file server works:

```bash
python -m http.server 8000
```

Then open http://localhost:8000. You can also open a game's `index.html` file directly.

## Android app

```bash
./build-apk.sh
```

That builds `dist/EarlyLearningGames-<date>.apk`, an installable app with all 20 games packed inside. It
works offline and needs no permissions. Share that one file. Installing a newer one over an older one keeps
every child's progress.

The app lives in [`android-app/`](android-app/). Its [README](android-app/README.md) covers the one-time
setup, how to install on a phone, how to ship updates safely, and the signing key you must back up.

Android WebView usually has no speech engine, so the games fall back to their visual cues and Web Audio
tones there.

## Debugging

Each game exposes a read-only hook in the browser console, for example `ShapeSort.state()` and
`ShapeSort.progress()`, and logs its key events with a `[Tag]` prefix.
