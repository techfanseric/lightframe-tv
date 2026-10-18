# Lightframe TV

- Canonical source: `apps/home` and `apps/overlay`. Build with `scripts/build.sh` (JDK 17, SDK 35).
- Keep the two package IDs and shared signature permission stable; both APKs must share a signing key.
- Keep private keys, tokens, subscriptions, router data and device preferences out of Git. Local signing belongs in `.local/`.
- A connected TV may be in active use. Do not press remote keys, launch apps, toggle audio or reboot unless the user has explicitly requested that action. Prefer background builds and read-only checks; the user may choose to validate UI themselves.
- Capture actual device images when asked; never present a mockup, black capture or older image as the current device view.
- Preserve app-row initial focus, MRU/current-app exclusion, wrapping horizontal navigation and smart status visibility.
- UI is a feathered scrim, not live background blur. Do not describe unimplemented blur or unverified playback as working.
