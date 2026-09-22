package land.temmi.rollercoaster.example;

import com.badlogic.gdx.Gdx;
import land.temmi.rollercoaster.asset.ModelCatalog;
import land.temmi.rollercoaster.asset.ModelDefinition;
import land.temmi.rollercoaster.asset.ModelManifest;
import land.temmi.rollercoaster.asset.GltfModelFactory;
import land.temmi.rollercoaster.world.LoadedMap;
import land.temmi.rollercoaster.world.TextureTileset;
import land.temmi.rollercoaster.world.WorldScene;
import land.temmi.rollercoaster.world.WorldSceneLoader;

/**
 * Example world: a path cross, two raised plateaus with three ramps, a walkable bridge, and a
 * house — loaded entirely from maps/ the same way a real rollercoaster-editor export would be
 * consumed, through {@link TextureTileset}/{@link WorldSceneLoader}/{@link ModelManifest}. No
 * hand-built Tileset or geometry lives in this class; heights, shapes and tile appearance all
 * come from the exported map and tileset documents.
 */
public final class ExampleMap {
    private static ModelCatalog modelCatalog;
    private static TextureTileset tileset;
    private static LoadedMap loadedMap;
    private ExampleMap() { }

    /** The scene derives prop collision and owns the chunk models; the catalog stays here. */
    public static WorldScene createScene() {
        tileset = new TextureTileset(Gdx.files.classpath("maps/overworld.json"));
        WorldScene scene = new WorldSceneLoader().load(Gdx.files.classpath("maps/testfield.json"),
            tileset.getTileset(), tileset.createMaterial(), getModelCatalog());
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

    /** Releases the cached catalog and tileset atlas so a relaunched application rebuilds them. */
    public static void dispose() {
        if (modelCatalog != null) modelCatalog.dispose();
        modelCatalog = null;
        if (tileset != null) tileset.dispose();
        tileset = null;
        loadedMap = null;
    }
}
