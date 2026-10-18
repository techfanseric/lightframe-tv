# Lightframe TV · 轻幕

[中文](README.md)

**No home screen. Boot straight into the app you want to watch.**

[Download the latest release](https://github.com/techfanseric/lightframe-tv/releases/latest)

Lightframe replaces the traditional launcher page with a lightweight menu over the current video. Press HOME, pick another app, and switch directly without visiting a separate desktop.

![Lightframe TV app switcher over the current video](docs/images/launcher-menu.png)

On-device capture of an earlier version: the HOME app switcher and settings appear over the video. v0.1.1 removes the ad-audio row shown at the top, leaving the app and settings rows.

## YouTube client

Starting with **v0.1.1**, Lightframe consistently uses [TizenTube Cobalt](https://github.com/reisxd/TizenTubeCobalt) for its YouTube entry, app switching, chosen startup app, last-used-app opening and foreground recognition. The menu label remains “YouTube”.

Install TizenTube Cobalt separately (currently validated version: 2.0.3). The official YouTube app may remain installed for manual fallback; Lightframe does not switch back to it automatically. The user reported normal playback and no ads during this trial, which does not establish ad-free playback for every video.

The current menu has **two rows: apps and settings**. The ad-audio row and its menu network calls have been removed. Overlay updates restore the background service without opening the menu or switching the video. HOME interaction and boot restoration remain for user validation when convenient.

## What it does

- **No separate home screen:** stay centred on the current app instead of navigating a desktop.
- **Boot directly into an app:** choose YouTube, Bilibili or Netflix, or follow the last-used app (the default). Automatic opening can be disabled independently.
- **Switch straight from the video:** HOME opens the switcher, excludes the current app and selects the previous app. Left/right wraps; up/down changes rows.
- **Detect and start the network proxy:** check the configured FlClash VPN and request startup when disconnected. YouTube / Netflix opening waits for controller and target-network readiness; Bilibili opens directly.
- **Network status at a glance:** a small top strip shows proxy node, latency, upload/download rates and a traffic graph.
- **Smart visibility by default:** hide the status strip in Bilibili and restore it elsewhere. Always-on and always-off remain available and are remembered.
- **Direct settings:** input source, startup app, automatic startup, status strip, FlClash and system settings are immediately visible in one row.
- **Restrained presentation:** left-aligned icons and labels, a continuous translucent scrim, and one focused container instead of many cards.
- **Native input integration:** source selection and HDMI hotplug switching on the verified Xiaomi firmware.

Proxy startup requires an already configured FlClash installation, subscription and system VPN permission. No proxy service, video apps or member accounts are bundled.

Download **LightframeHome.apk** and **LightframeOverlay.apk** together from [Releases](https://github.com/techfanseric/lightframe-tv/releases/latest). The HOME relay and overlay are separate packages and must both be installed. Release APKs do not require an SDK to use.

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

Video apps, accounts, VPN configuration and subscriptions are not bundled. See [integration setup](docs/integrations.md) for the local Clash controller. TizenTube Cobalt is installed separately.

## Architecture and limitations

`apps/home` contains the transparent HOME relay and firmware bridges. `apps/overlay` contains the independent foreground overlay service, menu, status strip and network gate. Communication is protected by a signature permission. Package IDs remain `local.simple.tvhome` and `local.simple.tvoverlay` to preserve existing installations and preferences.

Verified on an ARM32 Xiaomi TV running Android 9 / API 28 (alita / MiTV-MFTP0). Native settings, input sources and remote behaviour are firmware-specific. The menu uses a feathered local scrim, not live cross-app background blur; it does not capture the screen. Native voice invocation is supported, but the device's speech recognition failure remains unresolved. Video-service certification, DRM and playback compatibility are outside the launcher's guarantees.

Lucide icons are licensed under ISC; source SVGs, checksums and the licence are preserved in `apps/overlay/third-party/lucide/`. Installed-app icons are loaded at runtime.
