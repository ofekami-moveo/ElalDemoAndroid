# Current data contract (frozen for this spec)

**Snapshot:** 2026-11-10, from `update-LUN-ui` (`8655dc1`).
**Rule:** this spec is **UI only**. The FCM payload, the demo parser and `FlightContentState` stay exactly as
described here. The UI work reads these fields; it doesn't add, rename or re-parse any of them.

## How data reaches the SDK

1. FCM data message → `app/.../firebase/FlightMessagingService.onMessageReceived` (all values are strings).
2. `event` (lower-cased):
   - `end` → `LiveUpdateManager.endJourney(context)`.
   - `start`, or no active journey → `startJourney(context, state, serverNowMs = message.sentTime)`.
   - anything else → `updateJourney(context, state, serverNowMs = message.sentTime)`.
3. If the in-memory singleton is empty (process killed), `restoreFromPrefs(startServiceIfActive = false)` runs first.
4. `Map<String, String>.toFlightContentState(fallback)` builds the state. On an update, a missing or empty key falls
   back to the previous state's value (`fallback`), except `baggageCarousel`, which never merges.
5. `message.sentTime` (FCM server time) is the trusted "now" for `ServerClock`; `0` means "use the device clock".

## Payload keys → `FlightContentState`

| Push key | Field | Type | Parsing / default |
|---|---|---|---|
| `event` | — | `start` / `update` / `end` | routing only |
| `activityId` | `activityId` | String | merge, else `""` |
| `flightNumber` | `flightNumber` | String | merge, else `"--"` |
| `flightDate` | `flightDate` | String | merge, else `""` |
| `direction` | `direction` | `DEPARTURE` / `ARRIVAL` | upper-cased; **required** on start |
| `carrier` | `carrier` | `EL_AL` / `SUN_DOR` | upper-cased; **required** on start |
| `pnr` | `pnr` | String | merge, else `""` |
| `origin` | `origin` | String (IATA) | **required** on start |
| `destination` | `destination` | String (IATA) | **required** on start |
| `departureTime` | `departureTime` | Instant | `ISO_LOCAL_DATE_TIME`, **naive airport-local** time stored as if UTC; **required** on start |
| `arrivalTime` | `arrivalTime` | Instant | same as `departureTime`; **required** on start |
| `departureTimezoneOffset` | `departureTimezoneOffset` | String? | raw, e.g. `-04`, `+02:00`; merge, else null |
| `terminal` | `terminal` | String | merge, else `"--"` |
| `arrivalTerminal` | `arrivalTerminal` | String | merge, else `"--"` |
| `gate` | `gate` | String | merge, else `"--"` |
| `baggageCarousel` | `baggageCarousel` | String | **no merge**; blank/absent → `"--"` |
| `checkInCounters` | `checkInCounters` | String | merge, else `"--"` |
| `boardingZone` | `boardingZone` | String | merge, else `"--"` |
| `stage` | `stage` | `Stage` (13 values) | upper-cased; **required on every message** |
| `progressFraction` | `progressFraction` | Double | merge, else `0.0` |
| `flightStatus` | `flightStatus` | String | e.g. `ON-TIME`; merge, else `"--"` |
| `flightStatusText` | `flightStatusText` | String | merge, else `""` |
| `title` | `title` | String | merge, else `""` |
| `stageTitle` | `stageTitle` | String | merge, else `""` (blank in some Postman samples) |
| `message` | `message` | String | merge, else `""` |
| `terminalTitle` | `terminalTitle` | String | merge, else `""` |
| `baggageText` | `baggageText` | String | merge, else `""` |
| `carryOnText` | `carryOnText` | String | merge, else `""` |
| `deskTitle` | `deskTitle` | String | merge, else `""` |
| `gateTitle` | `gateTitle` | String | merge, else `""` |
| `carouselTitle` | `carouselTitle` | String | merge, else `""` |
| `dismissalDate` | `dismissalDate` | Instant? | `Instant.parse` (needs `Z`/offset); merge, else null |
| `flightDuration` | `flightDuration` | Int? (minutes) | merge, else null |
| `hasConnections` | `hasConnections` | Boolean | `"TRUE"` case-insensitive; merge, else false |
| `language` | `language` | `EN` / `HE` | unknown → `EN`; merge, else `EN` |

`Stage`: CHECK_IN, GATE_READY, BOARDING_PENDING, BOARDING, LAST_CALL, DEPARTURE_PENDING, DEPARTED, LANDING_SOON,
LANDED, BAGGAGE, COMPLETED, CONNECTION_ERROR, GENERAL_ERROR.

## Sent by the server but not parsed today

- `gateOpenTime` (GATE_READY, BOARDING, DEPARTED in the Postman collection).
- `LiveActivityStaleModeSecondaryMessageGeneral`.

## Notes for the UI work

- **`"--"` means missing.** `gate`, `terminal`, `arrivalTerminal`, `checkInCounters`, `boardingZone`,
  `baggageCarousel`, `flightNumber` and `flightStatus` use `"--"` as their empty value. Pill and text code must treat
  `"--"` like null, or the pill shows `Gate --` / `Belt --`.
- Times are airport-local wall-clock values: display them as `HH:mm` directly (`toFormattedTime()`); add
  `departureTimezoneOffset` only to compare against "now".
- Arrival for the countdown = `departureTime` + `departureTimezoneOffset` + `flightDuration` minutes (current SDK logic).
- No version, delay or gate-close fields exist; delay is read from `flightStatus` only.

## Exception: schedule-change fields (task 3b, 2026-11-16) — demo-only until Phase B

`FlightContentState` has four new **optional** fields, all `Instant?` (default `null`, `InstantSerializer`, same naive
airport-local convention as `departureTime` / `arrivalTime`):

| Field | Meaning |
|---|---|
| `scheduledDepartureTime` | Departure as originally scheduled |
| `estimatedDepartureTime` | Departure now expected (later than scheduled → delayed) |
| `scheduledArrivalTime` | Arrival as originally scheduled |
| `estimatedArrivalTime` | Arrival now expected |

- **Only the demo simulator fills them** ("Simulate delay": estimated = scheduled + 55 min). The FCM parser
  (`FlightMessagingService.toFlightContentState`) is **not** changed and never sets them, so a real push renders
  exactly as before.
- Old persisted JSON still loads (`ignoreUnknownKeys`, defaults null).
- Phase B: agree the payload keys with the backend (server item S4: absolute times with offsets) and parse them.
- The previous gate for "Gate ~~D2~~ D3" is not a payload field: the SDK records it on a gate change
  (`JourneyPrefs`, per `activityId`).
