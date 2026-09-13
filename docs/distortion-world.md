# Zerrwelt

Eine Höhle, in der die Figur aus der normalen Karte ausbricht und an Wänden und Decke weiterläuft.
Der Eingang steht am nördlichen Hauptweg der Testmap auf Tile (12, 2). Von dort nach Norden laufen:
sobald der Schritt in die Höhle abgeschlossen ist, übernimmt `DistortionScreen`. Der Rückweg führt
über das südliche Bodenfeld des Eingangs, Escape bricht jederzeit ab.

## Aufbau

Vier Platten schweben im blauen Nichts und bilden eine Schleife: Boden, Westwand, Decke, Ostwand.
Die Platten berühren sich nicht — zwischen ihnen liegt eine Lücke, über die die Figur springt. Das
ist die einzige Stelle, an der die Schwerkraft wechselt.

`DistortionMap` hält nur den Inhalt: die vier Platten, ihre Farben und die Nahtstellen an den
Ecken. Bewegung, Kollision, Geometrie und Kamera kommen aus der Bibliothek
(siehe `docs/surface-rooms.md` in `rollercoaster`).

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

- `./gradlew test --offline` — Eingabetabellen, die komplette Schleife in beide Richtungen,
  Umkehrbarkeit jeder Naht, blockierte Schritte, Bogen über die Lücke, und für die Kamera: dass sie
  nie rollt, jede Fläche von ihrer freien Seite zeigt, rechts immer rechts bleibt und der Sprite
  unter der Decke kopfsteht.
- `./gradlew worldSmokeTest --offline` — echter Desktop-GL-Durchlauf durch alle vier Flächen.
  Schreibt pro Fläche einen Screenshot nach `build/smoke/`.
- `./gradlew :platforms:desktop:run --offline` — interaktiv starten.

Android und iOS kompilieren, sind zur Laufzeit aber nicht geprüft.
