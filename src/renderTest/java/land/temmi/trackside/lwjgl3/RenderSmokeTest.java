package land.temmi.trackside.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import land.temmi.trackside.example.ExampleGame;
import land.temmi.rollercoaster.render.WorldShaderProvider;
import land.temmi.rollercoaster.actor.DirectionalSpriteAnimation;
import land.temmi.rollercoaster.actor.Facing;
import land.temmi.rollercoaster.asset.ModelDefinition;
import land.temmi.rollercoaster.asset.SpriteAtlas;
import land.temmi.rollercoaster.asset.SpriteDefinition;
import land.temmi.rollercoaster.asset.SpriteManifest;
import land.temmi.trackside.example.ExampleMap;
import land.temmi.rollercoaster.actor.GridActor;
import land.temmi.rollercoaster.input.MoveIntent;
import land.temmi.rollercoaster.world.MapLoader;
import land.temmi.rollercoaster.world.MapProp;
import land.temmi.rollercoaster.world.TerrainRules;
import land.temmi.rollercoaster.world.WorldScene;
import land.temmi.rollercoaster.world.TileShape;
import land.temmi.rollercoaster.world.TileSurface;
import land.temmi.rollercoaster.world.Tileset;

/** Desktop GL integration check; run with :lwjgl3:renderSmokeTest. */
public final class RenderSmokeTest extends ExampleGame {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Trackside render verification");
        config.setWindowedMode(960, 540);
        config.disableAudio(true);
        new Lwjgl3Application(new RenderSmokeTest(), config);
    }

    @Override
    public void render() {
        super.render();
        Pixmap screenshot = Pixmap.createFromFrameBuffer(0, 0,
            Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            PixmapIO.writePNG(Gdx.files.absolute(System.getProperty("java.io.tmpdir") + "/trackside-step6.png"),
                screenshot, -1, true);
        } finally {
            screenshot.dispose();
        }
        verifyChunks();
        verifyMapDocument();
        verifyTerrainGrid();
        verifyTerrainSteps();
        verifyDepthAndTexture();
        verifySprites();
        if (Gdx.gl.glGetError() != GL20.GL_NO_ERROR) throw new AssertionError("OpenGL error");
        System.out.println("PASS: four single-mesh chunks, opaque depth in both orders, texture UV transform, four-direction sprite atlas, GL_NO_ERROR");
        Gdx.app.exit();
    }

    private void verifySprites() {
        SpriteManifest manifest = SpriteManifest.load(Gdx.files.classpath("sprites/player.json"));
        SpriteDefinition definition = manifest.sprite("player");
        if (!"sprites/player.atlas".equals(manifest.atlas)
            || definition.worldHeight != 1f || definition.frameDuration != 0.14f) {
            throw new AssertionError("Sprite manifest metadata did not load");
        }
        SpriteAtlas atlas = new SpriteAtlas(manifest.atlas);
        try {
            DirectionalSpriteAnimation animation = new DirectionalSpriteAnimation(atlas, definition);
            int previousX = -1;
            for (Facing facing : Facing.values()) {
                animation.setFacing(facing);
                animation.setMoving(false);
                if (animation.getFrame().getRegionWidth() != 16 || animation.getFrame().getRegionHeight() != 24) {
                    throw new AssertionError("Sprite frame dimensions are not 16x24");
                }
                if (previousX == animation.getFrame().getRegionX()) {
                    throw new AssertionError("Sprite directions share one atlas region");
                }
                previousX = animation.getFrame().getRegionX();
            }
            animation.setFacing(Facing.SOUTH);
            animation.setMoving(true);
            int firstWalkFrame = animation.getFrame().getRegionX();
            animation.update(0.15f);
            if (animation.getFrame().getRegionWidth() != 16 || !animation.isMoving()
                || animation.getFrame().getRegionX() == firstWalkFrame) {
                throw new AssertionError("Sprite walk animation did not advance");
            }
        } finally {
            atlas.dispose();
        }
    }

    private void verifyMapDocument() {
        Tileset tileset = new Tileset();
        for (String id : new String[]{"grass", "lightGrass", "path", "plateau", "ramp", "rampBlocked"}) {
            tileset.add(new TileSurface(id));
        }
        land.temmi.rollercoaster.world.LoadedMap map = new MapLoader().load(
            Gdx.files.classpath("maps/testfield.json"), tileset);
        if (!"testfeld".equals(map.name) || map.tiles.getWidth() != 24 || map.tiles.getDepth() != 24
            || map.tiles.isBlocked(6, 8) || map.tiles.isBlocked(10, 11)
            || map.props.size != 4 || map.entities.size != 1
            || map.props.first().elevation != 0f
            || !"streetLamp".equals(map.props.get(1).model) || map.props.get(1).x != 10f
            || map.props.get(1).z != 13f || map.props.get(2).z != 3f
            || !"suspensionBridge".equals(map.props.get(3).model)
            || map.props.get(3).x != 12f || map.props.get(3).z != 6f
            || !"player".equals(map.entities.first().type)
            || map.entities.first().x != 12 || map.entities.first().z != 14) {
            throw new AssertionError("Map document did not load its layers, prop, and entity");
        }
        if (map.tiles.getHeight(18, 11) != 0f || map.tiles.getHeight(18, 10) != 0.5f
            || map.tiles.getHeight(18, 9) != 1.5f || map.tiles.getHeight(18, 8) != 2f
            || map.tiles.getHeight(18, 2) != 0.5f || map.tiles.getHeight(18, 3) != 1.5f
            || map.tiles.getHeight(6, 8) != 2f || map.tiles.getHeight(4, 4) != 2f) {
            throw new AssertionError("Raised plateaus and ramp heights did not load");
        }
        if (map.tiles.getShape(4, 10) != TileShape.RAMP_NORTH
            || map.tiles.getShape(4, 9) != TileShape.RAMP_NORTH
            || map.tiles.getShape(18, 10) != TileShape.RAMP_NORTH
            || map.tiles.getShape(18, 9) != TileShape.RAMP_NORTH
            || map.tiles.getShape(18, 2) != TileShape.RAMP_SOUTH
            || map.tiles.getShape(18, 3) != TileShape.RAMP_SOUTH
            || map.tiles.getShape(16, 9) != TileShape.FLAT) {
            throw new AssertionError("One-level ramp shape layer did not load");
        }
    }

    private static void verifyTerrainGrid() {
        TileSurface ground = new TileSurface("ground");
        try {
            new land.temmi.rollercoaster.world.TileMap(1, 1)
                .set(0, 0, ground, 0.5f, TileShape.FLAT, false);
            throw new AssertionError("Flat terrain accepted a fractional height");
        } catch (IllegalArgumentException expected) { }
        try {
            new land.temmi.rollercoaster.world.TileMap(1, 1)
                .set(0, 0, ground, 1f, TileShape.RAMP_EAST, false);
            throw new AssertionError("Ramp terrain accepted a non-midpoint height");
        } catch (IllegalArgumentException expected) { }

        land.temmi.rollercoaster.world.TileMap shortcut = new land.temmi.rollercoaster.world.TileMap(3, 1);
        shortcut.set(0, 0, ground, 0f, TileShape.FLAT, false);
        shortcut.set(1, 0, ground, 0.5f, TileShape.RAMP_EAST, false);
        shortcut.set(2, 0, ground, 2f, TileShape.FLAT, false);
        TerrainRules shortcutRules = new TerrainRules(shortcut);
        if (shortcutRules.step(0, 0, 1, 0) != TerrainRules.Step.ALLOWED
            || shortcutRules.step(1, 0, 1, 0) != TerrainRules.Step.TOO_STEEP) {
            throw new AssertionError("One ramp can still skip a height level");
        }

        land.temmi.rollercoaster.world.TileMap chain = new land.temmi.rollercoaster.world.TileMap(4, 1);
        chain.set(0, 0, ground, 0f, TileShape.FLAT, false);
        chain.set(1, 0, ground, 0.5f, TileShape.RAMP_EAST, false);
        chain.set(2, 0, ground, 1.5f, TileShape.RAMP_EAST, false);
        chain.set(3, 0, ground, 2f, TileShape.FLAT, false);
        TerrainRules chainRules = new TerrainRules(chain);
        if (chainRules.step(0, 0, 1, 0) != TerrainRules.Step.ALLOWED
            || chainRules.step(1, 0, 1, 0) != TerrainRules.Step.ALLOWED
            || chainRules.step(2, 0, 1, 0) != TerrainRules.Step.ALLOWED) {
            throw new AssertionError("Two one-level ramps do not connect the plateau");
        }
    }

    /** Walks the actor across each one-level ramp and checks the cliff beside it stays closed. */
    private void verifyTerrainSteps() {
        land.temmi.rollercoaster.world.TileMap tiles = ExampleMap.getLoadedMap().tiles;
        TerrainRules terrain = new TerrainRules(tiles);
        GridActor.TileAccess access = terrain;
        GridActor west = actorAt(access, 4, 11);
        move(west, MoveIntent.UP, 4, 10, 0.5f);
        move(west, MoveIntent.UP, 4, 9, 1.5f);
        move(west, MoveIntent.UP, 4, 8, 2f);
        GridActor eastSouth = actorAt(access, 18, 11);
        move(eastSouth, MoveIntent.UP, 18, 10, 0.5f);
        move(eastSouth, MoveIntent.UP, 18, 9, 1.5f);
        move(eastSouth, MoveIntent.UP, 18, 8, 2f);
        GridActor eastNorth = actorAt(access, 18, 1);
        move(eastNorth, MoveIntent.DOWN, 18, 2, 0.5f);
        move(eastNorth, MoveIntent.DOWN, 18, 3, 1.5f);
        move(eastNorth, MoveIntent.DOWN, 18, 4, 2f);
        if (terrain.step(17, 9, 1, 0) != TerrainRules.Step.TOO_STEEP
            || terrain.step(19, 9, -1, 0) != TerrainRules.Step.TOO_STEEP) {
            throw new AssertionError("Actor can climb the cliff instead of using the ramp");
        }
        for (int x = 9; x <= 15; x++) {
            if (!tiles.hasWalkableSurface(x, 6) || tiles.isBlocked(x, 6)
                || Math.abs(tiles.getWalkableSurfaceHeight(x, 6) - 2f) > 1e-5f) {
                throw new AssertionError("Bridge deck is not a raised walkable surface at " + x + ",6");
            }
        }
        GridActor underpass = actorAt(access, 11, 10);
        for (int z = 9; z >= 6; z--) {
            underpass.update(0f, MoveIntent.UP);
            underpass.update(1f, MoveIntent.UP);
            if (underpass.getTileZ() != z || Math.abs(underpass.getPosition().y) > 1e-5f) {
                throw new AssertionError("Actor cannot pass below bridge at tile 11," + z);
            }
        }
        GridActor actor = actorAt(access, 8, 6);
        if (terrain.step(8, 6, 1, 0) != TerrainRules.Step.ALLOWED) {
            throw new AssertionError("Plateau does not meet the bridge: " + terrain.step(8, 6, 1, 0));
        }
        for (int x = 9; x <= 16; x++) {
            actor.update(0f, MoveIntent.RIGHT);
            actor.update(1f, MoveIntent.RIGHT);
            float expected = 2f;
            if (actor.getTileX() != x || Math.abs(actor.getPosition().y - expected) > 1e-5f) {
                throw new AssertionError("Actor cannot cross bridge at tile " + x + ": "
                    + actor.getTileX() + "," + actor.getTileZ() + " at " + actor.getPosition().y);
            }
        }
    }

    private static GridActor actorAt(GridActor.TileAccess access, int x, int z) {
        GridActor actor = new GridActor(24, 24, 5f);
        actor.setTileAccess(access);
        actor.setTile(x, z);
        return actor;
    }

    private static void move(GridActor actor, MoveIntent intent, int x, int z, float height) {
        actor.update(0f, intent);
        actor.update(1f, intent);
        if (actor.getTileX() != x || actor.getTileZ() != z
            || Math.abs(actor.getPosition().y - height) > 1e-5f) {
            throw new AssertionError("Actor missed terrain step " + x + "," + z + ": " + actor.getPosition().y);
        }
    }

    private static void assertTranslation(ModelInstance instance, float x, float y, float z, String label) {
        Vector3 translation = instance.transform.getTranslation(new Vector3());
        if (Math.abs(translation.x - x) > 1e-5f || Math.abs(translation.y - y) > 1e-5f
            || Math.abs(translation.z - z) > 1e-5f) {
            throw new AssertionError(label + " is not centred on its tile: " + translation);
        }
    }

    private void verifyChunks() {
        // createScene exercises the real path: it bakes the chunks, validates the prop
        // footprint against the collision layer and instantiates the catalog models.
        WorldScene scene = ExampleMap.createScene();
        try {
            Array<Model> chunks = new Array<>();
            for (Model chunk : sceneChunks(scene)) chunks.add(chunk);
            if (chunks.size != 4) throw new AssertionError("Expected four chunks");
            if (scene.getInstances().size != 8) {
                throw new AssertionError("Expected four chunk instances, house, two lamps, and bridge");
            }
            Array<ModelInstance> visible = new Array<>();
            scene.getVisibleInstances(housePeekCamera(), visible);
            ModelInstance houseInstance = null;
            for (ModelInstance instance : scene.getInstances()) {
                if (instance.userData instanceof MapProp && "house".equals(((MapProp) instance.userData).model)) {
                    houseInstance = instance;
                    break;
                }
            }
            if (houseInstance == null || !visible.contains(houseInstance, true)) {
                throw new AssertionError("House prop was culled at its own position");
            }
            // The footprint is authored on the model, so placing the prop must block these tiles
            // even though the map document marks nothing.
            land.temmi.rollercoaster.world.TileMap tiles = scene.getMap().tiles;
            if (!tiles.isBlocked(6, 8) || !tiles.isBlocked(10, 11) || !tiles.isBlocked(8, 10)
                || !tiles.isBlocked(10, 3) || !tiles.isBlocked(10, 13)
                || tiles.isBlocked(9, 3) || tiles.isBlocked(11, 3)
                || tiles.isBlocked(10, 2) || tiles.isBlocked(10, 4)
                || tiles.isBlocked(9, 13) || tiles.isBlocked(11, 13)
                || tiles.isBlocked(10, 12) || tiles.isBlocked(10, 14)
                || tiles.isBlocked(5, 8) || tiles.isBlocked(11, 8)) {
                throw new AssertionError("Model collision was not merged into the map collision");
            }
            GridActor lampApproach = actorAt(new TerrainRules(tiles), 10, 14);
            lampApproach.update(0f, MoveIntent.UP);
            lampApproach.update(1f, MoveIntent.UP);
            if (lampApproach.getTileX() != 10 || lampApproach.getTileZ() != 14) {
                throw new AssertionError("Street lamp does not block its rendered tile");
            }
            for (int x = 9; x <= 15; x++) if (tiles.isBlocked(x, 6) || !tiles.hasWalkableSurface(x, 6)) {
                throw new AssertionError("Bridge collision footprint is not walkable");
            }
            int blocked = 0;
            for (int z = 0; z < tiles.getDepth(); z++) {
                for (int x = 0; x < tiles.getWidth(); x++) if (tiles.isBlocked(x, z)) blocked++;
            }
            if (blocked != 22) throw new AssertionError("Expected house and lamp footprints, got " + blocked);
            ModelDefinition house = ExampleMap.getModelCatalog().definition("house");
            if (!"gltf:models/house.gltf".equals(house.source)
                || house.offsetX != -0.5f || house.offsetY != 0f || house.offsetZ != -0.5f
                || house.scale != 1f || house.height != 4f
                || house.boundsMinX != -1.8f || house.boundsMinY != 0f || house.boundsMinZ != -1.3f
                || house.boundsMaxX != 2.8f || house.boundsMaxY != 4f || house.boundsMaxZ != 2.3f
                || house.collisionMinX != -2 || house.collisionMaxX != 2
                || house.collisionMinZ != -2 || house.collisionMaxZ != 1) {
                throw new AssertionError("Model manifest did not load house metadata");
            }
            ModelDefinition lamp = ExampleMap.getModelCatalog().definition("streetLamp");
            if (!"gltf:models/street-lamp.gltf".equals(lamp.source)
                || lamp.offsetX != 0f || lamp.offsetY != 0f || lamp.offsetZ != 0f
                || lamp.scale != 1f || lamp.height != 2.9f
                || lamp.boundsMinX != -0.2f || lamp.boundsMinY != 0f || lamp.boundsMinZ != -0.2f
                || lamp.boundsMaxX != 0.2f || lamp.boundsMaxY != 2.9f || lamp.boundsMaxZ != 0.2f
                || lamp.collisionMinX != 0 || lamp.collisionMaxX != 0
                || lamp.collisionMinZ != 0 || lamp.collisionMaxZ != 0) {
                throw new AssertionError("Model manifest did not load street lamp metadata");
            }
            ModelDefinition bridge = ExampleMap.getModelCatalog().definition("suspensionBridge");
            if (!"gltf:models/suspension-bridge.gltf".equals(bridge.source)
                || bridge.offsetX != 0f || bridge.offsetY != -0.12f || bridge.offsetZ != 0f
                || bridge.scale != 1f || bridge.height != 3.04f
                || bridge.boundsMinX != -3.5f || bridge.boundsMinY != 0f || bridge.boundsMinZ != -0.45f
                || bridge.boundsMaxX != 3.5f || bridge.boundsMaxY != 3.04f || bridge.boundsMaxZ != 0.45f
                || bridge.collisionMinX != -3 || bridge.collisionMaxX != 3
                || bridge.collisionMinZ != 0 || bridge.collisionMaxZ != 0
                || !bridge.walkable || bridge.walkHeight != 2.12f) {
                throw new AssertionError("Model manifest did not load bridge metadata");
            }
            assertTranslation(houseInstance, 7f, 0f, 9f, "house");
            for (ModelInstance instance : scene.getInstances()) {
                if (!(instance.userData instanceof MapProp)) continue;
                MapProp prop = (MapProp) instance.userData;
                if ("streetLamp".equals(prop.model)) {
                    assertTranslation(instance, prop.x - 0.5f, 0f, prop.z - 0.5f, "street lamp");
                } else if ("suspensionBridge".equals(prop.model)) {
                    assertTranslation(instance, 11.5f, -0.12f, 5.5f, "bridge");
                }
            }
            for (Model chunk : chunks) {
                if (chunk.meshes.size != 1 || chunk.meshParts.size != 1 || chunk.materials.size != 1) {
                    throw new AssertionError("Expected one mesh and material per chunk");
                }
                float[] vertices = new float[chunk.meshes.first().getNumVertices()
                    * chunk.meshes.first().getVertexSize() / 4];
                chunk.meshes.first().getVertices(vertices);
                for (float value : vertices) if (!Float.isFinite(value)) throw new AssertionError("Invalid vertex");
            }
        } finally {
            scene.dispose();
        }
    }

    private static Array<Model> sceneChunks(WorldScene scene) {
        Array<Model> chunks = new Array<>();
        for (ModelInstance instance : scene.getInstances()) {
            if (instance.userData == WorldScene.TERRAIN_TAG && instance.model != null
                && !chunks.contains(instance.model, true)) chunks.add(instance.model);
        }
        return chunks;
    }

    /**
     * Frames the house closely from the south. The far plane deliberately sits in front of the
     * world origin, so a prop culled by untransformed model-space bounds fails this check.
     */
    private static PerspectiveCamera housePeekCamera() {
        PerspectiveCamera camera = new PerspectiveCamera(30f, 640f, 360f);
        camera.position.set(7.5f, 3f, 14f);
        camera.direction.set(0f, 0f, -1f);
        camera.up.set(0f, 1f, 0f);
        camera.near = 1f;
        camera.far = 9f;
        camera.normalizeUp();
        camera.update();
        return camera;
    }

    private void verifyDepthAndTexture() {
        FrameBuffer target = new FrameBuffer(Pixmap.Format.RGB888, 32, 32, true);
        ModelBatch batch = new ModelBatch(new WorldShaderProvider());
        ModelBuilder builder = new ModelBuilder();
        Model front = builder.createBox(2, 2, 0.1f, new Material(ColorAttribute.createDiffuse(Color.RED)), Usage.Position);
        Pixmap pixels = new Pixmap(2, 1, Pixmap.Format.RGBA8888);
        pixels.setBlending(Pixmap.Blending.None);
        pixels.drawPixel(0, 0, Color.rgba8888(Color.BLUE));
        pixels.drawPixel(1, 0, Color.rgba8888(0, 1, 0, 0));
        Texture texture = new Texture(pixels);
        pixels.dispose();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureAttribute atlas = TextureAttribute.createDiffuse(texture);
        atlas.offsetU = 0.75f;
        atlas.scaleU = 0f;
        Model back = builder.createBox(2, 2, 0.1f, new Material(atlas), Usage.Position | Usage.TextureCoordinates);
        ModelInstance near = new ModelInstance(front);
        ModelInstance far = new ModelInstance(back);
        near.transform.setToTranslation(0, 0, 1);
        OrthographicCamera camera = new OrthographicCamera(4, 4);
        camera.position.set(0, 0, 5);
        camera.near = 0.1f;
        camera.far = 10;
        camera.update();
        try {
            target.begin();
            for (int pass = 0; pass < 3; pass++) {
                Gdx.gl.glClearColor(0, 0, 0, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
                // Separate flushes exercise both submission orders regardless of batch sorting.
                batch.begin(camera);
                batch.render(pass == 0 ? near : far);
                batch.flush();
                if (pass < 2) batch.render(pass == 0 ? far : near);
                batch.end();
                Pixmap result = Pixmap.createFromFrameBuffer(16, 16, 1, 1);
                int actual = result.getPixel(0, 0);
                result.dispose();
                int expected = Color.rgba8888(pass < 2 ? Color.RED : Color.GREEN);
                if (actual != expected) throw new AssertionError("Pass " + pass + ": " + Integer.toHexString(actual));
            }
            target.end();
        } finally {
            batch.dispose();
            front.dispose();
            back.dispose();
            texture.dispose();
            target.dispose();
        }
    }
}
