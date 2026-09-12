package land.temmi.trackside.example;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import land.temmi.rollercoaster.asset.ModelCatalog;
import land.temmi.rollercoaster.asset.ModelDefinition;
import land.temmi.rollercoaster.asset.ModelManifest;
import land.temmi.rollercoaster.asset.GltfModelFactory;
import land.temmi.rollercoaster.world.ChunkMesher;
import land.temmi.rollercoaster.world.LoadedMap;
import land.temmi.rollercoaster.world.MapLoader;
import land.temmi.rollercoaster.world.MapProp;
import land.temmi.rollercoaster.world.TileMap;
import land.temmi.rollercoaster.world.TilePrototype;
import land.temmi.rollercoaster.world.Tileset;
import land.temmi.rollercoaster.world.WorldScene;

/** Procedural example world with four chunks, a path, a plateau and a house. */
public final class ExampleMap {
    private static ModelCatalog modelCatalog;
    private static LoadedMap loadedMap;
    private static TileMap loadedTiles;
    private ExampleMap() { }

    public static Array<Model> create() {
        Array<TilePrototype> prototypes = new Array<>();
        try {
            TilePrototype grass = ground(new Color(0.34f, 0.58f, 0.25f, 1f), 0.35f);
            prototypes.add(grass);
            TilePrototype lightGrass = ground(new Color(0.38f, 0.63f, 0.28f, 1f), 0.35f);
            prototypes.add(lightGrass);
            TilePrototype path = ground(new Color(0.76f, 0.65f, 0.43f, 1f), 0.35f);
            prototypes.add(path);
            TilePrototype plateau = ground(new Color(0.47f, 0.64f, 0.30f, 1f), 1.35f, 5f, 6f);
            prototypes.add(plateau);
            Tileset tileset = new Tileset()
                .add("grass", grass)
                .add("lightGrass", lightGrass)
                .add("path", path)
                .add("plateau", plateau);
            LoadedMap loaded = new MapLoader().load(
                Gdx.files.classpath("maps/testfield.json"), tileset);
            TileMap map = loaded.tiles;
            loadedMap = loaded;
            loadedTiles = map;
            return new ChunkMesher().build(map, new Material());
        } finally {
            for (TilePrototype prototype : prototypes) prototype.dispose();
        }
    }

    public static WorldScene createScene() {
        Array<Model> chunks = create();
        return new WorldScene(getLoadedMap(), chunks, getModelCatalog());
    }

    public static boolean isBlocked(int x, int z) {
        return loadedTiles != null && loadedTiles.isBlocked(x, z);
    }

    public static LoadedMap getLoadedMap() {
        if (loadedMap == null) throw new IllegalStateException("Test map has not been created");
        return loadedMap;
    }

    public static Array<Model> createPropModels() {
        Array<Model> models = new Array<>();
        for (MapProp prop : getLoadedMap().props) {
            try {
                ModelDefinition definition = getModelCatalog().definition(prop.model);
                validateCollision(prop, definition);
                models.add(getModelCatalog().create(prop.model));
            } catch (RuntimeException failure) {
                getModelCatalog().dispose();
                throw failure;
            }
        }
        return models;
    }

    public static ModelCatalog getModelCatalog() {
        if (modelCatalog == null) {
            modelCatalog = new ModelCatalog();
            for (ModelDefinition definition : ModelManifest.load(Gdx.files.classpath("maps/models.json"))) {
                if (definition.source.startsWith("gltf:") || definition.source.startsWith("glb:")) {
                    modelCatalog.register(definition, new GltfModelFactory(definition));
                } else {
                    throw new IllegalArgumentException("Unknown model source: " + definition.source);
                }
            }
        }
        return modelCatalog;
    }

    private static void validateCollision(MapProp prop, ModelDefinition definition) {
        if (Math.abs(prop.rotation % 360f) > 0.001f) return;
        for (int z = definition.collisionMinZ; z <= definition.collisionMaxZ; z++) {
            for (int x = definition.collisionMinX; x <= definition.collisionMaxX; x++) {
                int mapX = MathUtils.floor(prop.x) + x;
                int mapZ = MathUtils.floor(prop.z) + z;
                if (!loadedTiles.isBlocked(mapX, mapZ)) {
                    throw new IllegalStateException("Prop collision footprint is not blocked: " + definition.id
                        + " at " + mapX + "," + mapZ);
                }
            }
        }
    }

    private static TilePrototype ground(Color color, float thickness) {
        return ground(color, thickness, 1f, 1f);
    }

    private static TilePrototype ground(Color color, float thickness, float width, float depth) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder mesh = builder.part("ground", GL20.GL_TRIANGLES, ChunkMesher.ATTRIBUTES, new Material());
        mesh.setVertexTransform(new Matrix4().setToScaling(width, 1f, depth));
        float sideTint = thickness > 1f ? 0.72f : 1f;
        mesh.setColor(color.r * sideTint, color.g * sideTint, color.b * sideTint, 1f);
        mesh.rect(0, -thickness, 1, 1, -thickness, 1, 1, 0, 1, 0, 0, 1, 0, 0, 1);
        mesh.rect(1, -thickness, 0, 0, -thickness, 0, 0, 0, 0, 1, 0, 0, 0, 0, -1);
        mesh.rect(0, -thickness, 0, 0, -thickness, 1, 0, 0, 1, 0, 0, 0, -1, 0, 0);
        mesh.rect(1, -thickness, 1, 1, -thickness, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0);
        mesh.setColor(color);
        mesh.rect(0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0);
        return new TilePrototype(builder.end());
    }

}
