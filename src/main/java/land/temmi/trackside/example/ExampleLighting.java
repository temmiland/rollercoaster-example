package land.temmi.trackside.example;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import land.temmi.rollercoaster.render.LightingEnvironment;
import land.temmi.rollercoaster.render.PointLightSource;
import land.temmi.rollercoaster.world.WorldScene;
import land.temmi.rollercoaster.world.TerrainSurface;

/** Window emission and street lamps placed in the example scene. */
public final class ExampleLighting implements Disposable {
    final Array<ModelInstance> instances = new Array<>();
    private final Array<Model> models = new Array<>();
    private final Array<PointLightSource> lights = new Array<>();
    private final Array<ColorAttribute> emission = new Array<>();
    private final LightingEnvironment environment;
    private final Color warm = new Color(1f, 0.82f, 0.56f, 1f);
    private boolean enabled = true;
    private final Array<Float> intensities = new Array<>();

    public ExampleLighting(LightingEnvironment environment, WorldScene scene) {
        this.environment = environment;
        for (ModelInstance instance : scene.getInstances()) {
            for (Material material : instance.materials) {
                if (!"windows".equals(material.id)) continue;
                emission.add((ColorAttribute) material.get(ColorAttribute.Emissive));
                window(instance, -0.7f);
                window(instance, 1.7f);
            }
        }
        TerrainSurface terrain = new TerrainSurface(scene.getMap().tiles);
        lamp(10.8f, terrain.heightAt(10.8f, 13f), 13f);
        lamp(10.8f, terrain.heightAt(10.8f, 5f), 5f);
    }

    private void window(ModelInstance house, float x) {
        Vector3 position = new Vector3(x, 1.65f, 2.12f).mul(house.transform);
        Vector3 direction = new Vector3(0f, -0.5f, 1f).rot(house.transform).nor();
        add(new PointLightSource(position.x, position.y, position.z, warm, 0.8f, 4.5f)
            .setSpot(direction, 75f, 145f));
    }

    private void lamp(float x, float y, float z) {
        ModelBuilder builder = new ModelBuilder();
        Model post = builder.createBox(0.12f, 2.6f, 0.12f,
            new Material(ColorAttribute.createDiffuse(new Color(0.16f, 0.19f, 0.23f, 1f))), Usage.Position | Usage.Normal);
        models.add(post);
        instances.add(new ModelInstance(post, x, y + 1.3f, z));
        Model bulb = builder.createBox(0.4f, 0.3f, 0.4f,
            new Material(ColorAttribute.createDiffuse(warm), ColorAttribute.createEmissive(warm)), Usage.Position | Usage.Normal);
        models.add(bulb);
        ModelInstance fixture = new ModelInstance(bulb, x, y + 2.75f, z);
        instances.add(fixture);
        emission.add((ColorAttribute) fixture.materials.first().get(ColorAttribute.Emissive));
        add(new PointLightSource(x, y + 2.7f, z, warm, 1.25f, 7f));
    }

    private void add(PointLightSource light) {
        lights.add(light);
        intensities.add(light.intensity);
        environment.addPointLight(light);
    }

    public void toggle() {
        enabled = !enabled;
        update();
    }

    public void update() {
        float night = 1f - Math.min(1f, environment.getSunIntensity() / 0.95f);
        for (int i = 0; i < lights.size; i++) {
            lights.get(i).enabled = enabled && night > 0f;
            lights.get(i).intensity = intensities.get(i) * night;
        }
        for (ColorAttribute glow : emission) glow.color.set(warm).mul(enabled ? night : 0f);
    }

    boolean isEnabled() { return lights.size > 0 && lights.first().enabled; }

    @Override public void dispose() {
        for (PointLightSource light : lights) environment.removePointLight(light);
        for (Model model : models) model.dispose();
    }
}
