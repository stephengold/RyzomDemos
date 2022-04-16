/*
 Copyright (c) 2019-2022, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ryzomdemos;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.LoopMode;
import com.jme3.app.StatsAppState;
import com.jme3.asset.ModelKey;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.ui.AcorusDemo;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.Locators;
import jme3utilities.wes.AnimationEdit;

/**
 * Demonstrate character animation using assets exported from the Ryzom Asset
 * Repository by Alweth's RyzomConverter.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DodgerGame
        extends AcorusDemo
        implements AnimEventListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(DodgerGame.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = DodgerGame.class.getSimpleName();
    /**
     * animation names
     */
    final private static String deathAnimation = "fy_hom_co_a1md_mort";
    final private static String deathLoopAnimation = "fy_hom_co_a1md_mort_idle";
    final private static String idleAnimation = "fy_hom_co_a1md_idle_attente6";
    final private static String strafeLeftAnimation
            = "fy_hom_co_a1md_strafgauche";
    final private static String strafeRightAnimation
            = "fy_hom_co_a1md_strafdroit";
    // *************************************************************************
    // fields

    /**
     * channel for playing canned animations
     */
    private AnimChannel animChannel;
    /**
     * AppState to manage the arrow
     */
    private ArrowAppState arrowAppState;
    /**
     * overlay to display the score
     */
    private BitmapText scoreText;
    /**
     * count of arrows successfully dodged (&ge;0)
     */
    private int score;
    /**
     * single-sided grassy material for the platform
     */
    private Material grassMaterial;
    /**
     * main Node of the loaded character model
     */
    private Node characterNode;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the DodgerGame application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        DodgerGame application = new DodgerGame();
        Heart.parseAppArgs(application, arguments);

        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        /*
         * Customize the window's title bar.
         */
        String title = applicationName + " " + MyString.join(arguments);
        settings.setTitle(title);

        settings.setAudioRenderer(null);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        settings.setSamples(4); // anti-aliasing
        application.setSettings(settings);
        /*
         * Invoke the JME startup code,
         * which in turn invokes acorusInit().
         */
        application.start();
    }

    /**
     * Locate the character's pelvis.
     *
     * @return a new location vector (in world coordinates)
     */
    Vector3f characterLocation() {
        Bone bone = MySkeleton.findBone(characterNode, "pelvis");
        Vector3f offset = bone.getLocalPosition();
        Spatial transformer = MySpatial.findAnimatedGeometry(characterNode);
        Vector3f result = transformer.localToWorld(offset, null);

        return result;
    }

    /**
     * Play the character's death animation.
     */
    void die() {
        Vector3f offset = characterLocation();
        offset.y = 0f;
        characterNode.setLocalTranslation(offset);

        float blendTime = 0f;
        animChannel.setAnim(deathAnimation, blendTime);
    }

    /**
     * Increment the displayed score.
     */
    void incrementScore() {
        ++score;
        updateScoreText();
    }
    // *************************************************************************
    // AcorusDemo methods

    /**
     * Initialize this Application.
     */
    @Override
    public void acorusInit() {
        configureMaterials();

        viewPort.setBackgroundColor(ColorRGBA.Black);
        addLighting();
        /*
         * Initially hide the render-statistics overlay.
         */
        stateManager.getState(StatsAppState.class).toggleStats();

        addBox();

        Locators.registerFilesystem(RyzomUtil.assetRoot);
        attachCharacter();

        configureCamera();

        arrowAppState = new ArrowAppState();
        boolean success = stateManager.attach(arrowAppState);
        assert success;

        scoreText = new BitmapText(guiFont);
        float x = cam.getWidth() - 100f;
        float y = cam.getHeight() - 10f;
        scoreText.setLocalTranslation(x, y, 0f);
        guiNode.attachChild(scoreText);

        super.acorusInit();

        score = 0;
        updateScoreText();
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.unbind(KeyInput.KEY_C);
        dim.unbind(KeyInput.KEY_M);
        dim.unbind(KeyInput.KEY_Q);
        dim.unbind(KeyInput.KEY_S);
        dim.unbind(KeyInput.KEY_W);
        dim.unbind(KeyInput.KEY_Z);

        dim.bind("start", KeyInput.KEY_G); // TODO use bind(String, int...)
        dim.bind("start", KeyInput.KEY_Y);
        dim.bind("start", KeyInput.KEY_RETURN);

        dim.bind("strafe left", KeyInput.KEY_A);
        dim.bind("strafe left", KeyInput.KEY_LEFT);
        dim.bind("strafe left", KeyInput.KEY_NUMPAD4);

        dim.bind("strafe right", KeyInput.KEY_D);
        dim.bind("strafe right", KeyInput.KEY_RIGHT);
        dim.bind("strafe right", KeyInput.KEY_NUMPAD6);

        dim.bind("toggle help", KeyInput.KEY_H);
    }

    /**
     * Process an action that wasn't handled by the active InputMode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case "start":
                    reset();
                    arrowAppState.start();
                    return;

                case "strafe left":
                    startAnimation(strafeLeftAnimation);
                    return;
                case "strafe right":
                    startAnimation(strafeRightAnimation);
                    return;

                case "toggle help":
                    toggleHelp();
                    return;
            }
        }

        super.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // AnimEventListener methods

    /**
     * Callback invoked when an animation cycle completes.
     *
     * @param control the control that's playing the Animation
     * @param channel the channel that's playing the Animation
     * @param name the name of the Animation that completed
     */
    @Override
    public void onAnimCycleDone(AnimControl control, AnimChannel channel,
            String name) {
        float blendTime = 0f;
        switch (name) {
            case deathAnimation:
                channel.setAnim(deathLoopAnimation, blendTime);
                break;

            case deathLoopAnimation:
            case idleAnimation:
                // do nothing, animation will loop
                break;

            case "reverse " + strafeLeftAnimation:
            case "reverse " + strafeRightAnimation:
                channel.setAnim(idleAnimation, blendTime);
                break;

            case strafeLeftAnimation:
            case strafeRightAnimation:
                channel.setAnim("reverse " + name, blendTime);
                break;

            default:
                throw new IllegalStateException("Unknown animation: " + name);
        }
    }

    /**
     * Callback invoked when an Animation starts.
     *
     * @param control the control that's playing tha Animation
     * @param channel the channel that's playing the Animation
     * @param name the name of the Animation
     */
    @Override
    public void onAnimChange(AnimControl control, AnimChannel channel,
            String name) {
        // do nothing
    }
    // *************************************************************************
    // private methods

    /**
     * Attach a large static Box to the scene, to serve as a platform.
     */
    private void addBox() {
        float halfExtent = 2f; // mesh units
        Mesh mesh = new Box(halfExtent, halfExtent, halfExtent);
        Geometry geometry = new Geometry("box", mesh);
        rootNode.attachChild(geometry);

        geometry.move(0f, -halfExtent, 0f);
        geometry.setMaterial(grassMaterial);
        geometry.setShadowMode(RenderQueue.ShadowMode.Receive);
    }

    /**
     * Add lighting and shadows to the scene.
     */
    private void addLighting() {
        ColorRGBA ambientColor = new ColorRGBA(0.7f, 0.7f, 0.7f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f direction = new Vector3f(1f, -2f, -1f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction);
        rootNode.addLight(sun);

        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(assetManager, 4_096, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.7f);
        viewPort.addProcessor(dlsr);
    }

    /**
     * Load the model of the configured Character and attach it to the scene.
     */
    private void attachCharacter() {
        Character character = new Character();
        character.setGender("m");
        character.setGroup("ge");
        character.setGeometry(BodyPart.Arms, "fy_hom_armor01_armpad@01x_c1");
        character.setGeometry(BodyPart.Torso, "fy_hom_armor01_gilet@01x_c1");
        character.setGeometry(BodyPart.Face, "fy_hom_visage@-x-");
        character.setGeometry(BodyPart.Feet, "fy_hom_armor01_bottes@01x_c1");
        character.setGeometry(BodyPart.Head, "fy_hom_cheveux_basic01@01x-");
        character.setGeometry(BodyPart.Hands, "fy_hom_armor01_hand@01x_c1");
        character.setGeometry(BodyPart.Legs,
                "fy_hom_armor01_pantabottes@01x_c1");
        /*
         * Load the character node
         * (including a SkeletonControl and an AnimControl but no geometries)
         * and attach it to the scene graph.
         */
        ModelKey assetKey = character.makeAnimationAssetKey();
        characterNode = (Node) assetManager.loadAsset(assetKey);
        rootNode.attachChild(characterNode);
        /*
         * Add time-reversed versions of the strafe animations.
         */
        AnimControl animControl = characterNode.getControl(AnimControl.class);
        for (String animationName : new String[]{
            strafeLeftAnimation,
            strafeRightAnimation
        }) {
            Animation forward = animControl.getAnim(animationName);
            String reverseName = "reverse " + animationName;
            Animation reverse
                    = AnimationEdit.reverseAnimation(forward, reverseName);
            animControl.addAnim(reverse);
        }
        /*
         * Attach body parts to the character node.
         */
        for (BodyPart part : BodyPart.values()) {
            if (character.includes(part)) {
                assetKey = character.makeGeometryAssetKey(part);
                Spatial geometries = assetManager.loadAsset(assetKey);

                String groupName = character.groupName();
                if (groupName.equals("ge")) {
                    geometries = geometries.getUserData("ryzom_alternate");
                }
                characterNode.attachChild(geometries);
            }
        }
        /*
         * Disable culling and configure the model to cast shadows,
         * but not receive them.
         */
        List<Spatial> list
                = MySpatial.listSpatials(characterNode, Spatial.class, null);
        for (Spatial spatial : list) {
            spatial.setCullHint(Spatial.CullHint.Never);
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }

        animControl.addListener(this);
        /*
         * Create a channel to animate all bones in the Skeleton.
         */
        animChannel = animControl.createChannel();
        float blendTime = 0f;
        animChannel.setAnim(idleAnimation, blendTime);
        animChannel.setLoopMode(LoopMode.Loop);
    }

    /**
     * Configure the Camera during startup.
     */
    private void configureCamera() {
        flyCam.setEnabled(false);

        float near = 0.02f;
        float far = 20f;
        MyCamera.setNearFar(cam, near, far);

        cam.setLocation(new Vector3f(0f, 5f, -4f));
        cam.lookAt(new Vector3f(0f, -3f, 5f), Vector3f.UNIT_Y);
    }

    /**
     * Configure materials during startup.
     */
    private void configureMaterials() {
        Texture grass
                = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        grassMaterial = MyAsset.createShadedMaterial(assetManager, grass);
        grassMaterial.setBoolean("UseMaterialColors", true);
        grassMaterial.setColor("Diffuse", ColorRGBA.Gray);
        grassMaterial.setName("grass");
    }

    /**
     * Reset the game, but don't start yet.
     */
    private void reset() {
        characterNode.setLocalTranslation(Vector3f.ZERO);

        float blendTime = 0f;
        animChannel.setAnim(idleAnimation, blendTime);

        arrowAppState.reset();
        setSpeed(1f);

        score = 0;
        updateScoreText();
    }

    /**
     * If the channel is idle, start the named animation.
     */
    private void startAnimation(String animationName) {
        if (animChannel.getAnimationName().equals(idleAnimation)) {
            float blendTime = 0f;
            animChannel.setAnim(animationName, blendTime);
        }
    }

    private void updateScoreText() {
        String text = String.format("Score: %d", score);
        scoreText.setText(text);
    }
}
