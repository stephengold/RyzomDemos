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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;

/**
 * Encapsulate the user-configurable state (MVC "model") of the BuildCharacter
 * application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class Status {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(Status.class.getName());
    // *************************************************************************
    // fields

    /**
     * character body, including its gender, group, and geometry assets
     */
    final private Character character = new Character();
    /**
     * whether to display each hideable feature
     */
    final private EnumMap<Feature, Boolean> visibilityFlags
            = new EnumMap<>(Feature.class);
    /**
     * index of the field being edited
     */
    private int selectedField = 0;
    /**
     * name of the Animation to play
     */
    private String animation = "ca_hom_co_course";
    /**
     * animation keyword
     */
    private String keyword = "course";
    // *************************************************************************
    // constructors

    /**
     * Instantiate the initial status.
     */
    Status() {
        visibilityFlags.put(Feature.Axes, false);
        visibilityFlags.put(Feature.Help, true);
        visibilityFlags.put(Feature.Meshes, true);
        visibilityFlags.put(Feature.Skeleton, false);

        character.setGender("m");
        character.setGroup("ca");
        character.setGeometry(BodyPart.Arms, "fy_hom_armor01_armpad");
        character.setGeometry(BodyPart.Torso, "fy_hom_armor01_gilet");
        character.setGeometry(BodyPart.Face, "fy_hom_visage");
        character.setGeometry(BodyPart.Feet, "fy_hom_armor01_bottes");
        character.setGeometry(BodyPart.Head, "fy_hom_cheveux_basic01");
        character.setGeometry(BodyPart.Hands, "fy_hom_armor01_hand");
        character.setGeometry(BodyPart.Legs, "fy_hom_armor01_pantabottes");
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Advance the status-field selection by the specified amount.
     *
     * @param numFields the number of fields to move downward
     */
    void advanceSelectedField(int numFields) {
        int sum = selectedField + numFields;
        selectedField = MyMath.modulo(sum, StatusAppState.numStatusLines);
    }

    /**
     * Read the name of the animation.
     *
     * @return the animation name (not null)
     */
    String animationName() {
        return animation;
    }

    /**
     * Make this instance equivalent to the specified instance.
     *
     * @param other (not null)
     */
    void copy(Status other) {
        character.copy(other.getCharacter());

        visibilityFlags.clear();
        visibilityFlags.putAll(other.visibilityFlags);

        selectedField = other.selectedField();
        animation = other.animationName();
        keyword = other.keyword();

        assert equals(other);
    }

    /**
     * Access the Character.
     *
     * @return the pre-existing instance (not null)
     */
    Character getCharacter() {
        return character;
    }

    /**
     * Test whether the specified feature should be visible.
     *
     * @param feature (not null)
     * @return true if visible, false if hidden
     */
    boolean isVisible(Feature feature) {
        boolean result = visibilityFlags.get(feature);
        return result;
    }

    /**
     * Read the animation keyword.
     *
     * @return the keyword (not null)
     */
    String keyword() {
        return keyword;
    }

    /**
     * Enumerate all known animations for the selected gender, skeletal group,
     * and animation keyword.
     *
     * @return a new sorted List of animation names (not null, may be empty)
     */
    List<String> knownAnimations() {
        String groupName = character.groupName();
        String genderCode = character.genderCode();
        String[] allNames = RyzomUtil.knownAnimations(groupName, genderCode);

        String substring;
        if (keyword == null) {
            substring = "";
        } else {
            substring = "_" + keyword;
        }

        int count = 0;
        for (String name : allNames) {
            if (name.contains(substring)) {
                ++count;
            }
        }

        List<String> result = new ArrayList<>(count);
        for (String name : allNames) {
            if (name.contains(substring)) {
                result.add(name);
            }
        }

        assert result != null;
        assert SortUtil.isSorted(result);
        return result;
    }

    /**
     * Access the array of known keywords for the configured gender and skeletal
     * group.
     *
     * @return the pre-existing array of animation keywords (not null)
     */
    String[] knownKeywords() {
        String groupName = character.groupName();
        String genderCode = character.genderCode();
        String[] result = RyzomUtil.knownKeywords(groupName, genderCode);

        return result;
    }

    /**
     * Select the next value for the selected field.
     */
    void nextValue() {
        switch (selectedField) {
            case StatusAppState.animationStatusLine:
                nextAnimation();
                break;
            case StatusAppState.genderStatusLine:
                character.toggleGender();
                character.adjustAssetsForGender();
                updateKeyword();
                updateAnimation();
                break;
            case StatusAppState.groupStatusLine:
                character.toggleGroup();
                updateKeyword();
                updateAnimation();
                break;
            case StatusAppState.keywordStatusLine:
                nextKeyword();
                break;
            default:
                int ordinal = selectedField - StatusAppState.firstPartStatusLine;
                BodyPart part = BodyPart.values()[ordinal];
                character.nextAssetFor(part);
        }
    }

    /**
     * Select the previous value for the selected field.
     */
    void previousValue() {
        switch (selectedField) {
            case StatusAppState.animationStatusLine:
                previousAnimation();
                break;
            case StatusAppState.genderStatusLine:
                character.toggleGender();
                character.adjustAssetsForGender();
                updateKeyword();
                updateAnimation();
                break;
            case StatusAppState.groupStatusLine:
                character.toggleGroup();
                updateKeyword();
                updateAnimation();
                break;
            case StatusAppState.keywordStatusLine:
                previousKeyword();
                break;
            default:
                int ordinal = selectedField - StatusAppState.firstPartStatusLine;
                BodyPart part = BodyPart.values()[ordinal];
                character.previousAssetFor(part);
        }
    }

    /**
     * Pseudo-randomly configure geometry assets.
     */
    void randomizeAllParts() {
        for (BodyPart part : BodyPart.values()) {
            character.randomize(part);
        }
    }

    /**
     * Configure a pseudo-random value for the selected field.
     */
    void randomizeValue() {
        switch (selectedField) {
            case StatusAppState.animationStatusLine:
                randomizeAnimation();
                break;
            case StatusAppState.genderStatusLine:
                character.randomizeGender();
                character.adjustAssetsForGender();
                updateKeyword();
                updateAnimation();
                break;
            case StatusAppState.groupStatusLine:
                character.randomizeGroup();
                updateKeyword();
                updateAnimation();
                break;
            case StatusAppState.keywordStatusLine:
                randomizeKeyword();
                break;
            default:
                int ordinal = selectedField - StatusAppState.firstPartStatusLine;
                BodyPart part = BodyPart.values()[ordinal];
                character.randomize(part);
        }
    }

    /**
     * Read the index of the field selected for editing.
     *
     * @return the index (&ge;0)
     */
    int selectedField() {
        return selectedField;
    }

    /**
     * Toggle visibility of the specified feature.
     *
     * @param feature (not null)
     */
    void toggleVisibility(Feature feature) {
        boolean visible = visibilityFlags.get(feature);
        visibilityFlags.put(feature, !visible);
    }
    // *************************************************************************
    // Object methods

    /**
     * Test for equivalency with another instance.
     *
     * @param otherObject (may be null, unaffected)
     * @return true if the references are equivalent, otherwise false
     */
    @Override
    public boolean equals(Object otherObject) {
        boolean result;
        if (this == otherObject) {
            result = true;

        } else if (otherObject instanceof Status) {
            Status other = (Status) otherObject;
            result = other.getCharacter().equals(character)
                    && other.selectedField() == selectedField
                    && other.animationName().equals(animation)
                    && other.keyword().equals(keyword)
                    && other.visibilityFlags.equals(visibilityFlags);

        } else {
            result = false;
        }

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Select the next animation.
     */
    private void nextAnimation() {
        List<String> names = knownAnimations();
        int lastIndex = names.size() - 1;
        int arrayIndex = Collections.binarySearch(names, animation);

        if (arrayIndex < 0) {
            animation = names.get(0);
        } else if (arrayIndex >= lastIndex) {
            animation = names.get(0);
        } else {
            animation = names.get(arrayIndex + 1);
        }
    }

    /**
     * Select the next animation keyword and update the animation accordingly.
     */
    private void nextKeyword() {
        String[] keywordArray = knownKeywords();
        int lastIndex = keywordArray.length - 1;
        int arrIndex = Arrays.binarySearch(keywordArray, keyword);

        if (arrIndex < 0) {
            keyword = keywordArray[0];
        } else if (arrIndex >= lastIndex) {
            keyword = keywordArray[0];
        } else {
            keyword = keywordArray[arrIndex + 1];
        }

        updateAnimation();
    }

    /**
     * Select the previous animation.
     */
    private void previousAnimation() {
        List<String> names = knownAnimations();
        int lastIndex = names.size() - 1;
        int listIndex = Collections.binarySearch(names, animation);

        if (listIndex < 0) {
            animation = names.get(lastIndex);
        } else if (listIndex == 0) {
            animation = names.get(lastIndex);
        } else {
            animation = names.get(listIndex - 1);
        }
    }

    /**
     * Select the previous animation keyword and update the animation
     * accordingly.
     */
    private void previousKeyword() {
        String[] keywordArray = knownKeywords();
        int lastIndex = keywordArray.length - 1;
        int arrayIndex = Arrays.binarySearch(keywordArray, keyword);

        if (arrayIndex < 0) {
            keyword = keywordArray[0];
        } else if (arrayIndex == 0) {
            keyword = keywordArray[lastIndex];
        } else {
            keyword = keywordArray[arrayIndex - 1];
        }

        updateAnimation();
    }

    /**
     * Pseudo-randomly select an Animation for the selected gender, skeletal
     * group, and animation keyword.
     */
    private void randomizeAnimation() {
        List<String> known = knownAnimations();
        animation = (String) RyzomUtil.generator.pick(known);
    }

    /**
     * Pseudo-randomly select an animation keyword for the selected gender and
     * skeletal group.
     */
    private void randomizeKeyword() {
        String[] known = knownKeywords();
        keyword = (String) RyzomUtil.generator.pick(known);
        updateAnimation();
    }

    /**
     * Update the selected animation to ensure it exists for the selected group
     * and gender and also matches the selected keyword, if any.
     *
     * @return the updated name (not null)
     */
    private void updateAnimation() {
        List<String> nameList = knownAnimations();
        if (Collections.binarySearch(nameList, animation) < 0) {
            String adjName = character.adjustForGender(animation);
            if (Collections.binarySearch(nameList, adjName) >= 0) {
                animation = adjName;
            } else {
                animation = nameList.get(0);
            }
        }
    }

    /**
     * Update the selected animation keyword to ensure it exists for the
     * selected group and gender.
     */
    private void updateKeyword() {
        String[] keywordArray = knownKeywords();
        if (Arrays.binarySearch(keywordArray, keyword) < 0) {
            keyword = keywordArray[0];
        }
    }
}
