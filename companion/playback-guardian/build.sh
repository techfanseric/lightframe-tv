#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SDK=${ANDROID_SDK_ROOT:-${ANDROID_HOME:?Set ANDROID_SDK_ROOT or ANDROID_HOME}}
BT=${LIGHTFRAME_BUILD_TOOLS:-$SDK/build-tools/35.0.0}
ANDROID=${LIGHTFRAME_ANDROID_JAR:-$SDK/platforms/android-35/android.jar}
mkdir -p "$ROOT/build/gen" "$ROOT/build/classes" "$ROOT/build/dex" "$ROOT/dist" "$ROOT/.local"
"$BT/aapt2" compile --dir "$ROOT/res" -o "$ROOT/build/resources.zip"
"$BT/aapt2" link -o "$ROOT/build/base.apk" -I "$ANDROID" --manifest "$ROOT/AndroidManifest.xml" --java "$ROOT/build/gen" "$ROOT/build/resources.zip"
find "$ROOT/src" "$ROOT/build/gen" -name '*.java' > "$ROOT/build/sources.list"
javac -encoding UTF-8 --release 8 -classpath "$ANDROID" -d "$ROOT/build/classes" @"$ROOT/build/sources.list"
jar cf "$ROOT/build/classes.jar" -C "$ROOT/build/classes" .
"$BT/d8" --lib "$ANDROID" --min-api 28 --output "$ROOT/build/dex" "$ROOT/build/classes.jar"
cp "$ROOT/build/base.apk" "$ROOT/build/unsigned.apk"
(cd "$ROOT/build/dex" && zip -q -j "$ROOT/build/unsigned.apk" classes.dex)
"$BT/zipalign" -f 4 "$ROOT/build/unsigned.apk" "$ROOT/build/aligned.apk"
if [ ! -f "$ROOT/.local/signing.jks" ]; then
 keytool -genkeypair -keystore "$ROOT/.local/signing.jks" -storepass android -keypass android -alias observer -dname 'CN=Local StreamGuard Observer' -keyalg RSA -keysize 2048 -validity 10000
 chmod 600 "$ROOT/.local/signing.jks"
fi
"$BT/apksigner" sign --ks "$ROOT/.local/signing.jks" --ks-key-alias observer --ks-pass pass:android --key-pass pass:android --out "$ROOT/dist/StreamGuardObserver.apk" "$ROOT/build/aligned.apk"
"$BT/apksigner" verify "$ROOT/dist/StreamGuardObserver.apk"
printf '%s\n' "$ROOT/dist/StreamGuardObserver.apk"
