package land.temmi.rollercoaster.web;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import land.temmi.rollercoaster.example.ExampleGame;

public final class WebLauncher {
    private WebLauncher() { }

    public static void main(String[] args) {
        WebApplicationConfiguration config = new WebApplicationConfiguration();
        // Zero size fills the browser window.
        config.width = 0;
        config.height = 0;
        config.useGL30 = true;
        new WebApplication(new ExampleGame(), config);
    }
}
