package land.temmi.trackside.example;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import land.temmi.rollercoaster.render.LightingSituation;
import land.temmi.rollercoaster.render.LightingEnvironment;
import land.temmi.rollercoaster.render.PointLightSource;
import land.temmi.rollercoaster.world.MapProp;
import land.temmi.rollercoaster.world.WorldScene;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;

/** Controls window emission and lights attached to street-lamp props. */
public final class ExampleLighting implements Disposable {
    private static final float EMISSION_BOOST = 2f;
    private final Array<PointLightSource> lights = new Array<>();
    private final Array<ColorAttribute> emission = new Array<>();
    private final Array<ColorAttribute> windowBaseColors = new Array<>();
    private final Array<Color> originalWindowBaseColors = new Array<>();
    private final LightingEnvironment environment;
    /** Shared amber tone for window emission, bulbs, and their local light. */
    private final Color warm = new Color(1f, 0.62f, 0.28f, 1f);
    private boolean enabled = true;
    private final Array<Float> intensities = new Array<>();
    private LightingSituation situation = LightingSituation.DAY;

    public ExampleLighting(LightingEnvironment environment, WorldScene scene) {
        this.environment = environment;
        for (ModelInstance instance : scene.getInstances()) {
            for (Material material : instance.materials) {
                if (!"windows".equals(material.id)) continue;
                emission.add((ColorAttribute) material.get(ColorAttribute.Emissive));
                ColorAttribute baseColor = (ColorAttribute) material.get(PBRColorAttribute.BaseColorFactor);
                if (baseColor == null) baseColor = (ColorAttribute) material.get(ColorAttribute.Diffuse);
                if (baseColor == null) {
                    baseColor = ColorAttribute.createDiffuse(Color.WHITE);
                    material.set(baseColor);
                }
                windowBaseColors.add(baseColor);
                originalWindowBaseColors.add(new Color(baseColor.color));
                window(instance, -0.7f);
                window(instance, 1.7f);
            }
        }
        for (ModelInstance instance : scene.getInstances()) {
            if (!(instance.userData instanceof MapProp)) continue;
            if ("streetLamp".equals(((MapProp) instance.userData).model)) lamp(instance);
        }
    }

    private void window(ModelInstance house, float x) {
        Vector3 position = new Vector3(x, 1.65f, 2.12f).mul(house.transform);
        Vector3 direction = new Vector3(0f, -0.5f, 1f).rot(house.transform).nor();
        add(new PointLightSource(position.x, position.y, position.z, warm, 0.45f, 4.5f)
            .setSpot(direction, 75f, 145f));
    }

    private void lamp(ModelInstance instance) {
        for (Material material : instance.materials) {
            if (!"lampGlow".equals(material.id)) continue;
            ColorAttribute glow = (ColorAttribute) material.get(ColorAttribute.Emissive);
            if (glow != null) emission.add(glow);
        }
        Vector3 position = new Vector3(0f, 2.7f, 0f).mul(instance.transform);
        add(new PointLightSource(position.x, position.y, position.z, warm, 0.70f, 7f));
    }

    private void add(PointLightSource light) {
        lights.add(light);
        intensities.add(light.intensity);
        environment.addPointLight(light);
    }

    public void toggle() {
        enabled = !enabled;
        update(situation);
    }

    public void update(LightingSituation situation) {
        if (situation == null) throw new IllegalArgumentException("Lighting situation is required");
        this.situation = situation;
        float localFactor = situation.getLocalLightFactor();
        for (int i = 0; i < lights.size; i++) {
            lights.get(i).enabled = enabled && localFactor > 0f;
            lights.get(i).intensity = intensities.get(i) * localFactor;
        }
        // Boost only the material emission. Point-light intensities stay on the original
        // localFactor so the illuminated ground and radius do not get brighter.
        for (ColorAttribute glow : emission) glow.color.set(warm).mul(enabled ? localFactor * EMISSION_BOOST : 0f);
        boolean windowsLit = enabled && localFactor > 0f;
        for (int i = 0; i < windowBaseColors.size; i++) {
            windowBaseColors.get(i).color.set(windowsLit ? warm : originalWindowBaseColors.get(i));
        }
    }

    boolean isEnabled() { return lights.size > 0 && lights.first().enabled; }

    @Override public void dispose() {
        for (PointLightSource light : lights) environment.removePointLight(light);
    }
}
