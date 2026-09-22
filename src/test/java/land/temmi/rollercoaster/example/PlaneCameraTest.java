package land.temmi.rollercoaster.example;

import com.badlogic.gdx.math.Vector3;
import land.temmi.rollercoaster.render.PlaneCamera;
import land.temmi.rollercoaster.world.GravityState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PlaneCameraTest {
    private static final float PITCH = 45f;

    @Test public void groundIsFramedLikeTheOrdinaryFieldCamera() {
        PlaneCamera camera = snapped(GravityState.FLOOR);
        Vector3 direction = camera.direction(new Vector3());
        assertEquals(0f, direction.x, 1e-4f);
        assertEquals(-0.7071f, direction.y, 1e-3f);
        assertEquals(-0.7071f, direction.z, 1e-3f);
    }

    /** The world stays upright on every plane; only the sprite turns. */
    @Test public void theCameraNeverRolls() {
        for (GravityState state : GravityState.values()) {
            assertTrue(state + " rolled the world", snapped(state).up(new Vector3()).y > 0.5f);
        }
    }

    /**
     * The one property that keeps the controls readable: whatever plane the player walks on, the
     * right-hand input has to move them towards the right of the screen.
     */
    @Test public void screenRightFollowsEachPlanesRightInput() {
        Vector3 direction = new Vector3();
        Vector3 up = new Vector3();
        Vector3 axis = new Vector3();
        for (GravityState state : GravityState.values()) {
            PlaneCamera camera = snapped(state);
            camera.direction(direction);
            camera.up(up);
            Vector3 screenRight = direction.cpy().crs(up).nor();
            assertTrue(state + " sends the right-hand input the wrong way",
                screenRight.dot(state.right(axis)) > 0.5f);
            assertTrue(state + " sends the forward input the wrong way",
                up.dot(state.forward(axis)) > 0.5f);
        }
    }

    /** A ceiling is watched from below, a ground from above. */
    @Test public void everyPlaneIsWatchedFromItsFreeSide() {
        Vector3 direction = new Vector3();
        Vector3 normal = new Vector3();
        for (GravityState state : GravityState.values()) {
            snapped(state).direction(direction);
            state.normal(normal);
            // The camera sits opposite its view direction, so it has to lie on the normal's side.
            assertTrue(state + " looks from inside the plane", direction.dot(normal) < 0f);
        }
    }

    @Test public void theSpriteHangsUpsideDownUnderACeiling() {
        Vector3 right = new Vector3();
        Vector3 up = new Vector3();
        Vector3 cameraUp = new Vector3();

        PlaneCamera ground = snapped(GravityState.FLOOR);
        ground.spriteBasis(right, up);
        assertEquals(1f, up.dot(ground.up(cameraUp)), 1e-3f);

        PlaneCamera ceiling = snapped(GravityState.CEILING);
        ceiling.spriteBasis(right, up);
        assertEquals(-1f, up.dot(ceiling.up(cameraUp)), 1e-3f);
    }

    /** Wall gravity uses a clean quarter turn, even though the camera looks at it obliquely. */
    @Test public void theSpriteUsesQuarterTurnsOnWalls() {
        Vector3 right = new Vector3();
        Vector3 up = new Vector3();
        Vector3 cameraUp = new Vector3();
        Vector3 cameraRight = new Vector3();
        for (GravityState state : new GravityState[] {GravityState.WEST_WALL, GravityState.EAST_WALL}) {
            PlaneCamera camera = snapped(state);
            camera.spriteBasis(state.normal(new Vector3()), right, up);
            camera.up(cameraUp);
            cameraRight.set(camera.direction(new Vector3())).crs(cameraUp).nor();
            assertEquals(state + " is not a quarter turn", 0f, up.dot(cameraUp), 1e-3f);
            assertEquals(state + " is not a quarter turn", 1f, Math.abs(up.dot(cameraRight)), 1e-3f);
        }
    }

    @Test public void sideWallsKeepTheOrdinaryDownwardPitch() {
        for (GravityState state : new GravityState[] {GravityState.WEST_WALL, GravityState.EAST_WALL}) {
            assertEquals(state + " lowered the camera to wall level", -0.7071f,
                snapped(state).direction(new Vector3()).y, 1e-3f);
        }
    }

    @Test public void blendingRunsForItsDurationAndLandsOnTheTarget() {
        PlaneCamera camera = snapped(GravityState.FLOOR);
        camera.setBlendSeconds(0.2f);
        camera.blendTo(GravityState.CEILING.normal(new Vector3()), GravityState.CEILING.right(new Vector3()));
        assertTrue(camera.isBlending());

        camera.update(0.1f);
        assertTrue(camera.isBlending());
        Vector3 midway = new Vector3();
        Vector3 unused = new Vector3();
        camera.spriteBasis(unused, midway);
        assertTrue("the sprite has to turn over gradually", Math.abs(midway.y) < 0.999f);

        camera.update(0.2f);
        assertFalse(camera.isBlending());
        assertEquals(1f, camera.direction(new Vector3())
            .dot(snapped(GravityState.CEILING).direction(new Vector3())), 1e-3f);
    }

    /** Looking over a rim must not turn the sprite before it leaves its current plane. */
    @Test public void previewingANewPlaneKeepsTheSpriteOnItsCurrentPlane() {
        PlaneCamera camera = snapped(GravityState.FLOOR);
        camera.setBlendSeconds(0.2f);
        camera.blendTo(GravityState.WEST_WALL.normal(new Vector3()), GravityState.WEST_WALL.right(new Vector3()));
        camera.update(0.1f);

        Vector3 right = new Vector3();
        Vector3 up = new Vector3();
        camera.spriteBasis(GravityState.FLOOR.normal(new Vector3()), right, up);
        Vector3 floorSpriteUp = new Vector3(up);

        camera.spriteBasis(GravityState.WEST_WALL.normal(new Vector3()), right, up);
        assertTrue("the floor sprite followed the upcoming wall during the preview",
            up.dot(floorSpriteUp) < 0.999f);
    }

    @Test public void steppingWithinAPlaneDoesNotRestartABlend() {
        PlaneCamera camera = snapped(GravityState.FLOOR);
        camera.blendTo(GravityState.FLOOR.normal(new Vector3()), GravityState.FLOOR.right(new Vector3()));
        assertFalse(camera.isBlending());
    }

    @Test public void repeatedRimPreviewDoesNotRestartTheCameraBlend() {
        PlaneCamera camera = snapped(GravityState.FLOOR);
        camera.setBlendSeconds(0.2f);
        Vector3 normal = GravityState.WEST_WALL.normal(new Vector3());
        Vector3 right = GravityState.WEST_WALL.right(new Vector3());
        camera.blendTo(normal, right);
        camera.update(0.1f);
        camera.blendTo(normal, right);
        camera.update(0.1f);
        assertFalse("the preview restarted instead of completing", camera.isBlending());
    }

    private static PlaneCamera snapped(GravityState state) {
        PlaneCamera camera = new PlaneCamera().setPitch(PITCH);
        camera.snapTo(state.normal(new Vector3()), state.right(new Vector3()));
        return camera;
    }
}
