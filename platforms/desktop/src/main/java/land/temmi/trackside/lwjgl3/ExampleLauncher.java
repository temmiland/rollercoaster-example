package land.temmi.trackside.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import land.temmi.trackside.example.ExampleGame;

public final class ExampleLauncher {
    private ExampleLauncher() { }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("example-game");
        config.useVsync(true);
        config.setWindowedMode(1280, 720);
        new Lwjgl3Application(new ExampleGame(), config);
    }
}
