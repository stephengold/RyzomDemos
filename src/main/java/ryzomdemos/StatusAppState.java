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

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.ModelKey;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.SimpleAppState;

/**
 * AppState to display the Status of the BuildCharacter application in an
 * overlay. The overlay consists of 11 status lines, one of which is selected
 * for editing. The overlay is located in the upper-left portion of the display.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class StatusAppState extends SimpleAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * index of the status line for the name of the animation that's playing
     */
    final static int animationStatusLine = 2;
    /**
     * index of the status line for the first body part
     */
    final static int firstPartStatusLine = 4;
    /**
     * index of the status line for gender
     */
    final static int genderStatusLine = 3;
    /**
     * index of the status line for the skeletal group
     */
    final static int groupStatusLine = 0;
    /**
     * index of the status line for the animation keyword
     */
    final static int keywordStatusLine = 1;
    /**
     * number of lines of text in the overlay
     */
    final static int numStatusLines
            = firstPartStatusLine + BodyPart.values().length;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(StatusAppState.class.getName());
    // *************************************************************************
    // fields

    /**
     * lines of text displayed in the upper-left corner of the display ([0] is
     * the top line)
     */
    final private BitmapText[] statusLines = new BitmapText[numStatusLines];
    /**
     * reference to the application instance
     */
    private BuildCharacter appInstance;
    /**
     * actual application state, or null for unknown
     */
    private Status actual = null;
    /**
     * configured application state, to be actualized during the next update()
     */
    final private Status config = new Status();
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized enabled state.
     */
    public StatusAppState() {
        super(true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Attach all configured body parts to the specified Node.
     *
     * @param parentNode where to attach (not null)
     */
    void attachBodyParts(Node parentNode) {
        Character character = config.getCharacter();
        character.adjustAssetsForGender();
        /*
         * Load the selected body-part geometries and attach them to the parent.
         */
        for (BodyPart part : BodyPart.values()) {
            if (character.includes(part)) {
                ModelKey assetKey = character.makeGeometryAssetKey(part);
                Spatial geometries = assetManager.loadAsset(assetKey);

                String groupName = character.groupName();
                if (groupName.equals("ge")) {
                    geometries = geometries.getUserData("ryzom_alternate");
                }
                parentNode.attachChild(geometries);
            }
        }
    }

    /**
     * Load the configured character node (including a SkeletonControl and an
     * AnimControl but no geometries) and attach it to the specified Node.
     *
     * @param parentNode where to attach (not null)
     * @return the new Node
     */
    Node attachCharacterNode(Node parentNode) {
        Character character = config.getCharacter();
        ModelKey assetKey = character.makeAnimationAssetKey();
        Node result = (Node) assetManager.loadAsset(assetKey);
        parentNode.attachChild(result);

        return result;
    }

    /**
     * Access the configured application state.
     *
     * @return the pre-existing instance (not null)
     */
    Status getConfig() {
        return config;
    }
    // *************************************************************************
    // ActionAppState methods

    /**
     * Clean up this AppState during the first update after it gets detached.
     * Should be invoked only by a subclass or by the AppStateManager.
     */
    @Override
    public void cleanup() {
        super.cleanup();
        /*
         * Remove the status lines from the guiNode.
         */
        for (int i = 0; i < numStatusLines; ++i) {
            statusLines[i].removeFromParent();
        }
    }

    /**
     * Initialize this AppState on the first update after it gets attached.
     *
     * @param sm application's state manager (not null)
     * @param app application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager sm, Application app) {
        super.initialize(sm, app);

        appInstance = (BuildCharacter) app;
        BitmapFont guiFont
                = assetManager.loadFont("Interface/Fonts/Default.fnt");
        RyzomUtil.preloadAssets(assetManager);
        /*
         * Add the status lines to the guiNode.
         */
        for (int i = 0; i < numStatusLines; ++i) {
            statusLines[i] = new BitmapText(guiFont, false);
            float y = cam.getHeight() - 20f * i;
            statusLines[i].setLocalTranslation(0f, y, 0f);
            guiNode.attachChild(statusLines[i]);
        }

        appInstance.updateCharacter();
    }

    /**
     * Callback to update this AppState prior to rendering. (Invoked once per
     * frame while the state is attached and enabled.)
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (actual != null && actual.equals(config)) {
            return; // unchanged
        }

        updateCharacter();
        appInstance.updateFeatureVisibility();

        List<String> nameList = config.knownAnimations();
        String animationName = config.animationName();
        int index = 1 + Collections.binarySearch(nameList, animationName);
        int count = nameList.size();
        String text = String.format("Animation #%d of %d: %s",
                index, count, animationName);
        updateStatusLine(animationStatusLine, text);

        appInstance.setAnim(animationName);

        String[] keywordArray = config.knownKeywords();
        String keyword = config.keyword();
        index = 1 + Arrays.binarySearch(keywordArray, keyword);
        count = keywordArray.length;
        text = String.format("Animation Keyword #%d of %d: %s",
                index, count, keyword);
        updateStatusLine(keywordStatusLine, text);

        if (actual == null) {
            actual = new Status();
        }
        actual.copy(config);
    }
    // *************************************************************************
    // private methods

    /**
     * Update everything that depends on the configured character body.
     */
    private void updateCharacter() {
        Character character = config.getCharacter();
        if (actual != null
                && actual.getCharacter().equals(character)
                && actual.selectedField() == config.selectedField()) {
            return; // unchanged
        }
        /*
         * Update body-dependent status lines.
         */
        String genderName = character.isFemale() ? "female" : "male";
        int index = character.isFemale() ? 1 : 2;
        String text = String.format("Gender #%d of 2: %s", index, genderName);
        updateStatusLine(genderStatusLine, text);

        String group = character.groupName();
        index = 1 + Arrays.binarySearch(RyzomUtil.groupNameArray, group);
        int count = RyzomUtil.groupNameArray.length;
        text = String.format("Skeletal Group #%d of %d: %s",
                index, count, group);
        updateStatusLine(groupStatusLine, text);

        String genderCode = character.genderCode();
        for (BodyPart part : BodyPart.values()) {
            List<String> known = RyzomUtil.knownGeometries(part, genderCode);
            String assetName = character.geometryName(part);
            if (assetName == null) {
                text = String.format("%s: <none>", part);
            } else {
                index = 1 + Collections.binarySearch(known, assetName);
                count = known.size();
                text = String.format("%s #%d of %d: %s",
                        part, index, count, assetName);
            }
            updateStatusLine(firstPartStatusLine + part.ordinal(), text);
        }

        if (actual == null || !actual.getCharacter().equals(character)) {
            appInstance.updateCharacter();
        }
    }

    /**
     * Update the indexed status line.
     */
    private void updateStatusLine(int lineIndex, String text) {
        BitmapText spatial = statusLines[lineIndex];
        int selectedLine = config.selectedField();

        if (lineIndex == selectedLine) {
            spatial.setColor(ColorRGBA.Yellow);
            spatial.setText("-> " + text);
        } else {
            spatial.setColor(ColorRGBA.White);
            spatial.setText(text);
        }
    }
}
