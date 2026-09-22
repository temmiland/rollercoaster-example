package land.temmi.rollercoaster.ios;

import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIScene;
import org.robovm.apple.uikit.UISceneConnectionOptions;
import org.robovm.apple.uikit.UISceneSession;
import org.robovm.apple.uikit.UIWindow;
import org.robovm.apple.uikit.UIWindowScene;
import org.robovm.apple.uikit.UIWindowSceneDelegateAdapter;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.Method;

/** Connects LibGDX's legacy window to iOS's scene lifecycle. */
@CustomClass(preload = true)
public final class IOSSceneDelegate extends UIWindowSceneDelegateAdapter {
    private UIWindow window;

    @Override
    public UIWindow getWindow() {
        return window;
    }

    @Override
    public void setWindow(UIWindow window) {
        this.window = window;
    }

    @Override
    @Method(selector = "scene:willConnectToSession:options:")
    public void willConnect(UIScene scene, UISceneSession session, UISceneConnectionOptions options) {
        if (!(scene instanceof UIWindowScene)) return;
        UIWindow appWindow = UIApplication.getSharedApplication().getDelegate().getWindow();
        if (appWindow == null || appWindow.getRootViewController() == null) return;
        UIWindow sceneWindow = new UIWindow((UIWindowScene) scene);
        sceneWindow.setRootViewController(appWindow.getRootViewController());
        setWindow(sceneWindow);
        sceneWindow.makeKeyAndVisible();
    }
}
