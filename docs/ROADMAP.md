# FireRed Native — Roadmap

Status: **M0 shipped, awaiting device verdict.**

## Milestones

| # | Gate | Definition of done (on the phone) | Status |
|---|------|-----------------------------------|--------|
| M0 | ABI probe | APK installs on the device and reports a clear GO / NO-GO for 32-bit (AArch32) app processes | ✅ built |
| M1 | Launcher shell | Compose UI: ROM library, SAF import with header-based region detection (BPRE/BPGE/BPRJ), control skin editor, settings, in-app diagnostics log page | planned |
| M2 | Literal execution proof | Guest code runs natively in a 32-bit process at real GBA addresses (or AArch64 DynaRec variant if the device is 64-bit-only); GameFreak splash renders | planned |
| M3 | Title + overworld | Title screen, menus, overworld walking (PPU, DMA, timers, poll-blocking) | planned |
| M4 | Battles, audio, saves | Battle flow, AAudio audio pipeline, Flash 128 K save persistence | planned |
| M5 | Polish pass | Latency tuning, scaling filters, UI/UX pass | planned |

## v2 backlog (core-dependent, deferred)
- Save states, fast-forward, rewind
- Cheat support (Action Replay / CodeBreaker)
- Screenshot / video capture
- Multi-slot save wiping/backup tools

## Pivot policy
M0's probe result is final for v1:
- `armeabi-v7a` present in `Build.SUPPORTED_ABIS` → literal-native execution ships (32-bit core).
- Absent → the same hardware/TrapLayer services front an AArch64 DynaRec core instead.

Both variants share the hardware layer; only the CPU front-end differs. No plan content is wasted either way.
