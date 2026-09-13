package land.temmi.trackside.example;

import land.temmi.rollercoaster.actor.GridActor;
import land.temmi.rollercoaster.input.MoveIntent;
import land.temmi.rollercoaster.world.TerrainRules;
import land.temmi.rollercoaster.world.TileMap;
import land.temmi.rollercoaster.world.TileShape;
import land.temmi.rollercoaster.world.TileSurface;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Ensures the walking actor follows ramp geometry instead of interpolating between tile centres. */
public final class RampMovementTest {
    private static final float EPSILON = 0.0001f;

    @Test public void walkingAcrossAnAscendingRampKeepsFeetOnItsSurface() {
        TileMap map = new TileMap(3, 1);
        TileSurface ground = new TileSurface("ground");
        map.set(0, 0, ground, 0f, TileShape.FLAT, false);
        map.set(1, 0, ground, 0.5f, TileShape.RAMP_EAST, false);
        map.set(2, 0, ground, 1f, TileShape.FLAT, false);

        GridActor actor = new GridActor(3, 1, 1f);
        actor.setTileAccess(new TerrainRules(map));
        actor.setTile(0, 0);

        actor.update(0f, MoveIntent.RIGHT);
        actor.update(0.25f, MoveIntent.NONE);
        assertEquals("The old flat tile must stay flat", 0f, actor.getPosition().y, EPSILON);
        actor.update(0.25f, MoveIntent.NONE);
        assertEquals("The ramp entry is level with the old tile", 0f, actor.getPosition().y, EPSILON);
        actor.update(0.25f, MoveIntent.NONE);
        assertEquals("The actor must rise with the ramp", 0.25f, actor.getPosition().y, EPSILON);
        actor.update(0.25f, MoveIntent.NONE);
        assertEquals("The ramp centre is its stored midpoint", 0.5f, actor.getPosition().y, EPSILON);

        actor.update(0f, MoveIntent.RIGHT);
        actor.update(0.25f, MoveIntent.NONE);
        assertEquals("The second half of the ramp continues to rise", 0.75f, actor.getPosition().y, EPSILON);
        actor.update(0.25f, MoveIntent.NONE);
        assertEquals("The top edge meets the plateau without a drop", 1f, actor.getPosition().y, EPSILON);
    }
}
