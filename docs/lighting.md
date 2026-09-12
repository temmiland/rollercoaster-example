# Lighting demo

Run `./gradlew :platforms:desktop:run`. The house and terrain cast sun shadows.
Two street lamps illuminate the path; two emissive window materials and exterior
spot lights demonstrate house lighting. The light cycle lasts 90 seconds.

- `N`: advance one hour.
- `Space`: pause or resume the clock.
- `L`: disable or re-enable automatic window and street lighting.
- `F3`: inspect time and whether lamps are currently active.

During the day all local lights and window emission are off. At dusk they fade in,
unless disabled with L. Sun shadows disappear at night. Local lights do not yet cast
their own shadows; see the engine's `docs/lighting.md` for the current render contract.

`./gradlew :lightingSmokeTest --offline` checks measured lighting and saves
`trackside-day.png` / `trackside-night.png` in the Java temporary directory.
