/*
 Copyright (c) 2019-2023, Stephen Gold
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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;

/**
 * Describe a character body that's constructed out of assets exported from the
 * Ryzom Asset Repository by Alweth's RyzomConverter. TODO make Savable
 *
 * @author Stephen Gold sgold@sonic.net
 */
class Character implements Cloneable {
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
     * geometry assets used in the character (one asset per body part, values
     * may be null)
     */
    private EnumMap<BodyPart, String> assets = new EnumMap<>(BodyPart.class);
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

        // Make at most one substitution.
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
     * Advance the asset selection for the specified body part by the specified
     * amount.
     *
     * @param part (not null)
     * @param amount the number of assets to advance, including null
     */
    void advanceAssetFor(BodyPart part, int amount) {
        String[] known = RyzomUtil.knownGeometries(part, gender);
        String selected = assets.get(part);

        int index;
        if (selected == null) {
            index = -1;
        } else {
            index = Arrays.binarySearch(known, selected);
            if (index < 0) {
                index = -1;
            }
        }
        int numKnown = known.length;
        assert numKnown > 0 : numKnown;
        index = MyMath.modulo(index + amount, numKnown + 1);
        if (index == numKnown) {
            selected = null;
        } else {
            selected = known[index];
        }

        setGeometry(part, selected);
    }

    /**
     * Make this instance equivalent to the specified instance.
     *
     * @param other (not null)
     */
    void copy(Character other) {
        assets.clear();
        assets.putAll(other.assets);

        gender = other.genderCode();
        group = other.groupName();
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
     * Pseudo-randomly alter the geometry asset for the specified body part.
     *
     * @param part (not null)
     */
    void randomize(BodyPart part) {
        String[] known = RyzomUtil.knownGeometries(part, gender);
        String assetName = (String) RyzomUtil.generator.pick(known);
        setGeometry(part, assetName);
    }

    /**
     * Pseudo-randomly alter the character's gender.
     */
    void randomizeGender() {
        Object picked = RyzomUtil.generator.pick(RyzomUtil.genderCodeArray);
        setGender((String) picked);
    }

    /**
     * Pseudo-randomly alter the character's skeletal group.
     */
    void randomizeGroup() {
        Object picked = RyzomUtil.generator.pick(RyzomUtil.groupNameArray);
        setGroup((String) picked);
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
    // *************************************************************************
    // Object methods

    /**
     * Create a copy of this instance.
     *
     * @return a new instance, equivalent to this one
     * @throws CloneNotSupportedException if the superclass isn't cloneable
     */
    @Override
    public Character clone() throws CloneNotSupportedException {
        Character clone = (Character) super.clone();
        assets = new EnumMap<>(assets);

        return clone;
    }

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

        } else if (otherObject instanceof Character) {
            Character other = (Character) otherObject;
            result = other.genderCode().equals(gender)
                    && other.groupName().equals(group)
                    && other.assets.equals(assets);

        } else {
            result = false;
        }

        return result;
    }
}
