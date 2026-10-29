# v0.2.0 · 2026-10-30

见[发布说明](docs/release-notes-v0.2.0.md)：紧凑覆盖UI、Phosphor Fill、并行包、可回退安装向导、配套开源APK与README图文指南。

# Changelog

## 0.1.7

- Add configurable smart-home spaces with one horizontal group row per space, scrolling in both directions and fixed, bold space headings. Keep app-row initial focus, MRU exclusion and wrapping navigation.
- Use compact 44dp space rows and a divider separating persistent home controls from application switching.
- Keep the main menu, focus and scroll positions after smart-home operations; suspend its timeout for the home-control session. Use custom control submenus that close after selecting an action without reopening the group.
- Configure space/group order, labels and visibility on TV; import group targets and details through private JSON. Add a DUMP-protected layout readback without exposing credentials; retain legacy favorites when no space layout is configured.
- Hide the Clash status strip in TV input mode as well as Bilibili when smart visibility is selected; retain always-on and always-off choices.
- Ship unchanged Home 8.4 (12) and Overlay 8.24 (33) with the existing shared signing key. Personal HA presets, credentials and device preferences remain local.

## 0.1.6

- Fix HDMI input discovery opening the external player during startup or CEC device rediscovery. Adding an input now establishes its baseline; automatic switching still requires an observed disconnected-to-connected transition. Manual source selection remains available.
- Bump Overlay to 8.23 (32); keep Home and the shared signing key unchanged.
- Confirm the original HDMI launch came from the overlay in on-device boot logs. Build/signature checks and background installation passed, with TizenTube remaining in front and the overlay service restored. Standby wake and cold boot validation remain pending.

## 0.1.5

- Add a native Home Assistant panel with room/device/entity discovery, cached registry fallback, read-only states and selected quick controls above the app row. Preserve app-row initial focus, MRU/current-app exclusion, wrapping navigation and smart status visibility.
- Support climate power and Celsius temperature adjustment, covers, fans, switches, lights and vacuum controls. Re-read climate power state before toggling; keep unsupported or disabled entities read-only.
- Provision HA credentials through a DUMP-protected local import, encrypt tokens with Android Keystore and retain selected favorite order. Add an explicit microphone statistics diagnostic; speech recognition and Assist voice control remain unverified.
- Rank IPTV entries within each group using an explicit Chinese-first editorial priority, followed by selected overseas broadcasters and natural station-name order. Keep stream alternatives adjacent; no audience measurement or live popularity is claimed.
- Add a separate region-based IPTV source, retaining content categories and stream identities; preserve the selected source with `configure-iptv.py --keep-current`.
- Enrich missing IPTV categories using reviewed station identities and explicit subjects; keep uncertain entries unclassified.
- Use native Chinese names for domestic stations and append established Chinese names to overseas brands. Preserve EPG lookup names and migrate saved favorite names on import; record local audit and migration files.
- Translate IPTV categories into Chinese and merge compound tags into one primary category; label undefined or unknown tags as “其他／待分类”. Preserve stream URLs and EPG identifiers.
- Add `prepare-iptv.py --offline` to regenerate compatible playlists from local snapshots without refreshing sources.
- Refresh both READMEs with release versions, smart-home setup, IPTV changes, remote navigation and an explicit old-screenshot caption.
- Remove the unused legacy ad-mute client and its embedded LAN endpoint.
- Ship unchanged Home 8.4 (12) and Overlay 8.22 (31) with the existing shared signing key. Private configuration, third-party APKs and playlist snapshots remain local.

## 0.1.4

- Add an IPTV entry for MyTV (`top.yogiczy.mytv.tv`) to both app catalogs, startup selection and last-used-app recognition.
- Extend MRU ordering, foreground exclusion and runtime status to all configured apps; distribute app tiles across the existing row while preserving initial focus and wrapping navigation.
- Add local preparation, installation and source-import tools for the official MyTV player and public IPTV playlists. Normalize compatible entries, deduplicate streams and place Chinese sources first.
- Fix configuration requests for the player’s AndroidAsync server by omitting Content-Type on bodyless GET requests. Preserve existing sources and preferences during repeat imports.
- Ship Home 8.4 (12) and Overlay 8.17 (26) with the existing shared signing key. Third-party APKs, playlist snapshots and device settings remain local and are not release assets.
- Background installation and source import were verified on the target Android 9 TV: six sources loaded, with 178 groups and 10,630 stream entries in the combined list. The user approved the experience; individual stream availability is not guaranteed.

## 0.1.3

- Add subtle 0.45dp outer outlines at 40% black opacity to status text and network/traffic icons, preserving white stroke thickness.
- Increase graph history from 40 to 80 samples with fixed spacing and right-aligned accumulation; retain the five-second sampling interval (about 6.5 minutes of history).
- Refine graph height to balance the original range with the visible rate-digit height.
- Ship Overlay 8.16 (25) with unchanged Home 8.3 (11), using the existing shared signing key.
- Build/signature checks passed; visual adjustments were reviewed by the user on the TV through background updates.

## 0.1.2

- Display TizenTube in both app catalogs, covering the menu, startup settings and status messages.
- Give its launcher tile a distinct teal T icon instead of inheriting the client icon. Keep the TizenTube package target and existing startup preferences.
- Ship Home 8.3 (11) and Overlay 8.15 (24) with the existing shared signature.

## 0.1.1

- Route the YouTube entry, saved startup choices and foreground/MRU recognition to TizenTube Cobalt (`io.gh.reisxd.tizentube.cobalt`).
- Remove the ad-audio menu row and its requests; keep initial focus on the app row and use the compact two-row menu.
- Restore the status overlay after an in-place overlay update without opening a video app or menu.
- Ship Home 8.2 (10) and Overlay 8.14 (23), signed with the existing shared key. Keep installed preferences and the separately installed official YouTube app.
- Build/signature checks and background installation passed on the target TV; HOME interaction and reboot recovery were not retested for this release.

## 0.1.0

- Named the project 轻幕 · Lightframe TV; added Chinese and English app labels and documentation.
- Extracted the HOME relay and overlay into a portable, shared-signing build.
- Includes the two-row MRU menu, source switching, last-app startup and network readiness gate.
- Includes the smart Clash status strip and the YouTube-only optional ad-audio switch.
- Uses settings-size typography for controls and a continuous feathered scrim.
- Preserves installed package identities and private user preferences.
