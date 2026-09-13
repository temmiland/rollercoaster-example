package land.temmi.trackside.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import land.temmi.rollercoaster.actor.GridActor;
import land.temmi.rollercoaster.input.InputSource;
import land.temmi.rollercoaster.input.MoveIntent;
import land.temmi.rollercoaster.world.GravityState;

import java.lang.reflect.Field;

/**
 * Desktop GL run of the cave: walks the whole gravity loop and writes one screenshot per plane.
 *
 * <p>Images land in {@code build/smoke} so a failing frame can be looked at rather than guessed at.
 */
public final class WorldSmoke {
    private static final float FRAME = 1f / 60f;
    private static final int SETTLE_FRAMES = 28;
    private static final MoveIntent[] SCRIPTED = {MoveIntent.NONE};

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(960, 540);
        config.setTitle("Trackside distortion cave verification");
        final Throwable[] failure = {null};
        new Lwjgl3Application(new ApplicationAdapter() {
            private ExampleGame game;

            @Override public void create() {
                try {
                    game = new ExampleGame();
                    game.create();
                    game.resize(960, 540);
                    game.render(FRAME);
                    run(game);
                } catch (Throwable error) {
                    failure[0] = error;
                } finally {
                    Gdx.app.exit();
                }
            }

            @Override public void dispose() { if (game != null) game.dispose(); }
        }, config);
        if (failure[0] != null) throw new RuntimeException(failure[0]);
    }

    private static void run(ExampleGame game) throws Exception {
        set(game, "input", (InputSource) () -> SCRIPTED[0]);
        GridActor player = (GridActor) get(game, "player");
        player.setTile(CaveEntrance.X, CaveEntrance.Z);
        frames(game, 2);
        check(get(game, "distortion") != null, "the cave did not open");
        shot("01-ground");
        checkPlane(game, GravityState.FLOOR);

        walkTo(game, MoveIntent.LEFT, GravityState.WEST_WALL, "02-west-wall");
        walkTo(game, MoveIntent.UP, GravityState.CEILING, "03-ceiling");
        walkTo(game, MoveIntent.RIGHT, GravityState.EAST_WALL, "04-east-wall");
        walkTo(game, MoveIntent.DOWN, GravityState.FLOOR, "05-ground-again");

        // A blend has to actually take frames; a snap would make the corner unreadable.
        press(game, MoveIntent.LEFT, 1);
        frames(game, 1);
        shot("06-mid-blend");

        frames(game, SETTLE_FRAMES);
        check(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "GL reported an error");
    }

    /** Presses one direction until the plane changes, then records the frame. */
    private static void walkTo(ExampleGame game, MoveIntent input, GravityState expected, String name)
        throws Exception {
        for (int step = 0; step < 24; step++) {
            press(game, input, SETTLE_FRAMES);
            if (plane(game) == expected) {
                shot(name);
                return;
            }
        }
        throw new IllegalStateException("never reached " + expected + ", stuck on " + plane(game));
    }

    private static void press(ExampleGame game, MoveIntent input, int settle) throws Exception {
        SCRIPTED[0] = input;
        frames(game, 1);
        SCRIPTED[0] = MoveIntent.NONE;
        frames(game, settle);
    }

    private static void frames(ExampleGame game, int count) {
        for (int i = 0; i < count; i++) {
            game.render(FRAME);
            check(Gdx.gl.glGetError() == GL20.GL_NO_ERROR, "GL reported an error");
        }
    }

    private static GravityState plane(ExampleGame game) throws Exception {
        Object screen = get(game, "distortion");
        check(screen != null, "the cave closed unexpectedly");
        return ((DistortionScreen) screen).gravity();
    }

    private static void checkPlane(ExampleGame game, GravityState expected) throws Exception {
        check(plane(game) == expected, "expected " + expected + " but stood on " + plane(game));
    }

    private static void shot(String name) {
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0,
            Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            PixmapIO.writePNG(Gdx.files.local("build/smoke/" + name + ".png"), pixels, -1, true);
        } finally {
            pixels.dispose();
        }
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
