# Changelog

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
