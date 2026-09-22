package land.temmi.rollercoaster.example;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import land.temmi.rollercoaster.render.LowResTarget;
import land.temmi.rollercoaster.render.PixelCamera;
import land.temmi.rollercoaster.render.WorldShaderProvider;
import land.temmi.rollercoaster.render.BillboardQuad;
import land.temmi.rollercoaster.render.BillboardRenderer;
import land.temmi.rollercoaster.render.DayNightCycle;
import land.temmi.rollercoaster.render.DirectionalShadowMap;
import land.temmi.rollercoaster.render.LightingEnvironment;
import land.temmi.rollercoaster.asset.SpriteAtlas;
import land.temmi.rollercoaster.asset.SpriteDefinition;
import land.temmi.rollercoaster.asset.SpriteManifest;
import land.temmi.rollercoaster.world.LoadedMap;
import land.temmi.rollercoaster.world.MapEntity;
import land.temmi.rollercoaster.world.TerrainRules;
import land.temmi.rollercoaster.world.TileMap;
import land.temmi.rollercoaster.world.WorldScene;
import land.temmi.rollercoaster.actor.GridActor;
import land.temmi.rollercoaster.actor.DirectionalSpriteAnimation;
import land.temmi.rollercoaster.input.InputSource;
import land.temmi.rollercoaster.input.CombinedInput;
import land.temmi.rollercoaster.input.KeyboardInput;
import land.temmi.rollercoaster.input.TouchInput;
import land.temmi.rollercoaster.input.TouchpadRenderer;

public class ExampleGame extends ApplicationAdapter {

    private static final float TERRAIN_LEVEL_PIXEL_HEIGHT = 48f;

    private LowResTarget lowRes;
    private PixelCamera pixelCamera;
    private SpriteBatch blitBatch;
    private ShapeRenderer shapes;
    private final Matrix4 overlayProjection = new Matrix4();

    private ModelBatch modelBatch;
    private LightingEnvironment lighting;
    private DayNightCycle dayNightCycle;
    private DirectionalShadowMap shadowMap;
    private ExampleLighting exampleLighting;
    private final Array<ModelInstance> shadowCasters = new Array<>();
    private WorldScene worldScene;
    private final Array<ModelInstance> visibleInstances = new Array<>();
    private SpriteAtlas playerAtlas;
    private SpriteDefinition playerDefinition;
    private BillboardQuad billboardQuad;
    private BillboardRenderer playerSprite;
    private GridActor player;
    private InputSource input;
    private DirectionalSpriteAnimation playerAnimation;
    private float subjectWorldHeight;
    private final Vector3 subjectFootPosition = new Vector3(0f, 0f, 0f);
    private final Vector3 spriteRight = new Vector3();
    private DistortionScreen distortion;
    private final Vector3 caveTile = new Vector3();
    private int debugFrames;
    private TouchpadRenderer touchpadRenderer;

    @Override
    public void create() {
        lowRes = new LowResTarget();
        lowRes.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        pixelCamera = new PixelCamera();
        pixelCamera.resize(lowRes.getWidth(), lowRes.getHeight());

        blitBatch = new SpriteBatch();
        shapes = new ShapeRenderer();

        lighting = new LightingEnvironment();
        dayNightCycle = new DayNightCycle(lighting).setSecondsPerDay(90f).enterMap();
        shadowMap = new DirectionalShadowMap(lighting).setWorldSize(32f);
        modelBatch = new ModelBatch(new WorldShaderProvider(lighting, shadowMap));
        worldScene = ExampleMap.createScene();
        exampleLighting = new ExampleLighting(lighting, worldScene);
        LoadedMap map = worldScene.getMap();

        billboardQuad = new BillboardQuad();
        createPlayerSprite();
        player = new GridActor(map.tiles.getWidth(), map.tiles.getDepth(), 5f);
        MapEntity playerEntity = findEntity(map, "player");
        MapEntity caveEntity = findEntity(map, "cave");
        caveTile.set(caveEntity.x, 0f, caveEntity.z);
        player.setTileAccess(new TerrainRules(map.tiles));
        player.setTile(playerEntity.x, playerEntity.z);
        TouchInput touchInput = new TouchInput();
        input = new CombinedInput(new KeyboardInput(), touchInput);
        // Only mobile players touch the screen to move; drawing it on desktop would just be
        // visual noise over a keyboard-driven game.
        Application.ApplicationType platform = Gdx.app.getType();
        if (platform == Application.ApplicationType.Android || platform == Application.ApplicationType.iOS) {
            touchpadRenderer = new TouchpadRenderer(touchInput);
        }
    }

    private void createPlayerSprite() {
        SpriteManifest manifest = SpriteManifest.load(Gdx.files.classpath("sprites/player.json"));
        playerAtlas = new SpriteAtlas(manifest.atlas);
        playerDefinition = manifest.sprite("player");
        playerAnimation = new DirectionalSpriteAnimation(playerAtlas, playerDefinition);
        subjectWorldHeight = playerDefinition.worldHeight;
        playerSprite = new BillboardRenderer(billboardQuad, playerAtlas.getTexture(),
            playerAnimation.getFrame(), subjectWorldHeight);
        playerSprite.setBottomPadding(2f / 24f);
    }

    @Override
    public void resize(int width, int height) {
        lowRes.resize(width, height);
        pixelCamera.resize(lowRes.getWidth(), lowRes.getHeight());
    }

    @Override
    public void render() {
        render(Gdx.graphics.getDeltaTime());
    }

    /** Frame with an explicit time step, so a driver can advance the game without the window loop. */
    void render(float delta) {
        if (debugFrames++ < 3) {
            Gdx.app.log("ExampleGame", "render frame " + debugFrames + " "
                + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight()
                + " GL=" + Gdx.gl.glGetString(GL20.GL_VERSION));
        }
        if (distortion != null) {
            distortion.render(delta);
            if (touchpadRenderer != null) touchpadRenderer.render();
            if (distortion.isFinished()) {
                distortion.dispose();
                distortion = null;
                player.setTile((int) caveTile.x, (int) caveTile.z + 1);
            }
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            exampleLighting.toggle();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) dayNightCycle.cycleSituation();
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) dayNightCycle.setPaused(!dayNightCycle.isPaused());
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            dayNightCycle.setTimeOfDay((dayNightCycle.getTimeOfDay() + 1f) % 24f);
        }
        dayNightCycle.update(delta);
        exampleLighting.update(dayNightCycle.getSituation());
        player.update(delta, input.pollMove());
        if (!player.isMoving() && player.getTileX() == (int) caveTile.x
                && player.getTileZ() == (int) caveTile.z) {
            distortion = new DistortionScreen(new DistortionCave(), lowRes, pixelCamera,
                modelBatch, blitBatch, playerSprite, playerAnimation, player, input, lighting, shadowMap);
            distortion.render(0f);
            return;
        }
        playerAnimation.setFacing(player.getFacing());
        playerAnimation.setMoving(player.isMoving());
        playerAnimation.update(delta);
        playerSprite.setRegion(playerAnimation.getFrame());
        subjectFootPosition.set(player.getPosition());

        pixelCamera.follow(subjectFootPosition, TileMap.LEVEL_HEIGHT, TERRAIN_LEVEL_PIXEL_HEIGHT);
        pixelCamera.snapToPixelGrid(lowRes.getWidth(), lowRes.getHeight());

        playerSprite.setPosition(subjectFootPosition);
        playerSprite.setBasis(pixelCamera.right(spriteRight), pixelCamera.camera.up);
        playerSprite.setDepthUp(Vector3.Y);

        shadowCasters.clear();
        shadowCasters.addAll(worldScene.getInstances());
        shadowMap.render(subjectFootPosition, shadowCasters);

        lowRes.begin();
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.16f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(pixelCamera.camera);
        modelBatch.render(worldScene.getVisibleInstances(pixelCamera.camera, visibleInstances));
        modelBatch.render(playerSprite);
        modelBatch.end();

        drawPixelRuler();
        lowRes.end();

        lowRes.blitToScreen(blitBatch);
        if (touchpadRenderer != null) touchpadRenderer.render();
    }

    protected void setDemoTime(float hours) {
        dayNightCycle.setTimeOfDay(hours).enterMap().setPaused(true);
    }

    private static MapEntity findEntity(LoadedMap map, String type) {
        for (MapEntity entity : map.entities) {
            if (type.equals(entity.type)) return entity;
        }
        throw new IllegalArgumentException("Map has no " + type + " entity");
    }

    // Horizontal ticks every 10px (brighter every 50px) reveal the terrain-level scale.
    private void drawPixelRuler() {
        int width = lowRes.getWidth();
        int height = lowRes.getHeight();

        overlayProjection.setToOrtho2D(0, 0, width, height);
        shapes.setProjectionMatrix(overlayProjection);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (int y = 0; y <= height; y += 10) {
            shapes.setColor(y % 50 == 0 ? Color.WHITE : Color.GRAY);
            shapes.line(0, y, 6, y);
        }
        shapes.end();
    }

    @Override
    public void dispose() {
        if (distortion != null) distortion.dispose();
        if (touchpadRenderer != null) touchpadRenderer.dispose();
        lowRes.dispose();
        blitBatch.dispose();
        shapes.dispose();
        modelBatch.dispose();
        shadowMap.dispose();
        exampleLighting.dispose();
        worldScene.dispose();
        ExampleMap.dispose();
        playerSprite.dispose();
        billboardQuad.dispose();
        playerAtlas.dispose();
    }
}
