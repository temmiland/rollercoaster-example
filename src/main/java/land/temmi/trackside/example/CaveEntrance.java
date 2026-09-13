package land.temmi.trackside.example;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Disposable;
import land.temmi.rollercoaster.world.TileMap;

/** Untextured rock arch at the northern end of the main path. */
public final class CaveEntrance implements Disposable {
    public static final int X = 12;
    public static final int Z = 2;
    private final Model model;
    public final ModelInstance instance;

    public CaveEntrance(TileMap tiles) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        block(builder, "left", "646777", -1f, .9f, 0f, 1f, 1.8f, 1.5f);
        block(builder, "right", "777684", 1f, .85f, 0f, 1f, 1.7f, 1.5f);
        block(builder, "lintel", "93909b", 0f, 1.85f, -.1f, 2.7f, .65f, 1.6f);
        block(builder, "crown", "747787", -.15f, 2.25f, -.25f, 1.8f, .3f, 1.2f);
        block(builder, "darkness", "0b1028", 0f, .75f, -.55f, 1f, 1.5f, .1f);
        block(builder, "threshold", "527dff", 0f, .025f, .1f, .95f, .05f, .8f);
        block(builder, "crystalLeft", "7bbaff", -.6f, .5f, .79f, .12f, .35f, .12f);
        block(builder, "crystalRight", "7bbaff", .6f, .5f, .79f, .12f, .35f, .12f);
        model = builder.end();
        instance = new ModelInstance(model);
        instance.transform.setToTranslation(X - .5f, 0f, Z - .5f);
        for (int x = X - 1; x <= X + 1; x++) {
            tiles.setBlocked(x, Z - 1, true);
            if (x != X) tiles.setBlocked(x, Z, true);
        }
    }

    private static void block(ModelBuilder builder, String name, String color,
                              float x, float y, float z, float w, float h, float d) {
        builder.part(name, GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
            new Material(ColorAttribute.createDiffuse(Color.valueOf(color))))
            .box(x, y, z, w, h, d);
    }

    @Override public void dispose() { model.dispose(); }
}
