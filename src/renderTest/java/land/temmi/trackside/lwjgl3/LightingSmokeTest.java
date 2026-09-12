package land.temmi.trackside.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import land.temmi.trackside.example.ExampleGame;

/** Measures lighting in a real GL context and captures both demo lighting states. */
public final class LightingSmokeTest extends ExampleGame {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Rollercoaster lighting verification");
        config.setWindowedMode(960, 540);
        config.disableAudio(true);
        new Lwjgl3Application(new LightingSmokeTest(), config);
    }

    @Override public void render() {
        setDemoTime(11f);
        super.render();
        capture("trackside-day.png");
        setDemoTime(22f);
        super.render();
        capture("trackside-night.png");
        LightingChecks.verify();
        if (Gdx.gl.glGetError() != GL20.GL_NO_ERROR) throw new AssertionError("OpenGL error");
        Gdx.app.exit();
    }

    private static void capture(String name) {
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0,
            Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            PixmapIO.writePNG(Gdx.files.absolute(System.getProperty("java.io.tmpdir") + "/" + name), pixels, -1, true);
        } finally { pixels.dispose(); }
    }
}
