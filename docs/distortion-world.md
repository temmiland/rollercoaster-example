# Zerrwelt

Eine Höhle, in der die Figur aus der normalen Karte ausbricht und an Wänden und Decke weiterläuft.
Der Eingang steht am nördlichen Hauptweg der Testmap auf Tile (12, 2). Von dort nach Norden laufen:
sobald der Schritt in die Höhle abgeschlossen ist, übernimmt `DistortionScreen`. Der Rückweg führt
über das südliche Bodenfeld des Eingangs, Escape bricht jederzeit ab.

## Aufbau

Vier Platten schweben im blauen Nichts und bilden eine Schleife: Boden, Westwand, Decke, Ostwand.
Die Platten berühren sich nicht — zwischen ihnen liegt eine Lücke, über die die Figur springt. Das
ist die einzige Stelle, an der die Schwerkraft wechselt.

Die Map liegt als eigenes Dokument in `maps/distortion.json`: jede Platte ist eine gewöhnliche
Karte mit Tiles, Props und Entities, dazu ihre Schwerkraft und das Raster-Tile, auf dem ihre lokale
(0, 0) sitzt. Start- und Ausgangsfeld sind Entities darin, keine Konstanten im Code. `DistortionCave`
lädt das Dokument und liefert das Tileset; Bewegung, Kollision, Geometrie und Kamera kommen aus der
Bibliothek (siehe `docs/folded-maps.md` in `rollercoaster`).

Felsen und Kristalle sind echte Props aus `models/models.json` und blockieren ihr Feld — auf der
Decke hängen sie nach unten, an den Wänden stehen sie seitlich ab. Auch der Höhleneingang in der
Testmap ist ein Prop (`caveArch` plus zwei `cavePillar`); seine Kollision kommt aus dem
Modell-Manifest, nicht aus handgesetzten Feldern.

## Steuerung

WASD, Pfeiltasten und das mobile Steuerkreuz steuern genau wie in der normalen Karte: die Eingabe
ist bildschirmbezogen, nicht weltbezogen. Rechts bewegt die Figur immer nach rechts über den
Bildschirm — auch an der Decke, wo man sonst spiegelverkehrt laufen würde.

Die Schleife wird an den Ecken durchlaufen: am Westrand des Bodens nach links, dann die Wand mit
hoch, an der Decke nach rechts, die Ostwand mit runter. Zurück geht jede Ecke in der jeweils
umgekehrten Richtung. Ein Schritt, der die Platte verlässt, zeigt immer entlang der Normalen, die
die Figur gerade gewinnt — deshalb ist die Rückrichtung eine andere Taste als die Hinrichtung.

## Darstellung

Die Kamera behält die Weltachse nach oben und rollt nie; Wände und Pfeiler bleiben auf dem
Bildschirm senkrecht. Stattdessen wandert sie auf die freie Seite der aktuellen Fläche — die Decke
wird von unten betrachtet — und der Sprite dreht sich in der Bildebene, sodass er mit den Füßen auf
seiner Fläche steht. Unter der Decke hängt er dadurch kopfüber.

Wechsel blenden über wenige Frames: die Figur läuft den Schritt über die Lücke in einem Bogen,
während Kamera und Sprite-Drehung parallel dazu überblenden. Die Welt selbst bewegt sich nie.

Die Höhle beleuchtet sich flach und ohne Sonne, damit die Schattenkarte der Oberwelt nicht auf die
Platten projiziert wird.

## Prüfung

- `./gradlew test --offline` — Eingabetabellen, die komplette Schleife, Umkehrbarkeit jedes
  Übergangs, dass kein Schritt aus dem Raum führt, Bogen über die Lücke, dass Props weder die
  Übergänge noch die Startrunde zumauern, und für die Kamera: dass sie nie rollt, jede Fläche von
  ihrer freien Seite zeigt, rechts immer rechts bleibt und der Sprite unter der Decke kopfsteht.
  Die Tests lesen dafür die Map und das Modell-Manifest, statt die Maße zu wiederholen.
- `./gradlew worldSmokeTest --offline` — echter Desktop-GL-Durchlauf durch alle vier Flächen.
  Schreibt pro Fläche einen Screenshot nach `build/smoke/`.
- `./gradlew :platforms:desktop:run --offline` — interaktiv starten.

Android und iOS kompilieren, sind zur Laufzeit aber nicht geprüft.
