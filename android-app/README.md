# Android app

A small native Android app: one full-screen WebView that plays the games in this repo's
`index.html` and `games/` folder. The games are packed inside the APK, so the app works offline
and needs no permissions at all.

## Build the APK

```bash
./build-apk.sh
```

Run it from the repo root. The result is `dist/EarlyLearningGames-<date>.apk`, which is the one file
you share. Every build packs whatever `index.html` and `games/` contain at that moment, so there is
nothing to copy by hand.

### One-time setup on a new Mac

```bash
brew install openjdk@17
brew install --cask android-commandlinetools
sdkmanager --sdk_root="$HOME/Library/Android/sdk" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Then restore your backed-up signing key folder to `~/.android-keys/early-learning-games/` (see below).
`build-apk.sh` refuses to build without it, and it also refuses to finish if the APK is signed with any
key other than the release key. A wrong key can therefore never slip out by accident.

## Installing on a phone

1. Send the `.apk` file over WhatsApp, Google Drive, email or USB.
2. On the phone, tap the file. Android asks once to allow installs from that app (Files, Chrome,
   WhatsApp…). Allow it.
3. Play Protect may say the app is from an unknown developer. Choose **Install anyway**. This happens
   because the app is not on the Play Store, not because something is wrong.

Works on Android 7.0 and newer.

## Shipping an update

Change the games, run `./build-apk.sh` again, and share the new APK. Tapping it on a phone shows
**Update** rather than Install, and all progress is kept.

That works because of three things that must **never change**:

| What | Where | If it changes |
|------|-------|---------------|
| App ID `io.github.gokul11122.earlylearning` | `app/build.gradle.kts` | Android installs it as a second, separate app with no progress |
| The signing key | `~/.android-keys/early-learning-games/` | Phones refuse the update ("App not installed"). The only fix is uninstalling, which deletes progress |
| The page address `https://appassets.androidplatform.net/assets/` | `MainActivity.kt` | Progress is stored per address, so it would look wiped |

The version number goes up automatically on every build (it is the build time), so a newer APK always
installs over an older one. A phone refuses to install an *older* APK over a newer one.

### Keeping progress safe when you change a game

Each game saves to `localStorage` under `<slug>.v1` (for example `shape-sorting.v1`).

- Don't rename a game's folder or its `STORAGE_KEY`. Progress lives under that name.
- If a game's saved data needs a new shape, read the old `v1` data and convert it. A game that finds
  data it doesn't recognise starts fresh, so skipping the conversion silently resets that game's progress.
- Keep `loadProgress()` wrapped in `try/catch`, as it is now, so damaged data can never crash a game.

## Back up the signing key (important)

Copy the whole folder `~/.android-keys/early-learning-games/` (`release.jks` + `signing.properties`)
somewhere safe and private, such as a password manager, a private cloud folder or a USB stick. It is
the only way to publish updates to phones that already have the app, and it cannot be recreated.
Never put it in the git repo (`.gitignore` blocks it).

## How the app behaves

- Back button: inside a game it returns to the game list; on the game list it leaves the app.
- Rotating the device does not restart the current game.
- Leaving the app pauses the game's animation and sound.
- If Android kills the web renderer (low memory), the app reopens the game list instead of crashing.
- Phone "large text" settings don't distort the game layouts.
- Progress is included in Android's own phone backup and phone-to-phone transfer.

## Debugging

```bash
adb logcat -s EarlyLearning
```

That shows every game's `console.log` output. A debug build (`./gradlew installDebug` in this folder) installs as a
separate app, "io.github.gokul11122.earlylearning.debug", next to the real one, so it never touches the
real app's progress. Chrome's `chrome://inspect` on the Mac can inspect its WebView live.
