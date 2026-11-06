# FireRed Native

Run legally-owned Game Boy Advance titles by executing their ARM code **natively on the phone's
CPU**, wrapped by an exception-virtualizing hardware layer. In the steady state the ROM's own
instructions are executed untranslated; graphics/audio/DMA/timer services come from a companion
hardware layer. If a device cannot run 32-bit app processes, the same hardware layer fronts an
AArch64 DynaRec CPU front-end instead.

No ROM files, no Nintendo assets, and no console code are included in this repository —
you provide your own legally obtained ROMs.

## Status: M0 — ABI capability probe

The first deliverable is a tiny diagnostic app that decides the core variant:

- **GO** — `armeabi-v7a` is listed in `Build.SUPPORTED_ABIS` → a 32-bit app process can run on this
  device → **literal native execution** ships (32-bit core).
- **NO-GO** — 32-bit apps cannot exist on this device (e.g. Snapdragon 8 Gen 3+ / Pixel 8+) → the
  core pivots to the **AArch64 DynaRec** variant on day one.

## Getting the probe APK

1. Open the repo on GitHub → **Actions** → latest `android-ci` run.
2. Under **Artifacts**, download `firered-native-debug-apk` and unzip it (artifacts are zipped).
3. On the phone: allow *Install unknown apps* for your browser/file manager, then install the APK.
   Or plug the phone into a PC and `adb install app-debug.apk`.

## Reporting back

Open **ABI Probe**, then tap **Copy report** (or **Share report**) and send the result back along
with the GO/NO-GO verdict and the exact phone model. That data locks in the core variant.

## Development

- No local Android SDK is required: CI (`.github/workflows/android-ci.yml`) builds the APK on every
  push and pulls the SDK itself.
- Toolchain: JDK 17, Gradle 8.9 (wrapper), Android Gradle Plugin 8.5.2, Kotlin 2.0.20.
- Docs: [`docs/ROADMAP.md`](docs/ROADMAP.md), [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Legal

- This repository contains only original code and diagnostics.
- The user supplies their own, legally owned, game backups.
- Product names referenced (e.g. Pokémon FireRed) are trademarks of their owners and are used here
  descriptively; this project is unaffiliated and non-commercial.
