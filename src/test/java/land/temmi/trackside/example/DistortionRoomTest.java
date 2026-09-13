package land.temmi.trackside.example;

import com.badlogic.gdx.math.Vector3;
import land.temmi.rollercoaster.input.MoveIntent;
import land.temmi.rollercoaster.world.GravityState;
import land.temmi.rollercoaster.world.SurfacePlatform;
import land.temmi.rollercoaster.world.SurfaceRoom;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class DistortionRoomTest {

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
        for (MoveIntent input : new MoveIntent[] {MoveIntent.UP, MoveIntent.DOWN, MoveIntent.LEFT, MoveIntent.RIGHT}) {
            assertEquals(0f, GravityState.FLOOR.step(input, step).y, 0f);
            assertEquals(0f, GravityState.CEILING.step(input, step).y, 0f);
            assertEquals(0f, GravityState.WEST_WALL.step(input, step).x, 0f);
            assertEquals(0f, GravityState.EAST_WALL.step(input, step).x, 0f);
        }
    }

    @Test public void theLoopCanBeWalkedAllTheWayAround() {
        DistortionMap map = new DistortionMap();
        int z = DistortionMap.DEPTH / 2;
        int wallSteps = DistortionMap.WALL_TOP - DistortionMap.WALL_BOTTOM;

        Vector3 at = new Vector3(0, 0, z);
        assertGravity(map, at, GravityState.FLOOR);

        at = step(map.room, at, MoveIntent.LEFT);
        assertEquals(new Vector3(DistortionMap.WEST_X, DistortionMap.WALL_BOTTOM, z), at);
        assertGravity(map, at, GravityState.WEST_WALL);

        for (int i = 0; i < wallSteps; i++) at = step(map.room, at, MoveIntent.UP);
        at = step(map.room, at, MoveIntent.UP);
        assertEquals(new Vector3(0, DistortionMap.CEILING_Y, z), at);
        assertGravity(map, at, GravityState.CEILING);

        for (int i = 0; i < DistortionMap.WIDTH - 1; i++) at = step(map.room, at, MoveIntent.RIGHT);
        at = step(map.room, at, MoveIntent.RIGHT);
        assertEquals(new Vector3(DistortionMap.EAST_X, DistortionMap.WALL_TOP, z), at);
        assertGravity(map, at, GravityState.EAST_WALL);

        for (int i = 0; i < wallSteps; i++) at = step(map.room, at, MoveIntent.DOWN);
        at = step(map.room, at, MoveIntent.DOWN);
        assertEquals(new Vector3(DistortionMap.WIDTH - 1, 0, z), at);
        assertGravity(map, at, GravityState.FLOOR);

        for (int i = 0; i < DistortionMap.WIDTH - 1; i++) at = step(map.room, at, MoveIntent.LEFT);
        assertEquals(new Vector3(0, 0, z), at);
    }

    /** Every seam has to be reversible, or a player can strand themselves on a wall. */
    @Test public void everySeamCanBeWalkedBack() {
        DistortionMap map = new DistortionMap();
        MoveIntent[] around = {MoveIntent.LEFT, MoveIntent.UP, MoveIntent.RIGHT, MoveIntent.DOWN};
        MoveIntent[] back = {MoveIntent.DOWN, MoveIntent.LEFT, MoveIntent.UP, MoveIntent.RIGHT};
        Vector3[] corners = {
            new Vector3(0, 0, 0),
            new Vector3(DistortionMap.WEST_X, DistortionMap.WALL_TOP, 0),
            new Vector3(DistortionMap.WIDTH - 1, DistortionMap.CEILING_Y, 0),
            new Vector3(DistortionMap.EAST_X, DistortionMap.WALL_BOTTOM, 0),
        };
        for (int z = 0; z < DistortionMap.DEPTH; z++) {
            for (int corner = 0; corner < corners.length; corner++) {
                Vector3 from = new Vector3(corners[corner]).add(0f, 0f, z);
                Vector3 crossed = step(map.room, from, around[corner]);
                assertEquals(from, step(map.room, crossed, back[corner]));
            }
        }
    }

    @Test public void stepsOffTheEdgeOfTheRoomAreBlocked() {
        DistortionMap map = new DistortionMap();
        Vector3 target = new Vector3();
        assertFalse(map.room.tryStep(0, 0, 0, MoveIntent.UP, target));
        assertFalse(map.room.tryStep(DistortionMap.WEST_X, DistortionMap.WALL_TOP, 0,
            MoveIntent.RIGHT, target));
    }

    /** A step that changes plane lifts clear of both planes instead of cutting the corner. */
    @Test public void seamStepsArcAwayFromBothPlanes() {
        DistortionMap map = new DistortionMap();
        Vector3 arc = new Vector3();
        map.room.stepArc(0, 0, 0, DistortionMap.WEST_X, DistortionMap.WALL_BOTTOM, 0, 0.5f, arc);
        assertTrue("clears the ground", arc.dot(GravityState.FLOOR.normal(new Vector3())) > 0f);
        assertTrue("clears the wall", arc.dot(GravityState.WEST_WALL.normal(new Vector3())) > 0f);

        map.room.stepArc(0, 0, 0, DistortionMap.WEST_X, DistortionMap.WALL_BOTTOM, 0, 0f, arc);
        assertEquals("starts flush with the plane", 0f, arc.len(), 1e-5f);

        map.room.stepArc(0, 0, 0, 1, 0, 0, 0.5f, arc);
        assertEquals("a step within one plane stays flat", 0f, arc.len(), 1e-5f);
    }

    private static void assertGravity(DistortionMap map, Vector3 tile, GravityState expected) {
        SurfacePlatform platform = map.room.platformAt((int) tile.x, (int) tile.y, (int) tile.z);
        assertNotNull("no platform at " + tile, platform);
        assertEquals(expected, platform.gravity);
    }

    private static Vector3 step(SurfaceRoom room, Vector3 from, MoveIntent input) {
        Vector3 target = new Vector3();
        assertTrue("blocked " + input + " from " + from,
            room.tryStep((int) from.x, (int) from.y, (int) from.z, input, target));
        return target;
    }

    private static void assertStep(GravityState state, MoveIntent input, int x, int y, int z) {
        assertEquals(state + " " + input, new Vector3(x, y, z), state.step(input, new Vector3()));
    }
}
