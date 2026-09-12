package land.temmi.trackside.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import land.temmi.rollercoaster.render.LightingEnvironment;
import land.temmi.rollercoaster.render.PointLightSource;
import land.temmi.rollercoaster.world.LoadedMap;
import land.temmi.rollercoaster.world.MapEntity;
import land.temmi.rollercoaster.world.WorldScene;
import land.temmi.rollercoaster.actor.GridActor;
import land.temmi.rollercoaster.actor.SpriteAnimation;
import land.temmi.rollercoaster.input.InputSource;
import land.temmi.rollercoaster.input.KeyboardInput;

public class ExampleGame extends ApplicationAdapter {

    private static final float SUBJECT_WORLD_HEIGHT = 1.8f;
    private static final float SUBJECT_PIXEL_HEIGHT = 48f;

    private LowResTarget lowRes;
    private PixelCamera pixelCamera;
    private SpriteBatch blitBatch;
    private BitmapFont debugFont;
    private ShapeRenderer shapes;
    private final Matrix4 overlayProjection = new Matrix4();

    private ModelBatch modelBatch;
    private LightingEnvironment lighting;
    private DayNightCycle dayNightCycle;
    private PointLightSource houseLight;
    private PointLightSource porchLight;
    private WorldScene worldScene;
    private final Array<ModelInstance> visibleInstances = new Array<>();
    private Texture spriteTexture;
    private BillboardQuad billboardQuad;
    private BillboardRenderer playerSprite;
    private GridActor player;
    private InputSource input;
    private SpriteAnimation playerAnimation;
    private final Vector3 subjectFootPosition = new Vector3(0f, 0f, 0f);
    private boolean debugVisible;

    @Override
    public void create() {
        lowRes = new LowResTarget();
        lowRes.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        pixelCamera = new PixelCamera();
        pixelCamera.resize(lowRes.getWidth(), lowRes.getHeight());

        blitBatch = new SpriteBatch();
        debugFont = new BitmapFont();
        shapes = new ShapeRenderer();

        lighting = new LightingEnvironment();
        dayNightCycle = new DayNightCycle(lighting).setSecondsPerDay(90f);
        houseLight = new PointLightSource(8f, 1.5f, 9f, new Color(1f, 0.45f, 0.12f, 1f), 2.2f, 5f);
        porchLight = new PointLightSource(7f, 3f, 8f, new Color(0.45f, 0.65f, 1f, 1f), 3f, 7f)
            .setSpot(new Vector3(0f, -1f, 0f), 18f, 42f);
        lighting.addPointLight(houseLight);
        lighting.addPointLight(porchLight);
        modelBatch = new ModelBatch(new WorldShaderProvider(lighting));
        worldScene = ExampleMap.createScene();
        LoadedMap map = worldScene.getMap();

        billboardQuad = new BillboardQuad();
        createPlayerSprite();
        player = new GridActor(map.tiles.getWidth(), map.tiles.getDepth(), 5f);
        MapEntity playerEntity = findPlayer(map);
        player.setTileAccess(new GridActor.TileAccess() {
            @Override public boolean canEnter(int x, int z) { return !map.tiles.isBlocked(x, z); }
            @Override public float heightAt(int x, int z) { return map.tiles.getHeight(x, z); }
        });
        player.setTile(playerEntity.x, playerEntity.z);
        input = new KeyboardInput();
    }

    private void createPlayerSprite() {
        Pixmap sprite = new Pixmap(32, 24, Pixmap.Format.RGBA8888);
        paintSprite(sprite, 0, false);
        paintSprite(sprite, 16, true);
        spriteTexture = new Texture(sprite);
        sprite.dispose();
        spriteTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion frameA = new TextureRegion(spriteTexture, 0, 0, 16, 24);
        TextureRegion frameB = new TextureRegion(spriteTexture, 16, 0, 16, 24);
        playerSprite = new BillboardRenderer(billboardQuad, spriteTexture, frameA, SUBJECT_WORLD_HEIGHT);
        playerSprite.setBottomPadding(3f / 24f);
        playerAnimation = new SpriteAnimation(0.14f, frameA, frameB);
    }

    @Override
    public void resize(int width, int height) {
        lowRes.resize(width, height);
        pixelCamera.resize(lowRes.getWidth(), lowRes.getHeight());
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) debugVisible = !debugVisible;
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            houseLight.enabled = !houseLight.enabled;
            porchLight.enabled = houseLight.enabled;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) dayNightCycle.setPaused(!dayNightCycle.isPaused());
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            dayNightCycle.setTimeOfDay((dayNightCycle.getTimeOfDay() + 1f) % 24f);
        }
        dayNightCycle.update(delta);
        player.update(delta, input.pollMove());
        playerAnimation.setPlaying(player.isMoving());
        playerAnimation.update(delta);
        playerSprite.setRegion(playerAnimation.getFrame());
        subjectFootPosition.set(player.getPosition());

        pixelCamera.follow(subjectFootPosition, SUBJECT_WORLD_HEIGHT, SUBJECT_PIXEL_HEIGHT);
        pixelCamera.snapToPixelGrid(lowRes.getWidth(), lowRes.getHeight());

        playerSprite.setPosition(subjectFootPosition);

        lowRes.begin();
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.16f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(pixelCamera.camera);
        modelBatch.render(worldScene.getVisibleInstances(pixelCamera.camera, visibleInstances));
        modelBatch.render(playerSprite);
        modelBatch.end();

        drawPixelRuler();
        if (debugVisible) drawDebugOverlay();
        lowRes.end();

        lowRes.blitToScreen(blitBatch);
    }

    // Pixmap rows run top-down and the billboard maps its top edge to the first row,
    // so the head is painted at the low row numbers and the legs at the high ones.
    private static void paintSprite(Pixmap sprite, int offsetX, boolean alternate) {
        sprite.setColor(0f, 0f, 0f, 0f);
        sprite.fillRectangle(offsetX, 0, 16, 24);
        sprite.setColor(0.95f, 0.55f, 0.15f, 1f);
        sprite.fillRectangle(offsetX + 5, 2, 6, 7);
        sprite.setColor(0.20f, 0.42f, 0.85f, 1f);
        sprite.fillRectangle(offsetX + 4, 9, 8, 7);
        sprite.setColor(0.15f, 0.20f, 0.32f, 1f);
        sprite.fillRectangle(offsetX + (alternate ? 3 : 4), 16, 3, 5);
        sprite.fillRectangle(offsetX + (alternate ? 10 : 9), 16, 3, 5);
    }

    private static MapEntity findPlayer(LoadedMap map) {
        for (MapEntity entity : map.entities) {
            if ("player".equals(entity.type)) return entity;
        }
        throw new IllegalArgumentException("Map has no player entity");
    }

    // Horizontal ticks every 10px (brighter every 50px), so a screenshot's rendered
    // subject height can be measured against SUBJECT_PIXEL_HEIGHT.
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

    private void drawDebugOverlay() {
        int width = lowRes.getWidth();
        int height = lowRes.getHeight();
        String text = "FPS " + Gdx.graphics.getFramesPerSecond()
            + "  GL " + Gdx.gl.glGetString(GL20.GL_VERSION)
            + "  depth " + lowRes.getDepthBits() + "b\n"
            + "FBO " + width + "x" + height
            + "  tile " + player.getTileX() + "," + player.getTileZ()
            + "  world " + subjectFootPosition.x + "," + subjectFootPosition.y
            + "," + subjectFootPosition.z + "\n"
            + "time " + String.format(java.util.Locale.ROOT, "%.1fh", dayNightCycle.getTimeOfDay())
            + "  lamps " + (houseLight.enabled ? "on" : "off") + "\n"
            + "camera fov " + pixelCamera.getFovDegrees()
            + " pitch " + pixelCamera.getPitchDegrees()
            + " distance " + pixelCamera.getDistance();
        blitBatch.setProjectionMatrix(overlayProjection.setToOrtho2D(0, 0, width, height));
        blitBatch.begin();
        debugFont.draw(blitBatch, text, 10f, height - 10f);
        blitBatch.end();
    }

    /**
     * A Pixmap-backed texture is unmanaged, so libGDX cannot restore it after a context loss.
     * The regions and the billboard material point at it, so they are rebuilt along with it.
     */
    @Override
    public void resume() {
        playerSprite.dispose();
        spriteTexture.dispose();
        createPlayerSprite();
    }

    @Override
    public void dispose() {
        lowRes.dispose();
        blitBatch.dispose();
        debugFont.dispose();
        shapes.dispose();
        modelBatch.dispose();
        worldScene.dispose();
        ExampleMap.dispose();
        playerSprite.dispose();
        billboardQuad.dispose();
        spriteTexture.dispose();
    }
}
