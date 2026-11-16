# Figma: status-bar pills

File: ELAL (`FAJOf7GeT7JDwi0nEy6CXM`), read 2026-11-10.

| Node | What it shows | Background | Text | Icon |
|---|---|---|---|---|
| [6944:19116](https://www.figma.com/design/FAJOf7GeT7JDwi0nEy6CXM/ELAL?node-id=6944-19116&m=dev) | Pill background only | `#1B358F`, radius 28 | — | — |
| [6944:19137](https://www.figma.com/design/FAJOf7GeT7JDwi0nEy6CXM/ELAL?node-id=6944-19137&m=dev) | Gate, on time | `#1B358F` | `E4`, white, Ping HL 12 | departure arrow in a filled circle |
| [6944:19267](https://www.figma.com/design/FAJOf7GeT7JDwi0nEy6CXM/ELAL?node-id=6944-19267&m=dev) | Gate, delayed | `#FFB936` | `E4`, black | same arrow, black circle |
| [6944:19247](https://www.figma.com/design/FAJOf7GeT7JDwi0nEy6CXM/ELAL?node-id=6944-19247&m=dev) | In flight, time left | `#1B358F` | `6:12h`, white | plane in a filled circle |

Layout: padding 6×4, gap 6, icon 12×12 (rotated 180° in the design).

## How it maps to Android

The system draws the pill; the app supplies only its content:
- **Icon** → `setSmallIcon`: `ic_live_update_chip_departure` (arrow) / `ic_live_update_chip_flight` (plane, DEPARTED and
  LANDING_SOON). Single-colour masks with the shape cut out, because the system tints small icons.
- **Text** → `setShortCriticalText` from `ChipTextResolver`.
- **Background** → **not controllable (verified on device 2026-11-10):** with `setColor(#FFB936)` the pill background
  stayed the same; the system picks it. `setColorized` is not allowed (it breaks promotion). `setColor` is kept: it
  still tints the app accent inside the shade.
- **Text colour and font** (white/black, Ping HL) → not controllable; the system picks them.
