# FireRed Native — Architecture (M0 baseline)

Goal: execute a legally-owned GBA ROM's ARM code **natively on the phone CPU**, wrapped by an
exception-virtualizing hardware layer. If the device cannot run 32-bit app processes, the same
hardware layer fronts an AArch64 DynaRec CPU front-end instead.

```
+--------------------------------------------------------------+
| Kotlin shell (launcher UI)                                   |
|   ROM library / SAF import / skin editor / settings / diag   |
+--------------------------+-----------------------------------+
                           |  surface, AAudio, input events
+--------------------------v-----------------------------------+
| Core runtime (C / AArch32-first, AArch64-DynaRec variant)     |
|                                                              |
|   CPU FRONT-END (one of)                                     |
|     A) literal: guest executed in-place at real GBA addresses |
|        - ROM: MAP_PRIVATE|EXEC at 0x08000000                 |
|        - EWRAM/IWRAM/VRAM/PAL/OAM: anon RAM at real addresses |
|        - IO 0x04000000 + flash 0x0E000000: PROT_NONE traps   |
|        - SWI sites rewritten to BL thunks (relocation)       |
|        - IRQ/vblank/timer events injected via signals that   |
|          save/restore guest context (exception virtualization)|
|     B) DynaRec: ARMv4T -> AArch64 blocks, cached             |
|   HARDWARE LAYER (shared)                                    |
|     PPU compositor, DMA engine, timer scheduler, keypad,     |
|     audio DMA/fifos -> AAudio, Flash 128K chip protocol      |
|     poll-blocking: reads of VCOUNT/IF/KEYINPUT park until    |
|     the watched event fires (kills busy-wait + timing skew)  |
+--------------------------------------------------------------+
```

Key properties:
- The steady-state game code is never translated (variant A) — the phone's CPU executes the ROM's
  own instructions; only exceptions and IO accesses are virtualized.
- Timing is anchored to real time (audio clock, ~59.7 Hz frame cadence), not cycle counting;
  CPU-side spin loops park instead of racing.
- Everything except the CPU front-end is ABI-portable so the DynaRec pivot changes one layer.
