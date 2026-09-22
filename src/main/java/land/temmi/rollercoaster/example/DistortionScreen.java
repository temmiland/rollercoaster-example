package land.temmi.rollercoaster.example;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import land.temmi.rollercoaster.actor.DirectionalSpriteAnimation;
import land.temmi.rollercoaster.actor.Facing;
import land.temmi.rollercoaster.actor.GridActor;
import land.temmi.rollercoaster.input.InputSource;
import land.temmi.rollercoaster.input.MoveIntent;
import land.temmi.rollercoaster.render.BillboardRenderer;
import land.temmi.rollercoaster.render.DirectionalShadowMap;
import land.temmi.rollercoaster.render.LightingEnvironment;
import land.temmi.rollercoaster.render.LowResTarget;
import land.temmi.rollercoaster.render.PixelCamera;
import land.temmi.rollercoaster.render.PointLightSource;
import land.temmi.rollercoaster.render.PlaneCamera;
import land.temmi.rollercoaster.world.GravityState;
import land.temmi.rollercoaster.world.MapPlane;
import land.temmi.rollercoaster.world.FoldedMapScene;
import land.temmi.rollercoaster.world.TileMap;

/** The cave, whose walking plane can turn. Drawn with the ordinary field renderer. */
final class DistortionScreen implements Disposable {
    private static final float TERRAIN_LEVEL_PIXEL_HEIGHT = 48f;
    private static final float SLAB_THICKNESS = 1.4f;
    private static final Color VOID_COLOR = new Color(0.10f, 0.16f, 0.55f, 1f);
    private static final Color CAVE_AMBIENT = new Color(0.86f, 0.88f, 1f, 1f);

    private final DistortionCave cave;
    private final LowResTarget target;
    private final PixelCamera camera;
    private final ModelBatch batch;
    private final SpriteBatch blit;
    private final BillboardRenderer sprite;
    private final DirectionalSpriteAnimation animation;
    private final GridActor player;
    private final InputSource input;
    private final LightingEnvironment lighting;
    private final DirectionalShadowMap shadowMap;
    private final FoldedMapScene scene;
    private final PlaneCamera planeCamera = new PlaneCamera();
    private final Array<ModelInstance> visible = new Array<>();
    private final Array<ModelInstance> noCasters = new Array<>();
    private final Vector3 direction = new Vector3();
    private final Vector3 up = new Vector3();
    private final Vector3 cameraNormal = new Vector3();
    private final Vector3 supportNormal = new Vector3();
    private final Vector3 fromNormal = new Vector3();
    private final Vector3 planeRight = new Vector3();
    private final Vector3 spriteRight = new Vector3();
    private final Vector3 spriteUp = new Vector3();
    private final Vector3 exitTile = new Vector3();
    private boolean finished;

    DistortionScreen(DistortionCave cave, LowResTarget target, PixelCamera camera, ModelBatch batch,
                     SpriteBatch blit, BillboardRenderer sprite, DirectionalSpriteAnimation animation,
                     GridActor player, InputSource input, LightingEnvironment lighting,
                     DirectionalShadowMap shadowMap) {
        this.cave = cave; this.target = target; this.camera = camera; this.batch = batch;
        this.blit = blit; this.sprite = sprite; this.animation = animation; this.player = player;
        this.input = input; this.lighting = lighting; this.shadowMap = shadowMap;
        scene = new FoldedMapScene(cave.map, new Material(), ExampleMap.getModelCatalog(), SLAB_THICKNESS);
        cave.exitTile(exitTile);

        planeCamera.setPitch(camera.getPitchDegrees());
        player.setMovementSpace(cave.map);
        Vector3 spawn = cave.spawnTile(new Vector3());
        player.setGridTile((int) spawn.x, (int) spawn.y, (int) spawn.z);
        applyPlane(true);
    }

    boolean isFinished() { return finished; }

    /** Plane the player currently stands on. */
    GravityState gravity() {
        MapPlane plane = cave.map.planeAt(player.getTileX(), player.getTileY(), player.getTileZ());
        return plane == null ? null : plane.gravity;
    }

    void render(float delta) {
        if (finished) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { finished = true; return; }

        MoveIntent move = input.pollMove();
        player.update(delta, move);
        if (player.didStep()) {
            applyPlane(false);
            previewCrossingAtEnteredTile(move);
        }
        updateSupportNormal();
        if (!player.isMoving() && onExitTile()) { finished = true; return; }

        planeCamera.update(delta);
        planeCamera.direction(direction);
        planeCamera.up(up);

        animation.setFacing(spriteFacing(gravity(), player.getFacing()));
        animation.setMoving(player.isMoving());
        animation.update(Math.max(0f, delta));
        sprite.setRegion(animation.getFrame());
        planeCamera.spriteBasis(supportNormal, spriteRight, spriteUp);
        sprite.setBasis(spriteRight, spriteUp);
        sprite.setDepthUp(supportNormal);
        sprite.setPosition(player.getPosition());

        camera.follow(player.getPosition(), TileMap.LEVEL_HEIGHT, TERRAIN_LEVEL_PIXEL_HEIGHT,
            direction, up, spriteUp);
        camera.snapToPixelGrid(target.getWidth(), target.getHeight());

        applyCaveLighting();

        target.begin();
        Gdx.gl.glClearColor(VOID_COLOR.r, VOID_COLOR.g, VOID_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera.camera);
        batch.render(scene.getVisibleInstances(camera.camera, visible));
        batch.render(sprite);
        batch.end();
        target.end();
        target.blitToScreen(blit);
    }

    /** Points the camera at the plane the player has entered. */
    private void applyPlane(boolean snap) {
        MapPlane plane = cave.map.planeAt(player.getTileX(), player.getTileY(), player.getTileZ());
        if (plane == null) return;
        plane.gravity.normal(cameraNormal);
        if (snap) supportNormal.set(cameraNormal);
        plane.gravity.right(planeRight);
        if (snap) planeCamera.snapTo(cameraNormal, planeRight);
        else planeCamera.blendTo(cameraNormal, planeRight);
    }

    /** The sprite turns over the arc of a real crossing, rather than when its view first previews it. */
    private void updateSupportNormal() {
        MapPlane destination = cave.map.planeAt(player.getTileX(), player.getTileY(), player.getTileZ());
        if (destination == null) return;
        MapPlane origin = cave.map.planeAt(player.getFromTileX(), player.getFromTileY(), player.getFromTileZ());
        if (player.isMoving() && origin != null && origin != destination) {
            origin.gravity.normal(fromNormal);
            destination.gravity.normal(supportNormal);
            supportNormal.set(fromNormal).lerp(supportNormal, player.getStepProgress()).nor();
            return;
        }
        destination.gravity.normal(supportNormal);
    }

    /**
     * Previews a jump only while entering its source tile in that jump's direction. The reverse
     * crossing at the landing tile therefore remains quiet until the player deliberately returns.
     */
    private void previewCrossingAtEnteredTile(MoveIntent move) {
        MapPlane destination = cave.map.crossingDestination(
            player.getTileX(), player.getTileY(), player.getTileZ(), move);
        if (destination == null) return;
        destination.gravity.normal(cameraNormal);
        destination.gravity.right(planeRight);
        planeCamera.blendTo(cameraNormal, planeRight);
    }

    /**
     * Directional frames remain tied to the character after its quarter-turn with a walking
     * plane. The west wall, for example, turns the north-facing image onto screen-left.
     */
    static Facing spriteFacing(GravityState gravity, Facing facing) {
        if (gravity == null || facing == null) return facing;
        switch (gravity) {
            case WEST_WALL: return turnLeft(facing);
            case EAST_WALL: return turnRight(facing);
            case CEILING: return turnAround(facing);
            default: return facing;
        }
    }

    private static Facing turnLeft(Facing facing) {
        switch (facing) {
            case NORTH: return Facing.WEST;
            case WEST: return Facing.SOUTH;
            case SOUTH: return Facing.EAST;
            default: return Facing.NORTH;
        }
    }

    private static Facing turnRight(Facing facing) {
        switch (facing) {
            case NORTH: return Facing.EAST;
            case EAST: return Facing.SOUTH;
            case SOUTH: return Facing.WEST;
            default: return Facing.NORTH;
        }
    }

    private static Facing turnAround(Facing facing) {
        switch (facing) {
            case NORTH: return Facing.SOUTH;
            case SOUTH: return Facing.NORTH;
            case EAST: return Facing.WEST;
            default: return Facing.EAST;
        }
    }

    private boolean onExitTile() {
        return player.getTileX() == (int) exitTile.x
            && player.getTileY() == (int) exitTile.y
            && player.getTileZ() == (int) exitTile.z;
    }

    /**
     * Flat cave light. The sun stays off so the shadow map reports itself unrendered instead of
     * projecting the overworld's last frame onto the slabs.
     */
    private void applyCaveLighting() {
        lighting.setAmbient(CAVE_AMBIENT, 0.95f);
        lighting.setSunIntensity(0f);
        for (PointLightSource light : lighting.getPointLights()) light.enabled = false;
        shadowMap.render(player.getPosition(), noCasters);
    }

    @Override public void dispose() {
        player.setMovementSpace(null);
        animation.setMoving(false);
        scene.dispose();
    }
}
