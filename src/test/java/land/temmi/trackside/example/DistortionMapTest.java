package land.temmi.trackside.example;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import land.temmi.rollercoaster.asset.ModelDefinition;
import land.temmi.rollercoaster.asset.ModelManifest;
import land.temmi.rollercoaster.input.MoveIntent;
import land.temmi.rollercoaster.world.GravityState;
import land.temmi.rollercoaster.world.MapProp;
import land.temmi.rollercoaster.world.MapPlane;
import land.temmi.rollercoaster.world.FoldedMap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class DistortionMapTest {
    private static final MoveIntent[] INPUTS =
        {MoveIntent.UP, MoveIntent.DOWN, MoveIntent.LEFT, MoveIntent.RIGHT};

    @BeforeClass public static void openFiles() {
        if (Gdx.files == null) Gdx.files = new Lwjgl3Files();
    }

    @Test public void inputMapsToTheAuthoredWorldAxes() {
        assertStep(GravityState.FLOOR, MoveIntent.UP, 0, 0, -1);
        assertStep(GravityState.FLOOR, MoveIntent.DOWN, 0, 0, 1);
        assertStep(GravityState.FLOOR, MoveIntent.LEFT, -1, 0, 0);
        assertStep(GravityState.FLOOR, MoveIntent.RIGHT, 1, 0, 0);

        assertStep(GravityState.WEST_WALL, MoveIntent.UP, 0, 1, 0);
        assertStep(GravityState.WEST_WALL, MoveIntent.DOWN, 0, -1, 0);
        assertStep(GravityState.WEST_WALL, MoveIntent.LEFT, 0, 0, 1);
        assertStep(GravityState.WEST_WALL, MoveIntent.RIGHT, 0, 0, -1);

        assertStep(GravityState.EAST_WALL, MoveIntent.UP, 0, 1, 0);
        assertStep(GravityState.EAST_WALL, MoveIntent.DOWN, 0, -1, 0);
        assertStep(GravityState.EAST_WALL, MoveIntent.LEFT, 0, 0, -1);
        assertStep(GravityState.EAST_WALL, MoveIntent.RIGHT, 0, 0, 1);

        assertStep(GravityState.CEILING, MoveIntent.UP, 0, 0, 1);
        assertStep(GravityState.CEILING, MoveIntent.DOWN, 0, 0, -1);
        assertStep(GravityState.CEILING, MoveIntent.LEFT, -1, 0, 0);
        assertStep(GravityState.CEILING, MoveIntent.RIGHT, 1, 0, 0);
    }

    /** Walls only ever change altitude; ground and ceiling never do. */
    @Test public void onlyWallsWalkAlongTheVerticalAxis() {
        Vector3 step = new Vector3();
        for (MoveIntent input : INPUTS) {
            assertEquals(0f, GravityState.FLOOR.step(input, step).y, 0f);
            assertEquals(0f, GravityState.CEILING.step(input, step).y, 0f);
            assertEquals(0f, GravityState.WEST_WALL.step(input, step).x, 0f);
            assertEquals(0f, GravityState.EAST_WALL.step(input, step).x, 0f);
        }
    }

    /** Holding one direction per plane has to carry the player the whole way round and back. */
    @Test public void theLoopCanBeWalkedAllTheWayAround() {
        DistortionCave cave = new DistortionCave();
        Vector3 at = cave.spawnTile(new Vector3());
        assertEquals(GravityState.FLOOR, gravity(cave.map, at));

        at = walkUntilThePlaneChanges(cave.map, at, MoveIntent.LEFT);
        assertEquals(GravityState.WEST_WALL, gravity(cave.map, at));
        at = walkUntilThePlaneChanges(cave.map, at, MoveIntent.UP);
        assertEquals(GravityState.CEILING, gravity(cave.map, at));
        at = walkUntilThePlaneChanges(cave.map, at, MoveIntent.RIGHT);
        assertEquals(GravityState.EAST_WALL, gravity(cave.map, at));
        at = walkUntilThePlaneChanges(cave.map, at, MoveIntent.DOWN);
        assertEquals(GravityState.FLOOR, gravity(cave.map, at));

        // The loop keeps to the row it started on, so the way back is the row it came from.
        assertEquals(cave.spawnTile(new Vector3()).z, at.z, 0f);
    }

    /** A crossing that cannot be walked back would strand the player on a wall. */
    @Test public void everyCrossingCanBeWalkedBack() {
        DistortionCave cave = new DistortionCave();
        Vector3 target = new Vector3();
        Vector3 back = new Vector3();
        int crossings = 0;
        for (Vector3 from : allTiles(cave.map)) {
            MapPlane origin = cave.map.planeAt((int) from.x, (int) from.y, (int) from.z);
            for (MoveIntent input : INPUTS) {
                if (!cave.map.tryStep((int) from.x, (int) from.y, (int) from.z, input, target)) continue;
                if (cave.map.planeAt((int) target.x, (int) target.y, (int) target.z) == origin) continue;
                crossings++;
                boolean returns = false;
                for (MoveIntent reverse : INPUTS) {
                    if (cave.map.tryStep((int) target.x, (int) target.y, (int) target.z, reverse, back)
                        && back.equals(from)) {
                        returns = true;
                        break;
                    }
                }
                assertTrue("no way back from " + target + " to " + from, returns);
            }
        }
        assertTrue("the map has no crossings at all", crossings > 0);
    }

    @Test public void stepsNeverLeaveTheMap() {
        DistortionCave cave = new DistortionCave();
        Vector3 target = new Vector3();
        for (Vector3 from : allTiles(cave.map)) {
            for (MoveIntent input : INPUTS) {
                if (!cave.map.tryStep((int) from.x, (int) from.y, (int) from.z, input, target)) continue;
                assertNotNull("stepped into nothing at " + target,
                    cave.map.planeAt((int) target.x, (int) target.y, (int) target.z));
            }
        }
    }

    /** A step that changes plane lifts clear of both planes instead of cutting the corner. */
    @Test public void crossingsArcAwayFromBothPlanes() {
        DistortionCave cave = new DistortionCave();
        Vector3 arc = new Vector3();
        Vector3 from = cave.spawnTile(new Vector3());
        Vector3 to = walkUntilThePlaneChanges(cave.map, from, MoveIntent.LEFT);
        Vector3 before = lastTileBefore(cave.map, cave.spawnTile(new Vector3()), MoveIntent.LEFT);

        cave.map.stepArc((int) before.x, (int) before.y, (int) before.z,
            (int) to.x, (int) to.y, (int) to.z, 0.5f, arc);
        assertTrue("clears the ground", arc.dot(GravityState.FLOOR.normal(new Vector3())) > 0f);
        assertTrue("clears the wall", arc.dot(GravityState.WEST_WALL.normal(new Vector3())) > 0f);

        cave.map.stepArc((int) before.x, (int) before.y, (int) before.z,
            (int) to.x, (int) to.y, (int) to.z, 0f, arc);
        assertEquals("starts flush with the plane", 0f, arc.len(), 1e-5f);
    }

    /** Props are obstacles, but they must not wall off the crossings or the way in and out. */
    @Test public void propsKeepTheCrossingsWalkable() {
        DistortionCave cave = new DistortionCave();
        Set<String> blocked = blockedTiles(cave.map);

        assertTrue("a prop sits on the spawn", !blocked.contains(key(cave.spawnTile(new Vector3()))));
        assertTrue("a prop sits on the exit", !blocked.contains(key(cave.exitTile(new Vector3()))));

        Vector3 target = new Vector3();
        for (Vector3 from : allTiles(cave.map)) {
            MapPlane origin = cave.map.planeAt((int) from.x, (int) from.y, (int) from.z);
            for (MoveIntent input : INPUTS) {
                if (!cave.map.tryStep((int) from.x, (int) from.y, (int) from.z, input, target)) continue;
                if (cave.map.planeAt((int) target.x, (int) target.y, (int) target.z) == origin) continue;
                assertTrue("a prop blocks the crossing at " + from, !blocked.contains(key(from)));
                assertTrue("a prop blocks the crossing at " + target, !blocked.contains(key(target)));
            }
        }
    }

    /** The loop the player actually walks has to stay clear of props. */
    @Test public void propsLeaveTheStartingLoopOpen() {
        DistortionCave cave = new DistortionCave();
        Set<String> blocked = blockedTiles(cave.map);
        Vector3 at = cave.spawnTile(new Vector3());
        MoveIntent[] route = {MoveIntent.LEFT, MoveIntent.UP, MoveIntent.RIGHT, MoveIntent.DOWN};
        Vector3 target = new Vector3();
        for (MoveIntent input : route) {
            MapPlane plane = cave.map.planeAt((int) at.x, (int) at.y, (int) at.z);
            while (cave.map.tryStep((int) at.x, (int) at.y, (int) at.z, input, target)) {
                at.set(target);
                assertTrue("a prop blocks the loop at " + at, !blocked.contains(key(at)));
                if (cave.map.planeAt((int) at.x, (int) at.y, (int) at.z) != plane) break;
            }
        }
        assertEquals(GravityState.FLOOR, gravity(cave.map, at));
    }

    /** World tiles covered by a prop's footprint, which the scene turns into collision. */
    private static Set<String> blockedTiles(FoldedMap map) {
        Array<ModelDefinition> models = ModelManifest.load(Gdx.files.classpath("maps/models.json"));
        Set<String> blocked = new HashSet<>();
        Vector3 tile = new Vector3();
        for (MapPlane plane : map.planes()) {
            for (MapProp prop : plane.map.props) {
                ModelDefinition definition = definition(models, prop.model);
                for (int x = definition.collisionMinX; x <= definition.collisionMaxX; x++) {
                    for (int z = definition.collisionMinZ; z <= definition.collisionMaxZ; z++) {
                        plane.tile((int) prop.x + x, (int) prop.z + z, tile);
                        blocked.add(key(tile));
                    }
                }
            }
        }
        return blocked;
    }

    private static ModelDefinition definition(Array<ModelDefinition> models, String id) {
        for (ModelDefinition definition : models) if (definition.id.equals(id)) return definition;
        throw new IllegalArgumentException("Unknown model in the cave map: " + id);
    }

    private static Vector3 walkUntilThePlaneChanges(FoldedMap map, Vector3 from, MoveIntent input) {
        MapPlane plane = map.planeAt((int) from.x, (int) from.y, (int) from.z);
        Vector3 at = new Vector3(from);
        Vector3 target = new Vector3();
        for (int step = 0; step < 64; step++) {
            assertTrue("blocked walking " + input + " from " + at,
                map.tryStep((int) at.x, (int) at.y, (int) at.z, input, target));
            at.set(target);
            if (map.planeAt((int) at.x, (int) at.y, (int) at.z) != plane) return at;
        }
        throw new AssertionError("never left the plane walking " + input);
    }

    private static Vector3 lastTileBefore(FoldedMap map, Vector3 from, MoveIntent input) {
        MapPlane plane = map.planeAt((int) from.x, (int) from.y, (int) from.z);
        Vector3 at = new Vector3(from);
        Vector3 target = new Vector3();
        while (map.tryStep((int) at.x, (int) at.y, (int) at.z, input, target)
            && map.planeAt((int) target.x, (int) target.y, (int) target.z) == plane) {
            at.set(target);
        }
        return at;
    }

    private static Iterable<Vector3> allTiles(FoldedMap map) {
        java.util.List<Vector3> tiles = new java.util.ArrayList<>();
        Vector3 tile = new Vector3();
        for (MapPlane plane : map.planes()) {
            for (int u = 0; u < plane.map.tiles.getWidth(); u++) {
                for (int v = 0; v < plane.map.tiles.getDepth(); v++) {
                    tiles.add(new Vector3(plane.tile(u, v, tile)));
                }
            }
        }
        return tiles;
    }

    private static GravityState gravity(FoldedMap map, Vector3 tile) {
        MapPlane plane = map.planeAt((int) tile.x, (int) tile.y, (int) tile.z);
        assertNotNull("no plane at " + tile, plane);
        return plane.gravity;
    }

    private static String key(Vector3 tile) {
        return (int) tile.x + "," + (int) tile.y + "," + (int) tile.z;
    }

    private static void assertStep(GravityState state, MoveIntent input, int x, int y, int z) {
        assertEquals(state + " " + input, new Vector3(x, y, z), state.step(input, new Vector3()));
    }
}
