# Lightframe TV · 轻幕

[中文](README.md)

A lightweight Android TV launcher built around the remote and the programme you are watching. Press HOME to switch apps or adjust a setting, then return to the video.

<!-- SCREENSHOT: awaiting a valid capture of the current video with the HOME menu open. -->

## Features

- A left-aligned, vertically centred translucent menu; only the focused item has a background.
- Recent-app ordering, current-app exclusion, and wrapping horizontal navigation for YouTube, Bilibili and Netflix.
- Direct settings for input source, startup app, automatic startup, status bar, FlClash and system settings.
- Startup defaults to the last video app; network readiness gates YouTube / Netflix automatic opening.
- A small Clash status strip with node, latency, rates and a traffic graph. **Smart** mode is the default: hidden in Bilibili and restored elsewhere. Always-on and always-off modes remain available.
- A YouTube-only “Mute ad audio” row, off by default. Initial focus remains on the app row; UP reaches the switch. This controls a separately configured LAN helper and does not disable automatic skipping.
- Firmware-specific HDMI hotplug and source switching.

## Build and install

Requires JDK 17, Android SDK Platform 35, Build Tools 35.0.0, Python 3 and `zip`.

```sh
export JAVA_HOME=/path/to/jdk-17
export ANDROID_SDK_ROOT=/path/to/android-sdk
./scripts/build.sh
export TV_ADB_SERIAL=your-tv-ip:5555
./scripts/install.sh "$TV_ADB_SERIAL" --set-home
```

Outputs: `dist/LightframeHome.apk` and `dist/LightframeOverlay.apk`. Both packages must share a signing key. Fresh checkouts create a local development key under `.local/`; updates to an existing installation require the original key. `LIGHTFRAME_KEYSTORE`, `LIGHTFRAME_KEY_ALIAS`, `LIGHTFRAME_STORE_PASSWORD` and `LIGHTFRAME_KEY_PASSWORD` override signing. Keys and generated outputs are ignored by Git. Existing tool paths can be selected with `LIGHTFRAME_BUILD_TOOLS` and `LIGHTFRAME_ANDROID_JAR`.

Installation grants overlay and usage access. `--set-home` selects the default HOME component; omit it to only install and grant access. The script does not reboot, clear data or disable the stock launcher. Press HOME yourself when ready.

Video apps, accounts, VPN configuration and subscriptions are not bundled. See [integration setup](docs/integrations.md) for the local Clash controller and optional ad-mute helper.

## Architecture and limitations

`apps/home` contains the transparent HOME relay and firmware bridges. `apps/overlay` contains the independent foreground overlay service, menu, status strip and network gate. Communication is protected by a signature permission. Package IDs remain `local.simple.tvhome` and `local.simple.tvoverlay` to preserve existing installations and preferences.

Verified on an ARM32 Xiaomi TV running Android 9 / API 28 (alita / MiTV-MFTP0). Native settings, input sources and remote behaviour are firmware-specific. The menu uses a feathered local scrim, not live cross-app background blur; it does not capture the screen. Native voice invocation is supported, but the device's speech recognition failure remains unresolved. Video-service certification, DRM and playback compatibility are outside the launcher's guarantees.

Lucide icons are licensed under ISC; source SVGs, checksums and the licence are preserved in `apps/overlay/third-party/lucide/`. Installed-app icons are loaded at runtime.
