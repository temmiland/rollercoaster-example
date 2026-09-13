package land.temmi.trackside.example;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import land.temmi.rollercoaster.input.MoveIntent;
import land.temmi.rollercoaster.world.GravityState;
import land.temmi.rollercoaster.world.LoadedMap;
import land.temmi.rollercoaster.world.MapEntity;
import land.temmi.rollercoaster.world.MapProp;
import land.temmi.rollercoaster.world.SurfacePlatform;
import land.temmi.rollercoaster.world.SurfaceRoom;
import land.temmi.rollercoaster.world.TileMap;
import land.temmi.rollercoaster.world.TileSurface;

/**
 * Four slabs floating in the void, folded into a loop: ground, west wall, ceiling, east wall.
 *
 * <p>The slabs do not touch. Walking off the end of one leaps the gap onto the next, which is the
 * only place gravity changes. Everything else - stepping, collision, terrain - is the library's
 * ordinary tile handling on a plane that happens not to be the ground.
 */
final class DistortionMap {
    static final int WIDTH = 9;
    static final int DEPTH = 7;
    /** Lowest and highest walkable row of either wall. */
    static final int WALL_BOTTOM = 1;
    static final int WALL_TOP = 7;
    static final int CEILING_Y = 9;
    static final int WEST_X = -2;
    static final int EAST_X = WIDTH + 1;
    static final int ENTRANCE_X = WIDTH / 2;

    private static final Color RIM = Color.valueOf("4f7bff");
    private static final Color ROCK = Color.valueOf("c98a86");
    private static final Color ROCK_DARK = Color.valueOf("b0736f");
    private static final Color FLANK = Color.valueOf("221d33");

    final SurfaceRoom room = new SurfaceRoom().setSeamArcHeight(0.9f);
    final SurfacePlatform floor;
    final SurfacePlatform west;
    final SurfacePlatform ceiling;
    final SurfacePlatform east;

    DistortionMap() {
        int wallHeight = WALL_TOP - WALL_BOTTOM + 1;
        floor = platform("floor", GravityState.FLOOR, 0, 0, 0, WIDTH, DEPTH);
        west = platform("west", GravityState.WEST_WALL, WEST_X, WALL_TOP, DEPTH - 1, DEPTH, wallHeight);
        ceiling = platform("ceiling", GravityState.CEILING, 0, CEILING_Y, DEPTH - 1, WIDTH, DEPTH);
        east = platform("east", GravityState.EAST_WALL, EAST_X, WALL_TOP, 0, DEPTH, wallHeight);

        for (int z = 0; z < DEPTH; z++) {
            // Leaving a plane always points along the normal it just gained, which no input maps
            // to, so both directions of every corner are authored.
            room.seam(tile(0, 0, z), MoveIntent.LEFT, tile(WEST_X, WALL_BOTTOM, z), MoveIntent.DOWN);
            room.seam(tile(WEST_X, WALL_TOP, z), MoveIntent.UP, tile(0, CEILING_Y, z), MoveIntent.LEFT);
            room.seam(tile(WIDTH - 1, CEILING_Y, z), MoveIntent.RIGHT, tile(EAST_X, WALL_TOP, z), MoveIntent.UP);
            room.seam(tile(EAST_X, WALL_BOTTOM, z), MoveIntent.DOWN, tile(WIDTH - 1, 0, z), MoveIntent.RIGHT);
        }
    }

    /** Where the cave drops the player in, one step short of the way back out. */
    Vector3 spawnTile() { return tile(ENTRANCE_X, 0, DEPTH - 2); }

    /** Stepping onto this ground tile leaves the cave again. */
    Vector3 exitTile() { return tile(ENTRANCE_X, 0, DEPTH - 1); }

    private static Vector3 tile(int x, int y, int z) { return new Vector3(x, y, z); }

    private SurfacePlatform platform(String id, GravityState gravity, int x, int y, int z,
                                     int width, int depth) {
        TileSurface rim = new TileSurface(id + "Rim").setColor(RIM, FLANK);
        TileSurface rock = new TileSurface(id + "Rock").setColor(ROCK, FLANK);
        TileSurface speck = new TileSurface(id + "Speck").setColor(ROCK_DARK, FLANK);
        TileMap tiles = new TileMap(width, depth);
        for (int u = 0; u < width; u++) {
            for (int v = 0; v < depth; v++) {
                boolean border = u == 0 || v == 0 || u == width - 1 || v == depth - 1;
                tiles.set(u, v, border ? rim : ((u * 7 + v * 3) % 11 == 0 ? speck : rock), 0f);
            }
        }
        SurfacePlatform platform = new SurfacePlatform(id, gravity,
            new LoadedMap(id, tiles, new Array<MapProp>(), new Array<MapEntity>()), x, y, z);
        room.add(platform);
        return platform;
    }
}
