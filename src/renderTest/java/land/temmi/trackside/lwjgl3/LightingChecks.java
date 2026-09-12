package land.temmi.trackside.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import land.temmi.rollercoaster.render.*;
import land.temmi.rollercoaster.world.MapProp;
import land.temmi.rollercoaster.world.WorldScene;
import land.temmi.trackside.example.ExampleMap;
import land.temmi.trackside.example.ExampleLighting;

final class LightingChecks {
    static void verify() {
        LightingEnvironment light = new LightingEnvironment().setAmbient(Color.WHITE, 0.2f)
            .setSun(new Vector3(1, 1, 0), Color.WHITE, 0.8f);
        DirectionalShadowMap shadows = new DirectionalShadowMap(light, 512).setWorldSize(16f);
        ModelBatch batch = new ModelBatch(new WorldShaderProvider(light, shadows));
        FrameBuffer target = new FrameBuffer(Pixmap.Format.RGBA8888, 128, 128, true);
        ModelBuilder builder = new ModelBuilder();
        Model ground = builder.createBox(12, 0.1f, 12, new Material(ColorAttribute.createDiffuse(Color.WHITE)), Usage.Position | Usage.Normal);
        Model block = builder.createBox(2, 2, 2, new Material(ColorAttribute.createDiffuse(Color.WHITE)), Usage.Position | Usage.Normal);
        ModelInstance floor = new ModelInstance(ground, 0, -0.05f, 0);
        floor.userData = WorldScene.TERRAIN_TAG;
        ModelInstance caster = new ModelInstance(block, 0, 1, 0);
        Array<ModelInstance> objects = new Array<>(); objects.add(floor); objects.add(caster);
        OrthographicCamera camera = camera(12, 0, 0);
        try {
            shadows.render(Vector3.Zero, objects);
            Pixmap first = draw(target, batch, camera, objects);
            int dark = redAt(first, camera, -1.8f, 0);
            int lit = redAt(first, camera, 3f, 0);
            first.dispose();
            if (lit < 180 || dark > lit - 70) throw new AssertionError("Missing cast shadow: " + dark + " vs " + lit);

            shadows.setStrength(0);
            Pixmap modelUnshadowed = draw(target, batch, camera, objects);
            int modelLit = redAt(modelUnshadowed, camera, 0f, 0f);
            modelUnshadowed.dispose();
            shadows.setStrength(1);
            Pixmap modelShadowed = draw(target, batch, camera, objects);
            int modelStillLit = redAt(modelShadowed, camera, 0f, 0f);
            modelShadowed.dispose();
            if (Math.abs(modelStillLit - modelLit) > 5) {
                throw new AssertionError("Non-ground geometry received a ground shadow: "
                    + modelLit + " vs " + modelStillLit);
            }

            caster.transform.setToTranslation(4, 1, 4);
            Gdx.gl.glDepthMask(false);
            shadows.render(Vector3.Zero, objects);
            Pixmap moved = draw(target, batch, camera, objects);
            int restored = redAt(moved, camera, -1.8f, 0);
            moved.dispose();
            if (Math.abs(restored - lit) > 5) throw new AssertionError("Stale shadow or self-shadow acne: " + restored + " vs " + lit);

            light.setSunIntensity(0).setAmbientIntensity(0);
            PointLightSource lamp = new PointLightSource(0, 2, 0, Color.WHITE, 1, 5);
            light.addPointLight(lamp);
            shadows.render(Vector3.Zero, objects);
            Pixmap lamps = draw(target, batch, camera, objects);
            int near = redAt(lamps, camera, 0, 0), edge = redAt(lamps, camera, 5, 0);
            lamps.dispose();
            if (near < 140 || edge > 3) throw new AssertionError("Lamp footprint: " + near + " / " + edge);
            lamp.enabled = false;
            floor.materials.first().set(ColorAttribute.createEmissive(new Color(0.8f, 0.4f, 0.1f, 1)));
            Pixmap glow = draw(target, batch, camera, objects);
            int emitted = redAt(glow, camera, 0, 0);
            glow.dispose();
            if (Math.abs(emitted - 204) > 3) throw new AssertionError("Emission depends on lighting: " + emitted);
            System.out.println("PASS: shadow contrast " + dark + "/" + lit + ", moving caster " + restored + ", lamp " + near + "/" + edge + ", emissive " + emitted);
        } finally {
            batch.dispose(); shadows.dispose(); target.dispose(); ground.dispose(); block.dispose();
        }
        verifyWorld();
        verifyAutomaticLights();
    }

    private static void verifyAutomaticLights() {
        WorldScene scene = ExampleMap.createScene();
        LightingEnvironment environment = new LightingEnvironment();
        DayNightCycle clock = new DayNightCycle(environment);
        ExampleLighting demo = new ExampleLighting(environment, scene);
        try {
            float[] hours = {6f, 12f, 18f, 22f};
            LightingSituation[] situations = {
                LightingSituation.EARLY_MORNING,
                LightingSituation.DAY,
                LightingSituation.EARLY_EVENING,
                LightingSituation.NIGHT
            };
            for (int i = 0; i < hours.length; i++) {
                LightingSituation before = clock.getSituation();
                clock.setTimeOfDay(hours[i]);
                if (clock.getSituation() != before) {
                    throw new AssertionError("Clock changed lighting before map entry: " + hours[i]);
                }
                clock.enterMap();
                demo.update(clock.getSituation());
                boolean active = situations[i].getLocalLightFactor() > 0f;
                if (environment.getPointLights().size != 4) throw new AssertionError("Expected two window and two street lights");
                for (PointLightSource lamp : environment.getPointLights()) {
                    if (lamp.enabled != active || (lamp.intensity > 0) != active) throw new AssertionError("Lights active at wrong time: " + hours[i]);
                }
                for (ModelInstance instance : scene.getInstances()) for (Material material : instance.materials) {
                    if (!"windows".equals(material.id)) continue;
                    ColorAttribute glow = (ColorAttribute) material.get(ColorAttribute.Emissive);
                    if (glow == null || (glow.color.r > 0) != active
                        || (active && glow.color.r < 0.5f)) {
                        throw new AssertionError("Window emission at wrong time: " + hours[i]);
                    }
                    ColorAttribute diffuse = (ColorAttribute) material.get(ColorAttribute.Diffuse);
                    if (diffuse == null || (diffuse.color.b < diffuse.color.g) != active) {
                        throw new AssertionError("Window diffuse tint at wrong time: " + hours[i]);
                    }
                }
            }
            demo.toggle(); clock.setTimeOfDay(22).enterMap(); demo.update(clock.getSituation());
            for (PointLightSource lamp : environment.getPointLights()) {
                if (lamp.enabled) throw new AssertionError("Manual off was lost at night");
            }
            clock.setTimeOfDay(12).enterMap();
            clock.cycleSituation();
            if (clock.getSituation() != LightingSituation.EARLY_EVENING) {
                throw new AssertionError("Situation cycle did not advance from day");
            }
            System.out.println("PASS: four fixed situations, map-entry switching, manual off preserved");
        } finally { demo.dispose(); scene.dispose(); }
    }

    private static void verifyWorld() {
        WorldScene scene = ExampleMap.createScene();
        LightingEnvironment light = new LightingEnvironment().setAmbient(Color.WHITE, 0.2f)
            .setSun(new Vector3(1, 1, 0.5f), Color.WHITE, 0.8f);
        DirectionalShadowMap shadows = new DirectionalShadowMap(light).setWorldSize(40);
        ModelBatch batch = new ModelBatch(new WorldShaderProvider(light, shadows));
        FrameBuffer target = new FrameBuffer(Pixmap.Format.RGBA8888, 256, 256, true);
        OrthographicCamera camera = camera(26, 11, 11);
        try {
            Array<ModelInstance> casters = new Array<>();
            for (boolean terrain : new boolean[] {false, true}) {
                casters.clear();
                for (ModelInstance instance : scene.getInstances()) {
                    boolean isTerrain = instance.userData == WorldScene.TERRAIN_TAG;
                    boolean isHouse = instance.userData instanceof MapProp
                        && "house".equals(((MapProp) instance.userData).model);
                    if (terrain ? isTerrain : isHouse) casters.add(instance);
                }
                shadows.render(new Vector3(11, 0, 11), casters);
                shadows.setStrength(0);
                Pixmap unshadowed = draw(target, batch, camera, scene.getInstances());
                shadows.setStrength(1);
                Pixmap shadowed = draw(target, batch, camera, scene.getInstances());
                int changed = 0;
                for (int y=0; y<256; y++) for (int x=0; x<256; x++) {
                    if ((unshadowed.getPixel(x,y) >>> 24) - (shadowed.getPixel(x,y) >>> 24) > 20) changed++;
                }
                unshadowed.dispose(); shadowed.dispose();
                if (changed < 40) throw new AssertionError((terrain ? "Terrain" : "House") + " does not cast a visible shadow: " + changed);
                System.out.println("PASS: " + (terrain ? "terrain" : "house") + " casts shadow on " + changed + " pixels");
            }
        } finally { batch.dispose(); shadows.dispose(); target.dispose(); scene.dispose(); }
    }

    private static OrthographicCamera camera(float size, float x, float z) {
        OrthographicCamera camera = new OrthographicCamera(size, size);
        camera.position.set(x, 30, z); camera.direction.set(0, -1, 0); camera.up.set(0, 0, -1);
        camera.near = 0.1f; camera.far = 80; camera.update();
        return camera;
    }

    private static Pixmap draw(FrameBuffer target, ModelBatch batch, Camera camera, Array<ModelInstance> objects) {
        target.begin();
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera); batch.render(objects); batch.end();
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, target.getWidth(), target.getHeight());
        target.end();
        return pixels;
    }

    private static int redAt(Pixmap pixels, Camera camera, float x, float z) {
        Vector3 screen = camera.project(new Vector3(x,0,z), 0,0,pixels.getWidth(),pixels.getHeight());
        return pixels.getPixel((int)screen.x, (int)screen.y) >>> 24;
    }
}
