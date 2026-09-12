# Lighting demo

Run `./gradlew :platforms:desktop:run`. The house and terrain cast sun shadows.
Two `streetLamp` map props with warm amber bulbs illuminate the path; two matching emissive window
materials and exterior spot lights demonstrate house lighting. The clock runs for 90 seconds per in-game day, but
lighting itself uses four fixed situations. The clock-selected situation is applied when a map
is entered, so crossing a threshold does not change the current map in the middle of a frame.

- `K`: cycle the four situations immediately (preview/debug control).
- `N`: advance one hour.
- `Space`: pause or resume the clock.
- `L`: disable or re-enable automatic window and street lighting.
- `F3`: inspect time and whether lamps are currently active.

The schedule is early morning from 05:00, day from 08:00, early evening from 17:00,
and night from 20:00 until 05:00. Early morning is red, day is close to neutral, and early
evening mixes sunset red with blue ambient light. Local lights and window emission are off during
early morning and day, then use a reduced fixed intensity in the evening and at night, unless disabled with `L`.
Window geometry keeps its blue daytime colour and receives the same amber tint as its glow when
the local lights are active. The visible emission is boosted separately from the point-light
intensity, so the glow is stronger without increasing its illuminated radius.
Night uses brighter cool ambient light and weak moonlight, so the scene remains visible. Local lights do not yet cast
their own shadows; see the engine's `docs/lighting.md` for the current render contract.

`./gradlew :lightingSmokeTest --offline` checks measured lighting and saves
`trackside-day.png` / `trackside-night.png` in the Java temporary directory.
