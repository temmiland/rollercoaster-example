package land.temmi.trackside.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import land.temmi.rollercoaster.actor.DirectionalSpriteAnimation;
import land.temmi.rollercoaster.actor.Facing;
import land.temmi.rollercoaster.actor.GridActor;
import land.temmi.rollercoaster.asset.SpriteAtlas;
import land.temmi.rollercoaster.asset.SpriteDefinition;
import land.temmi.rollercoaster.asset.SpriteManifest;
import land.temmi.rollercoaster.input.MoveIntent;
import land.temmi.rollercoaster.render.BillboardQuad;
import land.temmi.rollercoaster.render.BillboardRenderer;
import land.temmi.rollercoaster.render.LightingEnvironment;
import land.temmi.rollercoaster.render.LowResTarget;
import land.temmi.rollercoaster.render.PixelCamera;
import land.temmi.rollercoaster.render.WorldShaderProvider;
import land.temmi.rollercoaster.world.TerrainRules;
import land.temmi.rollercoaster.world.WorldScene;
import land.temmi.trackside.example.ExampleMap;

/** Compares the visible character silhouette with its unobstructed reference in both draw orders. */
final class OcclusionChecks {
    private enum Visibility { FULL, PARTIAL, HIDDEN }

    private final WorldScene scene = ExampleMap.createScene();
    private final SpriteManifest manifest = SpriteManifest.load(Gdx.files.classpath("sprites/player.json"));
    private final SpriteDefinition definition = manifest.sprite("player");
    private final SpriteAtlas atlas = new SpriteAtlas(manifest.atlas);
    private final DirectionalSpriteAnimation animation = new DirectionalSpriteAnimation(atlas, definition);
    private final BillboardQuad quad = new BillboardQuad();
    private final BillboardRenderer sprite = new BillboardRenderer(quad, atlas.getTexture(), animation.getFrame(), definition.worldHeight);
    private final ModelBatch batch = new ModelBatch(new WorldShaderProvider(
        new LightingEnvironment().setAmbient(Color.WHITE, 1f).setSunIntensity(0f)));
    private final LowResTarget target = new LowResTarget();
    private final PixelCamera camera = new PixelCamera();
    private final StringBuilder failures = new StringBuilder();
    private int checks;

    static void verify() {
        OcclusionChecks test = new OcclusionChecks();
        try { test.run(); }
        finally { test.dispose(); }
    }

    private void run() {
        sprite.setBottomPadding(2f / 24f);
        target.resize(640, 360);
        camera.resize(target.getWidth(), target.getHeight());
        for (Facing facing : Facing.values()) {
            animation.setFacing(facing);
            sprite.setRegion(animation.getFrame());
            GridActor actor = actorAt(8, 12);
            actor.update(0f, MoveIntent.UP);
            if (actor.isMoving()) throw new AssertionError("Actor can enter the house footprint");
            check("house-front-" + facing, actor.getPosition(), scene.getInstances(), Visibility.FULL, true);
        }
        walkRamp("west-ramp", 4, 11, MoveIntent.UP, MoveIntent.DOWN);
        walkRamp("east-south-ramp", 18, 11, MoveIntent.UP, MoveIntent.DOWN);
        walkRamp("east-north-ramp", 18, 1, MoveIntent.DOWN, MoveIntent.UP);
        check("bridge-deck", new Vector3(11.5f, 2f, 5.5f), scene.getInstances(), Visibility.FULL, true);
        verifyWalls();
        if (failures.length() > 0) throw new AssertionError(failures.toString());
        System.out.println("PASS: " + checks + " sprite image checks at houses, moving on ramps, bridge deck and wall occlusion");
    }

    private GridActor actorAt(int x, int z) {
        GridActor actor = new GridActor(scene.getMap().tiles.getWidth(), scene.getMap().tiles.getDepth(), 1f);
        actor.setTileAccess(new TerrainRules(scene.getMap().tiles));
        actor.setTile(x, z);
        return actor;
    }

    private void walkRamp(String name, int x, int z, MoveIntent ascending, MoveIntent descending) {
        GridActor actor = actorAt(x, z);
        for (MoveIntent direction : new MoveIntent[] {ascending, descending}) {
            animation.setFacing(direction.facing);
            animation.setMoving(true);
            for (int step = 0; step < 3; step++) {
                actor.update(0f, direction);
                if (!actor.isMoving()) throw new AssertionError("Ramp step blocked: " + name + " " + direction);
                for (int frame = 0; frame < 8; frame++) {
                    actor.update(0.125f, MoveIntent.NONE);
                    animation.update(0.125f);
                    sprite.setRegion(animation.getFrame());
                    check(name + "-" + direction + "-" + step + "-" + frame, actor.getPosition(),
                        scene.getInstances(), Visibility.FULL, step == 1 && frame == 7);
                }
            }
        }
        animation.setMoving(false);
    }

    private void verifyWalls() {
        Model wall = new ModelBuilder().createBox(2f, 4f, 0.1f,
            new Material(ColorAttribute.createDiffuse(Color.MAGENTA)), Usage.Position);
        ModelInstance instance = new ModelInstance(wall);
        Array<ModelInstance> objects = new Array<>();
        objects.add(instance);
        try {
            instance.transform.setToTranslation(0f, 2f, 1f);
            check("wall-in-front", Vector3.Zero, objects, Visibility.HIDDEN, true);
            instance.transform.setToTranslation(0f, 2f, -1f);
            check("wall-behind", Vector3.Zero, objects, Visibility.FULL, true);
            instance.transform.setToTranslation(0f, -1.25f, 0.25f);
            check("low-wall-in-front", Vector3.Zero, objects, Visibility.PARTIAL, true);
        } finally { wall.dispose(); }
    }

    private void check(String name, Vector3 foot, Array<ModelInstance> objects, Visibility visibility, boolean screenshot) {
        sprite.setPosition(foot);
        camera.follow(foot, 1f, 48f);
        camera.snapToPixelGrid(target.getWidth(), target.getHeight());
        sprite.setBasis(camera.right(new Vector3()), camera.camera.up);
        Pixmap reference = draw(null, false);
        try {
            for (boolean spriteFirst : new boolean[] {false, true}) {
                Pixmap actual = draw(objects, spriteFirst);
                int expected = 0, missing = 0;
                try {
                    for (int py = 0; py < reference.getHeight(); py++) {
                        for (int px = 0; px < reference.getWidth(); px++) {
                            int pixel = reference.getPixel(px, py);
                            if ((pixel >>> 8) == 0) continue;
                            expected++;
                            if (actual.getPixel(px, py) != pixel) missing++;
                        }
                    }
                    boolean valid = expected > 0 && (visibility == Visibility.FULL ? missing == 0
                        : visibility == Visibility.HIDDEN ? missing == expected : missing > 0 && missing < expected);
                    checks++;
                    if ((!spriteFirst && screenshot) || !valid) {
                        PixmapIO.writePNG(Gdx.files.local("build/occlusion/" + name + "-" + spriteFirst + ".png"), actual, -1, true);
                    }
                    if (!valid && failures.length() < 4000) {
                        failures.append(name).append(" (first=").append(spriteFirst).append("): ")
                            .append(missing).append('/').append(expected).append(" pixels hidden; expected ")
                            .append(visibility).append('\n');
                    }
                } finally { actual.dispose(); }
            }
        } finally { reference.dispose(); }
    }

    private Pixmap draw(Array<ModelInstance> objects, boolean spriteFirst) {
        target.begin();
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera.camera);
        if (spriteFirst) { batch.render(sprite); batch.flush(); }
        if (objects != null) { batch.render(objects); batch.flush(); }
        if (!spriteFirst) batch.render(sprite);
        batch.end();
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, target.getWidth(), target.getHeight());
        target.end();
        return pixels;
    }

    private void dispose() {
        target.dispose(); batch.dispose(); sprite.dispose(); quad.dispose(); atlas.dispose(); scene.dispose();
    }
}
