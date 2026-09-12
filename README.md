# Example Game

Small standalone example that exercises the Rollercoaster rendering and world APIs with a
procedural map and a GLTF house.

The build uses the sibling Rollercoaster repository through `includeBuild('../rollercoaster')`.

```sh
./gradlew build
./gradlew :renderSmokeTest
./gradlew :platforms:desktop:run
```

Die Plattform-Launcher liegen unter `platforms/desktop`, `platforms/android` und `platforms/ios`.
