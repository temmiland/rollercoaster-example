# Example Game

Small standalone example that exercises the Rollercoaster rendering and world APIs with a
procedural map and a GLTF house.

The build uses the sibling Rollercoaster repository through `includeBuild('../rollercoaster')`.

```sh
./gradlew build
./gradlew :renderSmokeTest
./gradlew :platforms:desktop:run
./gradlew :platforms:web:run
```

Die Plattform-Launcher liegen unter `platforms/desktop`, `platforms/android`, `platforms/ios` und
`platforms/web`. Die Web-Plattform übersetzt das Spiel mit gdx-teavm nach JavaScript und rendert
über WebGL 2; der Run-Task startet einen lokalen Server auf Port 8080.

Die Laufzeit lädt die Welt über `WorldSceneLoader` und kombiniert Tastatursteuerung mit dem
virtuellen D-Pad der `TouchInput`-Zone unten links. Das Gamepad-Adapter-API der Engine kann von
plattform-spezifischen Launchern über `CombinedInput` ergänzt werden.

Im Desktoplauf zeigt `example-game` vier feste Lichtsituationen. `K` schaltet sie sofort durch.
Die Uhr läuft im Hintergrund weiter; beim nächsten Kartenbetreten wird die Situation anhand der
Uhrzeit übernommen. `N` springt eine Stunde weiter, `Space` pausiert die Uhr und `L`
aktiviert/deaktiviert die automatische Haus- und Straßenbeleuchtung. Details und Bildtests stehen
in [docs/lighting.md](docs/lighting.md).
