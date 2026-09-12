package land.temmi.trackside.example;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.utils.Array;
import land.temmi.rollercoaster.asset.ModelCatalog;
import land.temmi.rollercoaster.asset.ModelDefinition;
import land.temmi.rollercoaster.asset.ModelManifest;
import land.temmi.rollercoaster.asset.GltfModelFactory;
import land.temmi.rollercoaster.world.ChunkMesher;
import land.temmi.rollercoaster.world.LoadedMap;
import land.temmi.rollercoaster.world.MapLoader;
import land.temmi.rollercoaster.world.TileSurface;
import land.temmi.rollercoaster.world.Tileset;
import land.temmi.rollercoaster.world.WorldScene;
import land.temmi.rollercoaster.world.WorldSceneLoader;

/**
 * Example world: a path cross, two raised plateaus with three ramps, a walkable bridge, and a
 * house. Tile types carry nothing but colour and walkability — heights, shapes and all geometry
 * come from the map document through {@link ChunkMesher}.
 */
public final class ExampleMap {
    private static final float SIDE_TINT = 0.72f;

    private static ModelCatalog modelCatalog;
    private static LoadedMap loadedMap;
    private ExampleMap() { }

    public static Array<Model> create() {
        Tileset tileset = createTileset();
        loadedMap = new MapLoader().load(Gdx.files.classpath("maps/testfield.json"), tileset);
        return new ChunkMesher().build(loadedMap.tiles, new Material());
    }

    private static Tileset createTileset() {
        return new Tileset()
            .add(tile("grass", new Color(0.34f, 0.58f, 0.25f, 1f)))
            .add(tile("lightGrass", new Color(0.38f, 0.63f, 0.28f, 1f)))
            .add(tile("path", new Color(0.76f, 0.65f, 0.43f, 1f)))
            .add(tile("plateau", new Color(0.47f, 0.64f, 0.30f, 1f)))
            .add(tile("ramp", new Color(0.70f, 0.60f, 0.40f, 1f)))
            // Same slope, but no actor may step onto it.
            .add(tile("rampBlocked", new Color(0.46f, 0.44f, 0.42f, 1f)).setWalkable(false));
    }

    private static TileSurface tile(String id, Color color) {
        return new TileSurface(id).setColor(color, SIDE_TINT);
    }

    /** The scene derives prop collision and owns the chunk models; the catalog stays here. */
    public static WorldScene createScene() {
        WorldScene scene = new WorldSceneLoader().load(
            Gdx.files.classpath("maps/testfield.json"), createTileset(), new Material(), getModelCatalog());
        loadedMap = scene.getMap();
        return scene;
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
}
