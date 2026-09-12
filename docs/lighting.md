# Lighting demo

Run `./gradlew :platforms:desktop:run`. The house and terrain cast sun shadows.
Two street lamps illuminate the path; two emissive window materials and exterior
spot lights demonstrate house lighting. The clock runs for 90 seconds per in-game day, but
lighting itself uses four fixed situations. The clock-selected situation is applied when a map
is entered, so crossing a threshold does not change the current map in the middle of a frame.

- `K`: cycle the four situations immediately (preview/debug control).
- `N`: advance one hour.
- `Space`: pause or resume the clock.
- `L`: disable or re-enable automatic window and street lighting.
- `F3`: inspect time and whether lamps are currently active.

The schedule is early morning from 05:00, day from 08:00, early evening from 17:00,
and night from 20:00 until 05:00. Local lights and window emission are off during the day
and use a fixed intensity in the other three situations, unless disabled with `L`. Sun shadows disappear at night. Local lights do not yet cast
their own shadows; see the engine's `docs/lighting.md` for the current render contract.

`./gradlew :lightingSmokeTest --offline` checks measured lighting and saves
`trackside-day.png` / `trackside-night.png` in the Java temporary directory.
