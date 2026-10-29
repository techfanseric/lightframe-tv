#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SDK=${ANDROID_SDK_ROOT:-${ANDROID_HOME:?Set ANDROID_SDK_ROOT or ANDROID_HOME}}
BT="$SDK/build-tools/35.0.0"
ANDROID=${LIGHTFRAME_ANDROID_JAR:-$SDK/platforms/android-35/android.jar}
B="$ROOT/preview/build"
mkdir -p "$B/gen" "$B/classes" "$B/dex" "$ROOT/dist"
cat > "$B/AndroidManifest.xml" <<'XML'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="local.simple.tvoverlay.preview" android:versionCode="1" android:versionName="1.0"><uses-sdk android:minSdkVersion="28" android:targetSdkVersion="28"/><application android:label="轻幕界面预览" android:theme="@android:style/Theme.Material.NoActionBar.Fullscreen" android:allowBackup="false"><activity android:name="local.simple.tvoverlay.DockPreviewActivity" android:exported="true" android:screenOrientation="landscape"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity></application></manifest>
XML
"$BT/aapt2" compile --dir "$ROOT/apps/overlay/res" -o "$B/resources.zip"
"$BT/aapt2" link -o "$B/base.apk" -I "$ANDROID" --manifest "$B/AndroidManifest.xml" --custom-package local.simple.tvoverlay --java "$B/gen" "$B/resources.zip"
javac --release 8 -classpath "$ANDROID" -d "$B/classes" "$B/gen/local/simple/tvoverlay/R.java" "$ROOT/apps/overlay/src/local/simple/tvoverlay/DockPanel.java" "$ROOT/apps/overlay/src/local/simple/tvoverlay/MenuNavigation.java" "$ROOT/apps/overlay/src/local/simple/tvoverlay/ClockStripView.java" "$ROOT/apps/overlay/src/local/simple/tvoverlay/EntryIcons.java" "$ROOT/apps/overlay/src/local/simple/tvoverlay/LoadingMarkView.java" "$ROOT/apps/overlay/src/local/simple/tvoverlay/StartupThemes.java" "$ROOT/apps/overlay/src/local/simple/tvoverlay/StartupBackdropView.java" "$ROOT/preview/DockPreviewActivity.java"
jar cf "$B/classes.jar" -C "$B/classes" .
"$BT/d8" --lib "$ANDROID" --min-api 28 --output "$B/dex" "$B/classes.jar"
cp "$B/base.apk" "$B/unsigned.apk"
(cd "$B/dex" && zip -q -j "$B/unsigned.apk" classes.dex)
"$BT/zipalign" -f 4 "$B/unsigned.apk" "$B/aligned.apk"
"$BT/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --out "$ROOT/dist/LightframeDockPreview.apk" "$B/aligned.apk"
"$BT/apksigner" verify "$ROOT/dist/LightframeDockPreview.apk"
