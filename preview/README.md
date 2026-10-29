# Lightframe bottom dock

This is the real native DockPanel rendered in an isolated Android preview activity. Images use a drawn landscape and sample device labels; they are **not screenshots of the user's TV**. Preview actions only log their names. The preview has no HOME role, overlay permission, Internet permission, or device-control code.

## Layout

- Bottom row retains the original MRU ordering and current-app exclusion in production.
- Cards are 116 × 68 dp, with 16 dp gaps and the whole row centered. On narrower screens the width adapts to fit. This is smaller than both earlier drafts and leaves more of the running video visible.
- The latest revision removes every visible entry name. Each card centers its mark in the same 60 × 40 dp icon region. App names remain in accessibility descriptions; the YouTube description identifies TizenTube. Official Netflix and Bilibili marks replace the early preview text substitutes. Each card has its own solid theme color: light YouTube, dark Netflix, pink Bilibili, cool slate settings, warm amber home.
- Focus only scales the selected card to 1.12× over 150 ms. There is no selected border, color change, vertical lift or native rectangular highlight.
- Settings and home control sections appear only while their category or controls have focus. Focus does not execute an action. LEFT/RIGHT wrap, UP enters controls, DOWN returns through the rows to their category, BACK closes the overlay.
- The upper-left clock shows `HH:mm   M月d日 周X` in one consistent 9 sp regular size and weight, updates once per minute and follows system time/timezone changes. It shares the existing status visibility policy, and is removed while the TV screen is off. The upper-right network widget is unchanged.
- The compact themed cards are opaque; the expanded section uses local translucent backing and the clock has no container background. Most of the video remains completely unobscured. No global dim or simulated live blur.

## Builds and verification

Run `sh preview/build.sh` to create `dist/LightframeDockPreview.apk` using the local Android SDK and debug key. `python3 preview/check_navigation.py` targets **only emulator-5580**, tests remote focus, wrap, expansion/collapse, tile alignment and accessibility-only labels, no action on focus, and BACK. It writes the images in `preview/images/`.

Production HOME/overlay compile successfully with the existing build script in unsigned mode. Existing smart-home navigation/command and WebSocket checks pass. Guardian DialogPolicy tests pass. The clock was observed crossing midnight from October 29 to October 30, changing both the 24-hour time and date/weekday.

Production preserves MRU/current-app exclusion and the existing network preflight. Guardian 2.10 supports both original and parallel launcher identities; it is an optional compatibility component. See docs/compatibility.md.

The bottom scrim now fades from transparent at its top to 90% dark at the screen edge. IPTV uses a matching flat broadcast-screen symbol on teal.

Latest panel layout uses four compact columns, 46dp controls, and content-height surfaces pinned above an unchanged dock. Settings include a tested ADB settings-page shortcut. Both production and preview content icons use Phosphor Fill; preview actions only log a label.
