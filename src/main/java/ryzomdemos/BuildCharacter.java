/*
 Copyright (c) 2019, Stephen Gold
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
import com.jme3.animation.Animation;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.Application;
import com.jme3.app.StatsAppState;
import com.jme3.font.Rectangle;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MySpatial;
import jme3utilities.debug.Dumper;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.CameraOrbitAppState;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.Locators;

/**
 * A JME3 application to demonstrate character construction using assets
 * imported from the Ryzom Asset Repository by Alweth's RyzomConverter.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BuildCharacter extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(BuildCharacter.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = BuildCharacter.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * channel for playing canned animations
     */
    private AnimChannel animChannel = null;
    /**
     * update GUI for character construction
     */
    private CharacterGui characterGui;
    /**
     * dump debugging information to System.out
     */
    final private Dumper dumper = new Dumper();
    /**
     * single-sided green material for the platform
     */
    private Material greenMaterial;
    /**
     * main node of the loaded character model
     */
    private Node characterNode;
    /**
     * node for displaying hotkey help/hints in the GUI
     */
    private Node helpNode;
    /**
     * skeleton visualizer for the loaded character model
     */
    private SkeletonVisualizer sv;
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether the named animation exists in the loaded model.
     *
     * @param name the animation name
     * @return true if found, otherwise false
     */
    boolean animationExists(String name) {
        AnimControl animControl = characterNode.getControl(AnimControl.class);
        Animation animation = animControl.getAnim(name);
        if (animation == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Count how many animations ther are for the loaded character.
     *
     * @return the count (&ge;0)
     */
    int countAnimations() {
        AnimControl animControl = characterNode.getControl(AnimControl.class);
        Collection<String> names = animControl.getAnimationNames();
        int result = names.size();

        return result;
    }

    /**
     * Enumerate all animations for the loaded character, in lexicographic
     * order.
     *
     * @return a new array of names (not null, not empty)
     */
    String[] listAnimationsSorted() {
        AnimControl animControl = characterNode.getControl(AnimControl.class);
        Collection<String> names = animControl.getAnimationNames();
        int numNames = names.size();
        assert numNames > 0 : numNames;

        String[] result = new String[numNames];
        names.toArray(result);
        Arrays.sort(result);

        return result;
    }

    /**
     * Main entry point for the BuildCharacter application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        /*
         * Mute the chatty loggers in certain packages.
         */
        Misc.setLoggingLevels(Level.WARNING);

        Application application = new BuildCharacter();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(applicationName);

        settings.setGammaCorrection(true);
        settings.setSamples(4); // anti-aliasing
        settings.setVSync(true);
        application.setSettings(settings);

        application.start();
    }

    /**
     * Play the named animation immediately (no blending).
     *
     * @param animationName (not null)
     */
    void setAnim(String animationName) {
        float blendTime = 0f;
        animChannel.setAnim(animationName, blendTime);
    }

    /**
     * Add the configured character to the scene, removing any pre-existing one.
     */
    void updateCharacter() {
        unloadCharacter();
        attachCharacter();
        /*
         * Update the selected animation and play it.
         */
        AnimControl animControl = characterNode.getControl(AnimControl.class);
        String animationName = characterGui.updateAnimationName();
        animChannel = animControl.createChannel();
        animChannel.setAnim(animationName);
        /*
         * Add a visualizer for the model's skeleton.
         */
        SkeletonControl skeletonControl
                = characterNode.getControl(SkeletonControl.class);
        sv = new SkeletonVisualizer(assetManager, skeletonControl);
        sv.setLineColor(ColorRGBA.Yellow);
        rootNode.addControl(sv);
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        configureCamera();
        configureDumper();
        configureMaterials();

        ColorRGBA bgColor = new ColorRGBA(0.2f, 0.2f, 1f, 1f);
        viewPort.setBackgroundColor(bgColor);
        addLighting();

        stateManager.getState(StatsAppState.class).toggleStats();
        addBox();

        Locators.registerFilesystem(Character.assetRoot);

        characterGui = new CharacterGui();
        stateManager.attach(characterGui);
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind("dump scenes", KeyInput.KEY_P);
        /*
         * CharacterGUI navigation
         */
        dim.bind("next statusLine", KeyInput.KEY_DOWN);
        dim.bind("next statusLine", KeyInput.KEY_NUMPAD2);
        dim.bind("next value", KeyInput.KEY_EQUALS);
        dim.bind("next value", KeyInput.KEY_NUMPAD6);
        dim.bind("next value", KeyInput.KEY_RIGHT);
        dim.bind("previous statusLine", KeyInput.KEY_NUMPAD8);
        dim.bind("previous statusLine", KeyInput.KEY_UP);
        dim.bind("previous value", KeyInput.KEY_LEFT);
        dim.bind("previous value", KeyInput.KEY_MINUS);
        dim.bind("previous value", KeyInput.KEY_NUMPAD4);

        dim.bind("randomize allParts", KeyInput.KEY_R);

        dim.bind("signal orbitLeft", KeyInput.KEY_A);
        dim.bind("signal orbitRight", KeyInput.KEY_D);

        dim.bind("toggle help", KeyInput.KEY_H);
        dim.bind("toggle meshes", KeyInput.KEY_M);
        dim.bind("toggle pause", KeyInput.KEY_PERIOD);
        dim.bind("toggle skeleton", KeyInput.KEY_V);
        /*
         * The help node can't be created until all hotkeys are bound.
         */
        addHelp();
    }

    /**
     * Process an action that wasn't handled by the active input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case "dump scenes":
                    dumper.dump(renderManager);
                    return;

                case "next statusLine":
                    characterGui.advanceSelectedLine(+1);
                    return;
                case "next value":
                    characterGui.nextValue();
                    return;

                case "previous statusLine":
                    characterGui.advanceSelectedLine(-1);
                    return;
                case "previous value":
                    characterGui.previousValue();
                    return;

                case "randomize allParts":
                    characterGui.randomizeAllParts();
                    return;

                case "toggle help":
                    toggleHelp();
                    return;
                case "toggle meshes":
                    toggleMeshes();
                    return;
                case "toggle pause":
                    togglePause();
                    return;
                case "toggle skeleton":
                    toggleSkeleton();
                    return;
            }
        }

        super.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // private methods

    /**
     * Attach a large static box to the scene, to serve as a platform.
     */
    private void addBox() {
        float halfExtent = 50f; // mesh units
        Mesh mesh = new Box(halfExtent, halfExtent, halfExtent);
        Geometry geometry = new Geometry("box", mesh);
        rootNode.attachChild(geometry);

        geometry.move(0f, -halfExtent, 0f);
        geometry.setMaterial(greenMaterial);
        geometry.setShadowMode(RenderQueue.ShadowMode.Receive);
    }

    /**
     * Attach a Node to display hotkey help/hints.
     */
    private void addHelp() {
        float x = 300f;
        float y = cam.getHeight() - 10f;
        float width = cam.getWidth() - 310f;
        float height = cam.getHeight() - 20f;
        Rectangle bounds = new Rectangle(x, y, width, height);

        InputMode dim = getDefaultInputMode();
        float space = 20f;
        helpNode = HelpUtils.buildNode(dim, bounds, guiFont, space);
        guiNode.attachChild(helpNode);
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
        dlsr.setShadowIntensity(0.5f);
        viewPort.addProcessor(dlsr);
    }

    /**
     * Load the configured character model and attach it to the scene.
     */
    private void attachCharacter() {
        /*
         * Load the character node
         * (including a SkeletonControl and an AnimControl but no geometries)
         * and attach it to the scene graph.
         */
        characterNode = characterGui.attachCharacterNode(rootNode);
        /*
         * Attach body parts to the character node.
         */
        characterGui.attachBodyParts(characterNode);
        /*
         * Configure the model to cast shadows, but not receive them.
         */
        List<Spatial> list
                = MySpatial.listSpatials(characterNode, Spatial.class, null);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
    }

    /**
     * Configure the camera during startup.
     */
    private void configureCamera() {
        float near = 0.02f;
        float far = 20f;
        MyCamera.setNearFar(cam, near, far);

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(2f);

        cam.setLocation(new Vector3f(-2.6f, 2.3f, 2.06f));
        cam.setRotation(new Quaternion(0.076f, 0.88238f, -0.152f, 0.4388f));

        getSignals().add(CameraInput.FLYCAM_STRAFELEFT);
        getSignals().add(CameraInput.FLYCAM_STRAFERIGHT);
        CameraOrbitAppState orbitState
                = new CameraOrbitAppState(cam, "orbitLeft", "orbitRight");
        stateManager.attach(orbitState);
    }

    /**
     * Configure the Dumper during startup.
     */
    private void configureDumper() {
        dumper.setDumpTransform(true);
        dumper.setDumpUser(true);
    }

    /**
     * Configure materials during startup.
     */
    private void configureMaterials() {
        ColorRGBA green = new ColorRGBA(0f, 0.12f, 0f, 1f);
        greenMaterial = MyAsset.createShadedMaterial(assetManager, green);
        greenMaterial.setName("green");
    }

    /**
     * Toggle visibility of the help node.
     */
    private void toggleHelp() {
        if (helpNode.getCullHint() == Spatial.CullHint.Always) {
            helpNode.setCullHint(Spatial.CullHint.Never);
        } else {
            helpNode.setCullHint(Spatial.CullHint.Always);
        }
    }

    /**
     * Toggle mesh rendering on/off.
     */
    private void toggleMeshes() {
        Spatial.CullHint hint = characterNode.getLocalCullHint();
        if (hint == Spatial.CullHint.Inherit
                || hint == Spatial.CullHint.Never) {
            hint = Spatial.CullHint.Always;
        } else if (hint == Spatial.CullHint.Always) {
            hint = Spatial.CullHint.Never;
        }
        characterNode.setCullHint(hint);
    }

    /**
     * Toggle the animation: paused/running.
     */
    private void togglePause() {
        float newSpeed = (speed > 1e-12f) ? 1e-12f : 1f;
        setSpeed(newSpeed);
    }

    /**
     * Toggle the skeleton visualizer on/off.
     */
    private void toggleSkeleton() {
        boolean enabled = sv.isEnabled();
        sv.setEnabled(!enabled);
    }

    /**
     * If the scene contains a character, remove it.
     */
    private void unloadCharacter() {
        if (characterNode != null) {
            rootNode.detachChild(characterNode);
            rootNode.removeControl(sv);
            characterNode = null;
        }
    }
}
