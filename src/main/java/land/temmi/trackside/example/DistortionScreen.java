package land.temmi.trackside.example;

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
import land.temmi.rollercoaster.actor.GridActor;
import land.temmi.rollercoaster.input.InputSource;
import land.temmi.rollercoaster.render.BillboardRenderer;
import land.temmi.rollercoaster.render.DirectionalShadowMap;
import land.temmi.rollercoaster.render.LightingEnvironment;
import land.temmi.rollercoaster.render.LowResTarget;
import land.temmi.rollercoaster.render.PixelCamera;
import land.temmi.rollercoaster.render.PointLightSource;
import land.temmi.rollercoaster.render.SurfaceCamera;
import land.temmi.rollercoaster.world.GravityState;
import land.temmi.rollercoaster.world.SurfacePlatform;
import land.temmi.rollercoaster.world.SurfaceRoomScene;
import land.temmi.rollercoaster.world.TileMap;

/** Cave room where the walking plane can turn. Drawn with the ordinary field renderer. */
final class DistortionScreen implements Disposable {
    private static final float TERRAIN_LEVEL_PIXEL_HEIGHT = 48f;
    private static final float SLAB_THICKNESS = 1.4f;
    private static final Color VOID_COLOR = new Color(0.10f, 0.16f, 0.55f, 1f);
    private static final Color CAVE_AMBIENT = new Color(0.86f, 0.88f, 1f, 1f);

    private final DistortionMap map;
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
    private final SurfaceRoomScene scene;
    private final SurfaceCamera surfaceCamera = new SurfaceCamera();
    private final Array<ModelInstance> visible = new Array<>();
    private final Array<ModelInstance> noCasters = new Array<>();
    private final Vector3 direction = new Vector3();
    private final Vector3 up = new Vector3();
    private final Vector3 normal = new Vector3();
    private final Vector3 planeRight = new Vector3();
    private final Vector3 spriteRight = new Vector3();
    private final Vector3 spriteUp = new Vector3();
    private final Vector3 exitTile = new Vector3();
    private boolean finished;

    DistortionScreen(DistortionMap map, LowResTarget target, PixelCamera camera, ModelBatch batch,
                     SpriteBatch blit, BillboardRenderer sprite, DirectionalSpriteAnimation animation,
                     GridActor player, InputSource input, LightingEnvironment lighting,
                     DirectionalShadowMap shadowMap) {
        this.map = map; this.target = target; this.camera = camera; this.batch = batch;
        this.blit = blit; this.sprite = sprite; this.animation = animation; this.player = player;
        this.input = input; this.lighting = lighting; this.shadowMap = shadowMap;
        scene = new SurfaceRoomScene(map.room, new Material(), ExampleMap.getModelCatalog(), SLAB_THICKNESS);
        map.exitTile(exitTile);

        surfaceCamera.setPitch(camera.getPitchDegrees());
        player.setMovementSpace(map.room);
        Vector3 spawn = map.spawnTile(new Vector3());
        player.setGridTile((int) spawn.x, (int) spawn.y, (int) spawn.z);
        applyPlane(true);
    }

    boolean isFinished() { return finished; }

    /** Plane the player currently stands on. */
    GravityState gravity() {
        SurfacePlatform platform = map.room.platformAt(player.getTileX(), player.getTileY(), player.getTileZ());
        return platform == null ? null : platform.gravity;
    }

    void render(float delta) {
        if (finished) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { finished = true; return; }

        player.update(delta, input.pollMove());
        if (player.didStep()) applyPlane(false);
        if (!player.isMoving() && onExitTile()) { finished = true; return; }

        surfaceCamera.update(delta);
        surfaceCamera.direction(direction);
        surfaceCamera.up(up);

        animation.setFacing(player.getFacing());
        animation.setMoving(player.isMoving());
        animation.update(Math.max(0f, delta));
        sprite.setRegion(animation.getFrame());
        surfaceCamera.spriteBasis(spriteRight, spriteUp);
        sprite.setBasis(spriteRight, spriteUp);
        sprite.setPosition(player.getPosition());

        camera.follow(player.getPosition(), TileMap.LEVEL_HEIGHT, TERRAIN_LEVEL_PIXEL_HEIGHT, direction, up);
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

    /** Points the camera at the plane the player now stands on. */
    private void applyPlane(boolean snap) {
        SurfacePlatform platform = map.room.platformAt(player.getTileX(), player.getTileY(), player.getTileZ());
        if (platform == null) return;
        platform.gravity.normal(normal);
        platform.gravity.right(planeRight);
        if (snap) surfaceCamera.snapTo(normal, planeRight);
        else surfaceCamera.blendTo(normal, planeRight);
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
