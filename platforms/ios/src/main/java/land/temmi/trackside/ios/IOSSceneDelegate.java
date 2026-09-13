package land.temmi.trackside.ios;

import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIScene;
import org.robovm.apple.uikit.UISceneConnectionOptions;
import org.robovm.apple.uikit.UISceneSession;
import org.robovm.apple.uikit.UIWindow;
import org.robovm.apple.uikit.UIWindowScene;
import org.robovm.apple.uikit.UIWindowSceneDelegateAdapter;

/** Connects LibGDX's legacy window to iOS's scene lifecycle. */
public final class IOSSceneDelegate extends UIWindowSceneDelegateAdapter {
    @Override
    public void willConnect(UIScene scene, UISceneSession session, UISceneConnectionOptions options) {
        if (!(scene instanceof UIWindowScene)) return;
        UIWindow window = UIApplication.getSharedApplication().getDelegate().getWindow();
        if (window == null) return;
        window.setWindowScene((UIWindowScene) scene);
        setWindow(window);
    }
}
