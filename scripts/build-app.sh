#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
APP=${1:?Usage: build-app.sh home|overlay}
case "$APP" in home) OUTPUT=LightframeHome.apk;; overlay) OUTPUT=LightframeOverlay.apk;; *) echo 'Expected home or overlay' >&2; exit 2;; esac
PROJECT="$ROOT/apps/$APP"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
BT="${LIGHTFRAME_BUILD_TOOLS:-${SDK:+$SDK/build-tools/35.0.0}}"
ANDROID="${LIGHTFRAME_ANDROID_JAR:-${SDK:+$SDK/platforms/android-35/android.jar}}"
if [ -n "${JAVA_HOME:-}" ]; then PATH="$JAVA_HOME/bin:$PATH"; export PATH; fi
for file in "$BT/aapt2" "$BT/d8" "$BT/apksigner" "$BT/zipalign" "$ANDROID"; do
 if [ ! -f "$file" ]; then echo 'Install Android SDK Platform 35 / Build Tools 35.0.0 or set LIGHTFRAME_BUILD_TOOLS and LIGHTFRAME_ANDROID_JAR.' >&2; exit 1; fi
done
command -v java >/dev/null
command -v keytool >/dev/null
command -v python3 >/dev/null
KS="${LIGHTFRAME_KEYSTORE:-$ROOT/.local/signing.jks}"
ALIAS="${LIGHTFRAME_KEY_ALIAS:-tvhome}"
export LIGHTFRAME_STORE_PASSWORD="${LIGHTFRAME_STORE_PASSWORD:-android}"
export LIGHTFRAME_KEY_PASSWORD="${LIGHTFRAME_KEY_PASSWORD:-$LIGHTFRAME_STORE_PASSWORD}"
if [ "${LIGHTFRAME_UNSIGNED:-0}" != "1" ] && [ ! -f "$KS" ]; then
 if [ -n "${LIGHTFRAME_KEYSTORE:-}" ]; then echo 'Configured signing keystore is missing.' >&2; exit 1; fi
 mkdir -p "$ROOT/.local"
 keytool -genkeypair -keystore "$KS" -storepass:env LIGHTFRAME_STORE_PASSWORD -keypass:env LIGHTFRAME_KEY_PASSWORD -alias "$ALIAS" -dname 'CN=Lightframe TV Development' -keyalg RSA -keysize 2048 -validity 10000
 chmod 600 "$KS"
fi
BUILD="$PROJECT/build"
rm -rf "$BUILD/classes" "$BUILD/dex"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$ROOT/dist"
"$BT/aapt2" compile --dir "$PROJECT/res" -o "$BUILD/resources.zip"
if [ -d "$PROJECT/third-party" ]; then
 "$BT/aapt2" link -o "$BUILD/base.apk" -I "$ANDROID" --manifest "$PROJECT/AndroidManifest.xml" --java "$BUILD/gen" -A "$PROJECT/third-party" "$BUILD/resources.zip"
else
 "$BT/aapt2" link -o "$BUILD/base.apk" -I "$ANDROID" --manifest "$PROJECT/AndroidManifest.xml" --java "$BUILD/gen" "$BUILD/resources.zip"
fi
find "$PROJECT/src" "$BUILD/gen" -name '*.java' > "$BUILD/sources.list"
java -Xmx128m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m com.sun.tools.javac.Main -encoding UTF-8 --release 8 -classpath "$ANDROID" -d "$BUILD/classes" @"$BUILD/sources.list"
python3 - "$BUILD/classes" "$BUILD/classes.jar" <<'PY'
import pathlib,sys,zipfile
root=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(sys.argv[2],'w',zipfile.ZIP_DEFLATED) as z:
 for p in root.rglob('*.class'):z.write(p,p.relative_to(root))
PY
JAVA_OPTS=-Xmx128m "$BT/d8" --lib "$ANDROID" --min-api 28 --output "$BUILD/dex" "$BUILD/classes.jar"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
(cd "$BUILD/dex" && zip -q -j "$BUILD/unsigned.apk" classes.dex)
"$BT/zipalign" -f 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
if [ "${LIGHTFRAME_UNSIGNED:-0}" = "1" ]; then
 cp "$BUILD/aligned.apk" "$ROOT/dist/${OUTPUT%.apk}-unsigned.apk"
 echo "$ROOT/dist/${OUTPUT%.apk}-unsigned.apk"
 exit 0
fi
"$BT/apksigner" sign --ks "$KS" --ks-key-alias "$ALIAS" --ks-pass env:LIGHTFRAME_STORE_PASSWORD --key-pass env:LIGHTFRAME_KEY_PASSWORD --out "$ROOT/dist/$OUTPUT" "$BUILD/aligned.apk"
"$BT/apksigner" verify "$ROOT/dist/$OUTPUT"
echo "$ROOT/dist/$OUTPUT"
