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

import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Spatial;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.math.noise.Generator;

/**
 * Encapsulate the properties of a character model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class Character {
    // *************************************************************************
    // constants and loggers

    /**
     * initial capacity for asset lists
     */
    final private static int initialCapacity = 120;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(Character.class.getName());
    /**
     * prefix for asset paths of converted assets
     */
    final static String assetPathPrefix = "/ryzom-assets/export/";
    /**
     * filesystem path to the asset root
     */
    final static String assetRoot = "../RyzomConverter/assets";
    // *************************************************************************
    // fields

    /**
     * all known geometry assets for female characters
     */
    final private static EnumMap<BodyPart, List<String>> knownFemaleAssets
            = new EnumMap<>(BodyPart.class);
    /**
     * all known geometry assets for male characters
     */
    final private static EnumMap<BodyPart, List<String>> knownMaleAssets
            = new EnumMap<>(BodyPart.class);
    /**
     * geometry assets used in the character (one asset per body part)
     */
    final private EnumMap<BodyPart, String> assets
            = new EnumMap<>(BodyPart.class);
    /**
     * pseudo-random generator
     */
    final private static Generator generator = new Generator();
    /**
     * 1-letter code for the character's gender ("f" or "m")
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
                if (!assetExists(assetName)) {
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
     * Test whether the named asset exists (among converted assets in the
     * filesystem). Works for both animation assets and geometry assets.
     *
     * @param assetName (not null)
     * @return true if found, otherwise false
     */
    static boolean assetExists(String assetName) {
        String fileName = assetName + ".j3o";
        String assetPath = assetPathPrefix + fileName;
        String filePath = assetRoot + assetPath;
        File file = new File(filePath);
        boolean result = file.exists();

        return result;
    }

    /**
     * Clear all lists of known geometry assets.
     */
    static void clearKnownGeometries() {
        for (BodyPart part : BodyPart.values()) {
            List<String> fList = new ArrayList<>(initialCapacity);
            knownFemaleAssets.put(part, fList);

            List<String> mList = new ArrayList<>(initialCapacity);
            knownMaleAssets.put(part, mList);
        }
    }

    /**
     * Read the character's gender.
     *
     * @return "f" or "m"
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
     * Access the list of known geometry assets for the specified body part and
     * gender.
     *
     * @param part (not null)
     * @param genderCode "f" or "m"
     * @return an reference to the internal list of asset names (not null)
     */
    static List<String> knownGeometries(BodyPart part,
            String genderCode) {
        EnumMap<BodyPart, List<String>> map;
        if (genderCode.equals("m")) {
            map = knownMaleAssets;
        } else {
            assert genderCode.equals("f") : genderCode;
            map = knownFemaleAssets;
        }

        List<String> result;
        result = map.get(part);
        if (result == null) { // lazy allocation of lists
            result = new ArrayList<>(initialCapacity);
            map.put(part, result);
        }

        assert result != null;
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
        String assetPath = assetPathPrefix + fileName;
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
        String assetPath = assetPathPrefix + fileName;
        ModelKey key = new ModelKey(assetPath);

        return key;
    }

    /**
     * Select the next known asset for the specified body part in the
     * character's gender.
     *
     * @param part (not null)
     */
    void nextAssetFor(BodyPart part) {
        List<String> known = knownGeometries(part, gender);
        int numKnown = known.size();
        assert numKnown > 0 : numKnown;

        String selected = assets.get(part);
        String next;
        if (selected == null) {
            next = known.get(0);
        } else {
            int index = known.indexOf(selected);
            if (index == -1) {
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
     * Preload all geometry assets in the specified directory and analyze them,
     * assigning each one to a list based on its body part and gender.
     *
     * @param assetManager the assetManager to use (not null)
     * @param directoryPath the filesystem path to the directory (not null)
     */
    static void preloadGeometries(AssetManager assetManager) {
        String directoryPath = assetRoot + assetPathPrefix;
        File directory = new File(directoryPath);
        assert directory.isDirectory();
        String[] fileNames = directory.list();
        int numFiles = fileNames.length;

        int progressCount = 0;
        for (String fileName : fileNames) {
            if (fileName.matches("^(ca|fy|ge|ma|tr|zo).*$")) {
                BodyPart bodyPart = bodyPart(fileName, assetManager);
                String assetName = fileName.replace(".j3o", "");
                String gender = genderOfGeometryAsset(assetName);
                List<String> known = knownGeometries(bodyPart, gender);
                known.add(assetName);
            }

            ++progressCount;
            if ((progressCount % 25) == 0) {
                float percentage = (100f * progressCount) / numFiles;
                String msg = String.format("%d of %d files analyzed (%.0f%%)",
                        progressCount, numFiles, percentage);
                System.out.printf("%s%n", msg);
            }
        }

        for (BodyPart part : BodyPart.values()) {
            List<String> fList = knownFemaleAssets.get(part);
            Collections.sort(fList);

            List<String> mList = knownMaleAssets.get(part);
            Collections.sort(mList);
        }
    }

    /**
     * Select the previous geometry asset for the specified body part in the
     * character's gender.
     *
     * @param part (not null)
     */
    void previousAssetFor(BodyPart part) {
        List<String> known = knownGeometries(part, gender);
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
     * Pesudo-randomly alter the specified body part.
     *
     * @param part (not null)
     */
    void randomize(BodyPart part) {
        List<String> known = knownGeometries(part, gender);
        String assetName = (String) generator.pick(known);
        setGeometry(part, assetName);
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
            assert assetExists(assetName);
        }
        assets.put(part, assetName);

        return this;
    }

    /**
     * Alter the character's gender.
     *
     * @param code "f" or "m"
     * @return this instance for chaining
     */
    Character setGender(String code) {
        assert code.equals("m") || code.equals("f") : code;
        gender = code;

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
    // private methods

    /**
     * Determine the appropriate body part for a geometry asset.
     *
     * @param fileName the filename of the asset (ending in ".j3o")
     * @param assetManager (not null)
     * @return an enum value (not null)
     */
    private static BodyPart bodyPart(String fileName,
            AssetManager assetManager) {
        assert fileName.endsWith(".j3o");

        String assetPath = assetPathPrefix + fileName;
        ModelKey key = new ModelKey(assetPath);
        Spatial geometries = assetManager.loadAsset(key);
        String partName = geometries.getUserData("ryzom_part");

        BodyPart result;
        switch (partName) {
            case "ARMOR_ARMPADS":
                result = BodyPart.Arms;
                break;
            case "ARMOR_BOOTS":
                result = BodyPart.Feet;
                break;
            case "ARMOR_CHEST":
                result = BodyPart.Torso;
                break;
            case "ARMOR_HANDS":
            case "GAUNTLET":
                result = BodyPart.Hands;
                break;
            case "ARMOR_PANTS":
                result = BodyPart.Legs;
                break;
            case "FACE":
                result = BodyPart.Face;
                break;
            case "ARMOR_HELMET":
            case "HAIR":
                result = BodyPart.Head;
                break;
            default:
                throw new RuntimeException("partName=" + partName);
        }

        assert result != null;
        return result;
    }

    /**
     * Infer the gender of a geometry asset from its name.
     *
     * @param assetName
     * @return "f" for female or "m" for male
     */
    private static String genderOfGeometryAsset(String assetName) {
        String result;
        String g3 = assetName.substring(3, 6);
        if (g3.equals("hof")) {
            result = "f";
        } else if (g3.equals("hom")) {
            result = "m";
        } else if (assetName.contains("_f_")) {
            result = "f";
        } else if (assetName.contains("_h_")) {
            result = "m";
        } else {
            String msg = "assetName=" + assetName;
            throw new RuntimeException(msg);
        }

        return result;
    }
}
