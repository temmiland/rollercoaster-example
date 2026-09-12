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
            || definition.worldHeight != 1.8f || definition.frameDuration != 0.14f) {
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
            animation.setMoving(true);
            animation.update(0.15f);
            if (animation.getFrame().getRegionWidth() != 16 || !animation.isMoving()) {
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
            || map.props.size != 1 || map.entities.size != 1
            || map.props.first().elevation != 0f
            || !"player".equals(map.entities.first().type)
            || map.entities.first().x != 12 || map.entities.first().z != 14) {
            throw new AssertionError("Map document did not load its layers, prop, and entity");
        }
        if (map.tiles.getHeight(18, 10) != 0f || map.tiles.getHeight(18, 9) != 0.5f
            || map.tiles.getHeight(18, 8) != 1f || map.tiles.getHeight(17, 9) != 0f) {
            throw new AssertionError("Ramp does not bridge flat ground and the plateau");
        }
        if (map.tiles.getShape(18, 9) != TileShape.RAMP_NORTH
            || map.tiles.getShape(16, 9) != TileShape.RAMP_NORTH
            || map.tiles.getShape(18, 10) != TileShape.FLAT) {
            throw new AssertionError("Shape layer did not load");
        }
    }

    /** Walks the actor up the ramp and checks the cliff beside it stays closed. */
    private void verifyTerrainSteps() {
        land.temmi.rollercoaster.world.TileMap tiles = ExampleMap.getLoadedMap().tiles;
        // Walkability rides on the tile type, shape on the map - the two ramps share a shape
        // and differ only in which tile type they use.
        if (tiles.isWalkable(16, 9) || !tiles.isWalkable(18, 9)) {
            throw new AssertionError("Ramp walkability does not come from the tile type");
        }
        GridActor.TileAccess access = new TerrainRules(tiles);
        if (!climbs(access, 18, 10) || !climbs(access, 18, 9)) {
            throw new AssertionError("Actor cannot walk up the ramp");
        }
        if (climbs(access, 17, 9) || climbs(access, 19, 9)) {
            throw new AssertionError("Actor can climb the cliff instead of using the ramp");
        }
        if (climbs(access, 16, 10)) {
            throw new AssertionError("Actor can step onto a ramp that is scenery only");
        }
        GridActor actor = actorAt(access, 18, 10);
        actor.update(0f, MoveIntent.UP);
        actor.update(1f, MoveIntent.UP);
        if (Math.abs(actor.getPosition().y - 0.5f) > 1e-5f) {
            throw new AssertionError("Actor does not stand on the ramp surface: " + actor.getPosition().y);
        }
    }

    private static GridActor actorAt(GridActor.TileAccess access, int x, int z) {
        GridActor actor = new GridActor(24, 24, 5f);
        actor.setTileAccess(access);
        actor.setTile(x, z);
        return actor;
    }

    private static boolean climbs(GridActor.TileAccess access, int x, int z) {
        GridActor actor = actorAt(access, x, z);
        actor.update(0.001f, MoveIntent.UP);
        return actor.isMoving();
    }

    private void verifyChunks() {
        // createScene exercises the real path: it bakes the chunks, validates the prop
        // footprint against the collision layer and instantiates the catalog models.
        WorldScene scene = ExampleMap.createScene();
        try {
            Array<Model> chunks = new Array<>();
            for (Model chunk : sceneChunks(scene)) chunks.add(chunk);
            if (chunks.size != 4) throw new AssertionError("Expected four chunks");
            if (scene.getInstances().size != 5) {
                throw new AssertionError("Expected four chunk instances and one prop");
            }
            Array<ModelInstance> visible = new Array<>();
            scene.getVisibleInstances(housePeekCamera(), visible);
            if (!visible.contains(scene.getInstances().peek(), true)) {
                throw new AssertionError("House prop was culled at its own position");
            }
            // The footprint is authored on the model, so placing the prop must block these tiles
            // even though the map document marks nothing.
            land.temmi.rollercoaster.world.TileMap tiles = scene.getMap().tiles;
            if (!tiles.isBlocked(6, 8) || !tiles.isBlocked(10, 11) || !tiles.isBlocked(8, 10)
                || tiles.isBlocked(5, 8) || tiles.isBlocked(11, 8) || tiles.isBlocked(10, 12)) {
                throw new AssertionError("Model collision was not merged into the map collision");
            }
            int blocked = 0;
            for (int z = 0; z < tiles.getDepth(); z++) {
                for (int x = 0; x < tiles.getWidth(); x++) if (tiles.isBlocked(x, z)) blocked++;
            }
            if (blocked != 20) throw new AssertionError("Expected a 5x4 derived footprint, got " + blocked);
            ModelDefinition house = ExampleMap.getModelCatalog().definition("house");
            if (!"gltf:models/house.gltf".equals(house.source)
                || house.offsetX != -1f || house.offsetY != 0f || house.offsetZ != -1f
                || house.scale != 1f || house.height != 4f
                || house.boundsMinX != -1.8f || house.boundsMinY != 0f || house.boundsMinZ != -1.3f
                || house.boundsMaxX != 2.8f || house.boundsMaxY != 4f || house.boundsMaxZ != 2.3f
                || house.collisionMinX != -2 || house.collisionMaxX != 2
                || house.collisionMinZ != -2 || house.collisionMaxZ != 1) {
                throw new AssertionError("Model manifest did not load house metadata");
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
            if (instance.model != null && !chunks.contains(instance.model, true)) chunks.add(instance.model);
        }
        chunks.removeValue(scene.getInstances().peek().model, true); // the prop model
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
