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

import com.jme3.asset.ModelKey;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Describe a character that's constructed out of assets imported from the Ryzom
 * Asset Repository by Alweth's RyzomConverter. TODO make Cloneable and Savable
 *
 * @author Stephen Gold sgold@sonic.net
 */
class Character {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(Character.class.getName());
    // *************************************************************************
    // fields

    /**
     * geometry assets used in the character (one asset per body part)
     */
    final private EnumMap<BodyPart, String> assets
            = new EnumMap<>(BodyPart.class);
    /**
     * 1-letter code for the character's gender ("f" for female or "m" for male)
     */
    private String gender = "m";
    /**
     * 2-letter code for the character's skeletal group ("ca" or "ge")
     */
    private String group = "ca";
    // *************************************************************************
    // new methods exposed

    /**
     * Update the geometry assets to match the character's gender. Parts without
     * an equivalent asset for that gender will be silently removed.
     */
    void adjustAssetsForGender() {
        for (BodyPart part : BodyPart.values()) {
            String assetName = geometryName(part);
            if (assetName != null) {
                assetName = adjustForGender(assetName);
                if (!RyzomUtil.assetExists(assetName)) {
                    assetName = null;
                }
                setGeometry(part, assetName);
            }
        }
    }

    /**
     * Adjust the specified animation/asset name to match the character's
     * gender.
     *
     * @param name the name of an animation or asset (not null)
     * @return an adjusted name (not null)
     */
    String adjustForGender(String name) {
        String result = name;
        boolean isFemale = isFemale();
        boolean isMale = isMale();
        /*
         * Make at most one substitution.
         */
        if (isFemale && result.contains("_hom_")) {
            result = result.replace("_hom_", "_hof_");
        } else if (isMale && result.contains("_hof_")) {
            result = result.replace("_hof_", "_hom_");
        } else if (isFemale && result.contains("_h_")) {
            result = result.replace("_h_", "_f_");
        } else if (isMale && result.contains("_f_")) {
            result = result.replace("_f_", "_h_");
        }

        return result;
    }

    /**
     * Read the character's gender.
     *
     * @return "f" for female or "m" for male
     */
    String genderCode() {
        return gender;
    }

    /**
     * Read the name of the geometry asset for the specified body part.
     *
     * @param part (not null)
     * @return the asset name (without ".j3o")
     */
    String geometryName(BodyPart part) {
        String result = assets.get(part);
        return result;
    }

    /**
     * Read the name of the character's skeletal group.
     *
     * @return "ca" or "ge"
     */
    String groupName() {
        return group;
    }

    /**
     * Test whether the character includes the specified body part.
     *
     * @param part (not null)
     * @return true if included, false if omitted
     */
    boolean includes(BodyPart part) {
        if (assets.get(part) == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the character is female.
     *
     * @return true if female, otherwise false
     */
    boolean isFemale() {
        boolean result = gender.equals("f");
        return result;
    }

    /**
     * Test whether the character is male.
     *
     * @return true if male, otherwise false
     */
    boolean isMale() {
        boolean result = gender.equals("m");
        return result;
    }

    /**
     * Generate a ModelKey for the character's animation asset.
     *
     * @return a new key
     */
    ModelKey makeAnimationAssetKey() {
        String fileName
                = String.format("animations_%s_ho%s.j3o", group, gender);
        String assetPath = RyzomUtil.assetPathPrefix + fileName;
        ModelKey key = new ModelKey(assetPath);

        return key;
    }

    /**
     * Generate a ModelKey for the specified body part.
     *
     * @param part (not null)
     * @return a new key
     */
    ModelKey makeGeometryAssetKey(BodyPart part) {
        String assetName = geometryName(part);
        String fileName = assetName + ".j3o";
        String assetPath = RyzomUtil.assetPathPrefix + fileName;
        ModelKey key = new ModelKey(assetPath);

        return key;
    }

    /**
     * Select the next known asset for the specified body part to match the
     * character's gender.
     *
     * @param part (not null)
     */
    void nextAssetFor(BodyPart part) {
        List<String> known = RyzomUtil.knownGeometries(part, gender);
        int numKnown = known.size();
        assert numKnown > 0 : numKnown;

        String selected = assets.get(part);
        String next;
        if (selected == null) {
            next = known.get(0);
        } else {
            int index = Collections.binarySearch(known, selected);
            if (index < 0) {
                next = known.get(0);
            } else if (index == numKnown - 1) {
                next = null;
            } else {
                next = known.get(index + 1);
            }
        }

        setGeometry(part, next);
    }

    /**
     * Select the previous geometry asset for the specified body part to match
     * the character's gender.
     *
     * @param part (not null)
     */
    void previousAssetFor(BodyPart part) {
        List<String> known = RyzomUtil.knownGeometries(part, gender);
        int numKnown = known.size();
        assert numKnown > 0 : numKnown;

        String selected = assets.get(part);
        String previous;
        if (selected == null) {
            previous = known.get(numKnown - 1);
        } else {
            int index = known.indexOf(selected);
            if (index == -1) {
                previous = known.get(numKnown - 1);
            } else if (index == 0) {
                previous = null;
            } else {
                previous = known.get(index - 1);
            }
        }

        setGeometry(part, previous);
    }

    /**
     * Pseudo-randomly alter the geometry asset for the specified body part.
     *
     * @param part (not null)
     */
    void randomize(BodyPart part) {
        List<String> known = RyzomUtil.knownGeometries(part, gender);
        String assetName = (String) RyzomUtil.generator.pick(known);
        setGeometry(part, assetName);
    }

    /**
     * Alter the character's gender.
     *
     * @param code "f" for female or "m" for male
     * @return this instance for chaining
     */
    Character setGender(String code) {
        assert code.equals("m") || code.equals("f") : code;
        gender = code;

        return this;
    }

    /**
     * Alter the geometry asset for the specified body part.
     *
     * @param part (not null)
     * @param assetName (may be null)
     * @return this instance for chaining
     */
    Character setGeometry(BodyPart part, String assetName) {
        if (assetName != null) {
            assert RyzomUtil.assetExists(assetName);
        }
        assets.put(part, assetName);

        return this;
    }

    /**
     * Alter the character's skeletal group.
     *
     * @param name "ca" or "ge"
     * @return this instance for chaining
     */
    Character setGroup(String name) {
        assert name.equals("ca") || name.equals("ge") : name;
        group = name;

        return this;
    }

    /**
     * Toggle the character's gender: masculine/feminine.
     */
    void toggleGender() {
        if (isFemale()) {
            setGender("m");
        } else {
            assert isMale();
            setGender("f");
        }
    }

    /**
     * Toggle the character's skeletal group.
     */
    void toggleGroup() {
        if (group.equals("ca")) {
            setGroup("ge");
        } else {
            assert group.equals("ge");
            setGroup("ca");
        }
    }
}
