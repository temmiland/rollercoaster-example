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
import land.temmi.rollercoaster.asset.ModelCatalog;
import land.temmi.rollercoaster.asset.ModelDefinition;
import land.temmi.rollercoaster.asset.ModelManifest;
import land.temmi.rollercoaster.asset.GltfModelFactory;
import land.temmi.rollercoaster.world.ChunkMesher;
import land.temmi.rollercoaster.world.LoadedMap;
import land.temmi.rollercoaster.world.MapLoader;
import land.temmi.rollercoaster.world.TilePrototype;
import land.temmi.rollercoaster.world.TileShape;
import land.temmi.rollercoaster.world.Tileset;
import land.temmi.rollercoaster.world.WorldScene;

/** Procedural example world with four chunks, a path, a plateau reached by a ramp, and a house. */
public final class ExampleMap {
    private static ModelCatalog modelCatalog;
    private static LoadedMap loadedMap;
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
            TilePrototype plateau = ground(new Color(0.47f, 0.64f, 0.30f, 1f), 1.35f);
            prototypes.add(plateau);
            TilePrototype ramp = rampNorth(new Color(0.70f, 0.60f, 0.40f, 1f), 1.35f);
            prototypes.add(ramp);
            Tileset tileset = new Tileset()
                .add("grass", grass)
                .add("lightGrass", lightGrass)
                .add("path", path)
                .add("plateau", plateau)
                .add("ramp", ramp);
            loadedMap = new MapLoader().load(Gdx.files.classpath("maps/testfield.json"), tileset);
            return new ChunkMesher().build(loadedMap.tiles, new Material());
        } finally {
            for (TilePrototype prototype : prototypes) prototype.dispose();
        }
    }

    /** The scene validates prop footprints and owns the chunk models; the catalog stays here. */
    public static WorldScene createScene() {
        Array<Model> chunks = create();
        return new WorldScene(getLoadedMap(), chunks, getModelCatalog());
    }

    public static LoadedMap getLoadedMap() {
        if (loadedMap == null) throw new IllegalStateException("Test map has not been created");
        return loadedMap;
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

    /** Releases the cached catalog so a relaunched application rebuilds it. */
    public static void dispose() {
        if (modelCatalog != null) modelCatalog.dispose();
        modelCatalog = null;
        loadedMap = null;
    }

    private static TilePrototype ground(Color color, float thickness) {
        return new TilePrototype(tileModel(color, thickness, new Matrix4()));
    }

    /**
     * Tile whose top surface climbs one level from its high edge (local z=0) to its low edge.
     * Built by shearing a flat tile, so every side face keeps its winding.
     */
    private static TilePrototype rampNorth(Color color, float thickness) {
        Matrix4 shear = new Matrix4();
        shear.val[Matrix4.M12] = -1f;  // y falls by one unit across the tile's depth
        shear.val[Matrix4.M13] = 0.5f; // centred, so the tile's layer height is its midpoint
        return new TilePrototype(tileModel(color, thickness, shear), TileShape.RAMP_NORTH, true);
    }

    private static Model tileModel(Color color, float thickness, Matrix4 vertexTransform) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder mesh = builder.part("ground", GL20.GL_TRIANGLES, ChunkMesher.ATTRIBUTES, new Material());
        mesh.setVertexTransform(vertexTransform);
        float sideTint = thickness > 1f ? 0.72f : 1f;
        mesh.setColor(color.r * sideTint, color.g * sideTint, color.b * sideTint, 1f);
        mesh.rect(0, -thickness, 1, 1, -thickness, 1, 1, 0, 1, 0, 0, 1, 0, 0, 1);
        mesh.rect(1, -thickness, 0, 0, -thickness, 0, 0, 0, 0, 1, 0, 0, 0, 0, -1);
        mesh.rect(0, -thickness, 0, 0, -thickness, 1, 0, 0, 1, 0, 0, 0, -1, 0, 0);
        mesh.rect(1, -thickness, 1, 1, -thickness, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0);
        mesh.setColor(color);
        mesh.rect(0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0);
        return builder.end();
    }
}
