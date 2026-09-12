package land.temmi.trackside.ios;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import land.temmi.trackside.example.ExampleGame;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

public final class IOSLauncher extends IOSApplication.Delegate {
    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration configuration = new IOSApplicationConfiguration();
        configuration.useGL30 = true;
        return new IOSApplication(new ExampleGame(), configuration);
    }

    public static void main(String[] args) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        try {
            UIApplication.main(args, null, IOSLauncher.class);
        } finally {
            pool.close();
        }
    }
}
