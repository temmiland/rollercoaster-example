# Sprite-Assets

Das Example Game lädt Figuren aus `src/main/resources/sprites/player.json`. Das Manifest ist
versioniert und verweist auf einen TexturePacker-Atlas. Jede Figur definiert eine Welt-Höhe, die
Frame-Dauer sowie `idle` und `walk` je `north`, `east`, `south` und `west`.
Der Beispielspieler hat die Welt-Höhe `1` und entspricht damit genau einer Terrain-Höhenebene.
Die Kamera ist auf eine Terrainstufe kalibriert, nicht auf die Höhe der aktuell gefolgten Figur;
größere oder kleinere NPC-Sprites verändern daher weder Zoom noch sichtbaren Kartenausschnitt.

```json
{
  "version": 1,
  "atlas": "sprites/player.atlas",
  "sprites": [{
    "id": "player",
    "height": 1,
    "frameDuration": 0.14,
    "directions": {
      "south": { "idle": "player_south_idle", "walk": ["player_south_walk_a", "player_south_walk_b"] }
    }
  }]
}
```

Das vollständige Manifest enthält alle vier Richtungen. `DirectionalSpriteAnimation` löst die
benannten Regionen auf und wechselt anhand von `GridActor.getFacing()` zwischen Idle und Lauf-
Frames. Der Fußpunkt bleibt dabei in der Tilemitte, weil nur die Billboard-Region wechselt.

Der Atlas hat unter den Füßen zwei transparente Pixelzeilen; das Billboard verwendet deshalb
`setBottomPadding(2f / 24f)`. Seine Bildfläche zeigt zur Kamera, während die Tiefenprüfung eine
aufrechte Fläche durch den Fußpunkt verwendet. So schneidet die Figur weder Hauswände noch
aufsteigende Rampen. In der Höhle richtet sie sich nach der Normalen der begehbaren Fläche.
`./gradlew renderSmokeTest` prüft die sichtbaren Sprite-Pixel auch während der Bewegung und in
beiden Zeichenreihenfolgen; die Kontrollbilder liegen unter `build/occlusion/`.

Für echte Spiel-Assets ersetzt der Editor später PNG und Atlasdatei; die Runtime benötigt keine
Pixel- oder Packing-Logik.
