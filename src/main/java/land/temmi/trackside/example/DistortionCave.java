package land.temmi.trackside.example;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import land.temmi.rollercoaster.world.FoldedMap;
import land.temmi.rollercoaster.world.FoldedMapLoader;
import land.temmi.rollercoaster.world.TileSurface;
import land.temmi.rollercoaster.world.Tileset;

/** The cave map: four slabs folded into a loop, authored in {@code maps/distortion.json}. */
final class DistortionCave {
    static final String PATH = "maps/distortion.json";

    private static final Color RIM = Color.valueOf("4f7bff");
    private static final Color ROCK = Color.valueOf("c98a86");
    private static final Color FLANK = Color.valueOf("221d33");

    final FoldedMap map;
    private final Vector3 spawn = new Vector3();
    private final Vector3 exit = new Vector3();

    DistortionCave() {
        map = new FoldedMapLoader().load(Gdx.files.classpath(PATH), createTileset());
        require(map.entityTile("player", spawn), "player");
        require(map.entityTile("exit", exit), "exit");
    }

    /** Where the cave drops the player in. */
    Vector3 spawnTile(Vector3 out) { return out.set(spawn); }

    /** Stepping onto this tile leaves the cave again. */
    Vector3 exitTile(Vector3 out) { return out.set(exit); }

    static Tileset createTileset() {
        return new Tileset()
            .add(new TileSurface("rock").setColor(ROCK, FLANK))
            .add(new TileSurface("rim").setColor(RIM, FLANK));
    }

    private static void require(Vector3 tile, String type) {
        if (tile == null) throw new IllegalStateException("The cave map has no " + type + " entity");
    }
}
