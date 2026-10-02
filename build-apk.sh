#!/usr/bin/env bash
# Builds the installable Android app from the games in this repo.
#
#   ./build-apk.sh            -> dist/EarlyLearningGames-<version>.apk
#   ./build-apk.sh --create-first-key   (only ever once, before the very first release)
#
# Share that one .apk file. Installing a newer one over an older one keeps all progress,
# as long as it was signed with the same key (see KEY_DIR below — back it up!).
set -euo pipefail

REPO="$(cd "$(dirname "$0")" && pwd)"
KEY_DIR="${EARLY_LEARNING_KEY_DIR:-$HOME/.android-keys/early-learning-games}"
SIGNING="$KEY_DIR/signing.properties"

# Fingerprint of THE release key. Phones with the app only accept updates signed with it, so the
# build refuses to finish with any other key (e.g. a fresh one made by accident on a new Mac).
RELEASE_CERT_SHA256="846ddc0797d8c6c9caa2ce3ea40286243b2cd5affeef003249ab6876e8bb74dc"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export EARLY_LEARNING_SIGNING="$SIGNING"
export PATH="$JAVA_HOME/bin:$PATH"   # apksigner runs whatever `java` is on PATH

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "Java 17 not found at $JAVA_HOME. Install it with: brew install openjdk@17" >&2; exit 1
fi
if [[ ! -d "$ANDROID_HOME/platforms" ]]; then
  echo "Android SDK not found at $ANDROID_HOME. See android-app/README.md (one-time setup)." >&2; exit 1
fi

# The release key. Every future update MUST be signed with this same key, otherwise phones refuse
# the update and the only way forward is an uninstall that deletes the children's progress.
if [[ ! -f "$SIGNING" && "${1:-}" != "--create-first-key" ]]; then
  echo "No signing key at $KEY_DIR." >&2
  echo "Restore your backup of that folder (release.jks + signing.properties) and run again." >&2
  echo "Do NOT create a new key: APKs signed with it can never update phones that have the app." >&2
  exit 1
fi
if [[ ! -f "$SIGNING" ]]; then
  if [[ -f "$KEY_DIR/release.jks" ]]; then
    echo "$KEY_DIR/release.jks exists but signing.properties is missing — restore both files." >&2; exit 1
  fi
  echo "No signing key yet — creating one in $KEY_DIR"
  mkdir -p "$KEY_DIR"; chmod 700 "$KEY_DIR"
  PASS="$(openssl rand -hex 24)"
  "$JAVA_HOME/bin/keytool" -genkeypair -v -keystore "$KEY_DIR/release.jks" -storetype PKCS12 \
    -alias release -keyalg RSA -keysize 4096 -validity 36500 \
    -storepass "$PASS" -keypass "$PASS" \
    -dname "CN=Early Learning Games, O=gokul11122" >/dev/null
  printf 'storeFile=release.jks\nstorePassword=%s\nkeyAlias=release\nkeyPassword=%s\n' "$PASS" "$PASS" > "$SIGNING"
  chmod 600 "$SIGNING" "$KEY_DIR/release.jks"
  echo
  echo "  !!! BACK UP THE FOLDER $KEY_DIR NOW (e.g. a private cloud drive or USB stick)."
  echo "  !!! Without it you can never publish an update that keeps the kids' progress."
  echo
fi

cd "$REPO/android-app"
./gradlew --quiet --no-daemon clean assembleRelease

APK_SRC="app/build/outputs/apk/release/app-release.apk"

# Prove the APK is signed with THE release key before it goes anywhere near dist/.
BUILD_TOOLS="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
CERTS="$("$BUILD_TOOLS/apksigner" verify --print-certs "$APK_SRC")"
CERT_SHA256="$(grep -m1 'SHA-256' <<<"$CERTS" | sed 's/.*: //')"
if [[ "$CERT_SHA256" != "$RELEASE_CERT_SHA256" ]]; then
  echo "Built APK is signed with key $CERT_SHA256," >&2
  echo "not the release key $RELEASE_CERT_SHA256. It could not update installed apps — not copied." >&2
  echo "(If this is truly the first release with a brand-new key, put the new value in RELEASE_CERT_SHA256.)" >&2
  exit 1
fi
echo "Signed with the release key ($CERT_SHA256)"

VERSION="$(sed -n 's/.*"versionName": *"\([^"]*\)".*/\1/p' app/build/outputs/apk/release/output-metadata.json)"
mkdir -p "$REPO/dist"
OUT="$REPO/dist/EarlyLearningGames-${VERSION:-latest}.apk"
cp "$APK_SRC" "$OUT"

echo
echo "Built: $OUT  ($(du -h "$OUT" | cut -f1))"
