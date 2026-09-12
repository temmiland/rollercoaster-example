package land.temmi.trackside.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import land.temmi.rollercoaster.asset.ModelDefinition;
import land.temmi.trackside.example.ExampleMap;
import land.temmi.rollercoaster.world.MapLoader;
import land.temmi.rollercoaster.world.TilePrototype;
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
        verifyDepthAndTexture();
        if (Gdx.gl.glGetError() != GL20.GL_NO_ERROR) throw new AssertionError("OpenGL error");
        System.out.println("PASS: four single-mesh chunks, opaque depth in both orders, texture UV transform, GL_NO_ERROR");
        Gdx.app.exit();
    }

    private void verifyMapDocument() {
        TilePrototype placeholder = new TilePrototype(null);
        Tileset tileset = new Tileset().add("grass", placeholder).add("lightGrass", placeholder)
            .add("path", placeholder).add("plateau", placeholder).add("house", placeholder);
        land.temmi.rollercoaster.world.LoadedMap map = new MapLoader().load(
            Gdx.files.classpath("maps/testfield.json"), tileset);
        if (!"testfeld".equals(map.name) || map.tiles.getWidth() != 24 || map.tiles.getDepth() != 24
            || !map.tiles.isBlocked(6, 8) || !map.tiles.isBlocked(10, 11)
            || map.tiles.isBlocked(5, 8) || map.tiles.isBlocked(10, 12)
            || map.props.size != 1 || map.entities.size != 1
            || map.props.first().elevation != 0f
            || !"player".equals(map.entities.first().type)
            || map.entities.first().x != 12 || map.entities.first().z != 14) {
            throw new AssertionError("Map document did not load its layers, prop, and entity");
        }
    }

    private void verifyChunks() {
        Array<Model> chunks = ExampleMap.create();
        Array<Model> props = ExampleMap.createPropModels();
        try {
            if (chunks.size != 4) throw new AssertionError("Expected four chunks");
            if (props.size != 1) throw new AssertionError("Expected one map prop model");
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
            for (Model chunk : chunks) chunk.dispose();
        }
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
