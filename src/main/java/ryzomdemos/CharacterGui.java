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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.SimpleAppState;
import jme3utilities.math.MyMath;

/**
 * AppState to display character parameters in the upper-left of the display.
 * <p>
 * The GUI consists of 10 status lines, one of which is selected for editing.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CharacterGui extends SimpleAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * index of the status line for the name of the animation that's playing
     */
    final private static int animationStatusLine = 0;
    /**
     * index of the status line for the first body part
     */
    final private static int firstPartStatusLine = 3;
    /**
     * index of the status line for gender
     */
    final private static int genderStatusLine = 1;
    /**
     * index of the status line for the skeletal group
     */
    final private static int groupStatusLine = 2;
    /**
     * number of lines of text in the GUI status
     */
    final private static int numStatusLines
            = firstPartStatusLine + BodyPart.values().length;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(CharacterGui.class.getName());
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
     * configured Character, including its gender and geometry assets
     */
    final private Character character = new Character();
    /**
     * index of the status line selected for editing
     */
    private int selectedLine = 0;
    /**
     * all known animation keywords: key = groupName + genderCode, each array
     * sorted lexicographically
     */
    final private static Map<String, String[]> knownKeywords
            = new TreeMap<>();
    /**
     * name of the Animation to play
     */
    private String animationName = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized enabled state.
     */
    public CharacterGui() {
        super(true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Advance the status-line selection by the specified amount.
     *
     * @param numLines the number of lines to move downward
     */
    void advanceSelectedLine(int numLines) {
        selectedLine = MyMath.modulo(selectedLine + numLines, numStatusLines);
    }

    /**
     * Attach all the selected body parts to the specified Node.
     *
     * @param parentNode (not null)
     */
    void attachBodyParts(Node parentNode) {
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
     * Load a character node (including a SkeletonControl and an AnimControl but
     * no geometries) and attach it to the specified Node.
     *
     * @param parentNode (not null)
     * @return the new Node
     */
    Node attachCharacterNode(Node parentNode) {
        ModelKey assetKey = character.makeAnimationAssetKey();
        Node result = (Node) assetManager.loadAsset(assetKey);
        parentNode.attachChild(result);

        return result;
    }

    /**
     * Determine the next value for the selected status line.
     */
    void nextValue() {
        switch (selectedLine) {
            case animationStatusLine:
                nextAnimation();
                break;
            case genderStatusLine:
                character.toggleGender();
                break;
            case groupStatusLine:
                character.toggleGroup();
                break;
            default:
                int ordinal = selectedLine - firstPartStatusLine;
                BodyPart part = BodyPart.values()[ordinal];
                character.nextAssetFor(part);
        }

        appInstance.updateCharacter();
    }

    /**
     * Determine the previous value for the selected status line.
     */
    void previousValue() {
        switch (selectedLine) {
            case animationStatusLine:
                previousAnimation();
                break;
            case genderStatusLine:
                character.toggleGender();
                break;
            case groupStatusLine:
                character.toggleGroup();
                break;
            default:
                int ordinal = selectedLine - firstPartStatusLine;
                BodyPart part = BodyPart.values()[ordinal];
                character.previousAssetFor(part);
        }

        appInstance.updateCharacter();
    }

    /**
     * Pseudo-randomly select body-part assets.
     */
    void randomizeAllParts() {
        for (BodyPart part : BodyPart.values()) {
            character.randomize(part);
        }

        appInstance.updateCharacter();
    }

    /**
     * Update the selected animation, to ensure it's playable.
     *
     * @return the updated name
     */
    String updateAnimationName() {
        if (!appInstance.animationExists(animationName)) {
            String adjName = character.adjustForGender(animationName);
            if (appInstance.animationExists(adjName)) {
                animationName = adjName;
            } else {
                String[] nameArray = appInstance.listAnimationsSorted();
                animationName = nameArray[0];
            }
        }

        return animationName;
    }
    // *************************************************************************
    // ActionAppState methods

    /**
     * Clean up this state during the first update after it gets detached.
     * Should be invoked only by a subclass or by the AppStateManager.
     */
    @Override
    public void cleanup() {
        super.cleanup();
        /*
         * Remove the status lines from the GUI.
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

        Character.preloadGeometries(assetManager);
        populateKeywords();

        character.setGender("m");
        character.setGroup("ca");
        character.setGeometry(BodyPart.Arms, "fy_hom_armor01_armpad");
        character.setGeometry(BodyPart.Torso, "fy_hom_armor01_gilet");
        character.setGeometry(BodyPart.Face, "fy_hom_visage");
        character.setGeometry(BodyPart.Feet, "fy_hom_armor01_bottes");
        character.setGeometry(BodyPart.Head, "fy_hom_cheveux_basic01");
        character.setGeometry(BodyPart.Hands, "fy_hom_armor01_hand");
        character.setGeometry(BodyPart.Legs, "fy_hom_armor01_pantabottes");

        animationName = "ca_hom_co_course";
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
     * Callback to update this state prior to rendering. (Invoked once per frame
     * while the state is attached and enabled.)
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        int numAnimations = appInstance.countAnimations();
        String text = String.format("Animation(%d): %s",
                numAnimations, animationName);
        updateStatusLine(animationStatusLine, text);

        String genderName = character.isFemale() ? "female" : "male";
        text = String.format("Gender(2): %s", genderName);
        updateStatusLine(genderStatusLine, text);

        String group = character.groupName();
        text = String.format("SkeletalGroup(2): %s", group);
        updateStatusLine(groupStatusLine, text);

        String gender = character.genderCode();
        for (BodyPart part : BodyPart.values()) {
            String assetName = character.geometryName(part);
            if (assetName == null) {
                assetName = "(none)";
            }
            List<String> known = Character.knownGeometries(part, gender);
            int numKnown = known.size();
            text = String.format("%s(%d): %s", part, numKnown, assetName);
            updateStatusLine(firstPartStatusLine + part.ordinal(), text);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Select and play the next animation.
     */
    private void nextAnimation() {
        String[] nameArray = appInstance.listAnimationsSorted();
        int lastIndex = nameArray.length - 1;
        int arrIndex = Arrays.binarySearch(nameArray, animationName);

        if (arrIndex < 0) {
            animationName = nameArray[0];
        } else if (arrIndex >= lastIndex) {
            animationName = nameArray[0];
        } else {
            animationName = nameArray[arrIndex + 1];
        }

        appInstance.setAnim(animationName);
    }

    /**
     * Populate the arrays of known animation keywords.
     */
    private void populateKeywords() {
        for (String groupName : Character.groupNameArray) {
            for (String genderCode : Character.genderCodeArray) {
                Set<String> keywordSet = new TreeSet<>();
                String[] nameArray
                        = Character.knownAnimations(groupName, genderCode);
                for (String name : nameArray) {
                    String[] words = name.split("_");
                    for (String word : words) {
                        // trim trailing digits
                        while (word.matches("^.+[0-9]$")) {
                            word = word.substring(0, word.length() - 1);
                        }

                        if (word.length() >= 3) {
                            keywordSet.add(word);
                        }
                    }
                }
                int numKeywords = keywordSet.size();
                String[] keywords = new String[numKeywords];
                keywordSet.toArray(keywords);
                assert SortUtil.isSorted(keywords);

                String key = groupName + genderCode;
                knownKeywords.put(key, keywords);
            }
        }
    }

    /**
     * Select and play the previous animation.
     */
    private void previousAnimation() {
        String[] nameArray = appInstance.listAnimationsSorted();
        int lastIndex = nameArray.length - 1;
        int arrIndex = Arrays.binarySearch(nameArray, animationName);

        if (arrIndex < 0) {
            animationName = nameArray[lastIndex];
        } else if (arrIndex == 0) {
            animationName = nameArray[lastIndex];
        } else {
            animationName = nameArray[arrIndex - 1];
        }

        appInstance.setAnim(animationName);
    }

    /**
     * Update the indexed status line.
     */
    private void updateStatusLine(int lineIndex, String text) {
        BitmapText spatial = statusLines[lineIndex];
        if (lineIndex == selectedLine) {
            spatial.setColor(ColorRGBA.Yellow);
            spatial.setText("-> " + text);
        } else {
            spatial.setColor(ColorRGBA.White);
            spatial.setText(text);
        }
    }
}
