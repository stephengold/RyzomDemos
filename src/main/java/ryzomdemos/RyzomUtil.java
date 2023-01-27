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

import com.jme3.animation.AnimControl;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.export.binary.ByteUtils;
import com.jme3.scene.Spatial;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.math.MyArray;
import jme3utilities.math.noise.Generator;

/**
 * Utility methods to interact with assets exported from the Ryzom Asset
 * Repository by Alweth's RyzomConverter.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class RyzomUtil {
    // *************************************************************************
    // constants and loggers

    /**
     * status interval (in nanoseconds)
     */
    final private static int statusInterval = 1_000_000_000;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(RyzomUtil.class.getName());
    /**
     * prefix for asset paths of exported assets
     */
    final static String assetPathPrefix = "/ryzom-assets/export/";
    /**
     * filesystem path to the asset root
     */
    final static String assetRoot = "../RyzomConverter/assets";
    /**
     * filesystem path to the summary file
     */
    final private static String summaryPathname = "./ryzom-summary.bin";
    /**
     * all gender codes
     */
    final static String[] genderCodeArray = {"f", "m"};
    /**
     * all skeletal-group names
     */
    final static String[] groupNameArray = {"ca", "ge"};
    // *************************************************************************
    // fields

    /**
     * all known geometry assets for female characters, each array sorted
     * lexicographically
     */
    final private static EnumMap<BodyPart, String[]> knownFemaleAssets
            = new EnumMap<>(BodyPart.class);
    /**
     * all known geometry assets for male characters, each array sorted
     * lexicographically
     */
    final private static EnumMap<BodyPart, String[]> knownMaleAssets
            = new EnumMap<>(BodyPart.class);
    /**
     * pseudo-random generator
     */
    final static Generator generator = new Generator();
    /**
     * all known animation names: key = groupName + genderCode, each array
     * sorted lexicographically
     */
    final private static Map<String, String[]> knownAnimations
            = new TreeMap<>();
    /**
     * all known animation keywords: key = groupName + genderCode, each array
     * sorted lexicographically
     */
    final private static Map<String, String[]> knownKeywords
            = new TreeMap<>();
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether the named asset exists (among exported assets in the
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
     * Access the sorted array of known animation names for the specified gender
     * and skeletal group. The assets must have been previously loaded by
     * {@link #preloadAssets(com.jme3.asset.AssetManager)}.
     *
     * @param groupName "ca" or "ge"
     * @param genderCode "f" for female or "m" for male
     * @return the pre-existing array of names (not null, in lexicographic
     * order)
     */
    static String[] knownAnimations(String groupName, String genderCode) {
        String key = groupName + genderCode;
        String[] result = knownAnimations.get(key);

        assert result != null;
        return result;
    }

    /**
     * Access the sorted list of known geometry assets for the specified body
     * part and gender. The assets must have been previously loaded by
     * {@link #preloadAssets(com.jme3.asset.AssetManager)}.
     *
     * @param part (not null)
     * @param genderCode "f" for female or "m" for male
     * @return the internal array of asset names (not null, in lexicographic
     * order)
     */
    static String[] knownGeometries(BodyPart part, String genderCode) {
        EnumMap<BodyPart, String[]> map;
        if (genderCode.equals("m")) {
            map = knownMaleAssets;
        } else {
            assert genderCode.equals("f") : genderCode;
            map = knownFemaleAssets;
        }

        String[] result = map.get(part);

        assert MyArray.isSorted(result);
        assert result != null;
        return result;
    }

    /**
     * Access the sorted array of known keywords for the specified gender and
     * skeletal group. The assets must have been previously loaded by
     * {@link #preloadAssets(com.jme3.asset.AssetManager)}.
     *
     * @param groupName "ca" or "ge"
     * @param genderCode "f" for female or "m" for male
     * @return the pre-existing array of animation keywords (not null, in
     * lexicographic order)
     */
    static String[] knownKeywords(String groupName, String genderCode) {
        String mapKey = groupName + genderCode;
        String[] result = knownKeywords.get(mapKey);

        assert result != null;
        return result;
    }

    /**
     * Preload all assets in the export directory. Assign each geometries asset
     * to a list based on its body part and gender. Also build lists of
     * animation names and keywords for each skeletal group and gender.
     *
     * @param assetManager the assetManager to use (not null)
     */
    static void preloadAssets(AssetManager assetManager) {
        knownFemaleAssets.clear();
        knownMaleAssets.clear();
        knownAnimations.clear();

        String directoryPath = assetRoot + assetPathPrefix;
        File directory = new File(directoryPath);
        assert directory.isDirectory() : directory.getAbsolutePath();
        String[] fileNames = directory.list();
        int numFiles = fileNames.length;

        int progressCount = 0;
        long nextStatus = System.nanoTime();
        Map<String, Set<String>> tmpMap = new TreeMap<>();
        for (String fileName : fileNames) {
            if (fileName.matches("^(ca|fy|ge|ma|tr|zo).*$")) {
                // geometries asset
                BodyPart bodyPart = bodyPart(fileName, assetManager);
                String assetName = fileName.replace(".j3o", "");
                String genderCode = genderOfGeometryAsset(assetName);
                String key = genderCode + bodyPart;
                Set<String> names = tmpMap.get(key);
                if (names == null) {
                    names = new TreeSet<>();
                    tmpMap.put(key, names);
                }
                names.add(assetName);

            } else if (fileName.matches("^animations_.*$")) {
                // animations asset
                String[] animations = listAnimations(fileName, assetManager);
                String groupName = fileName.substring(11, 13);
                String genderCode = fileName.substring(16, 17);
                String key = groupName + genderCode;
                knownAnimations.put(key, animations);
            }

            ++progressCount;
            if (System.nanoTime() >= nextStatus) {
                printStatus(progressCount, numFiles);
                nextStatus = System.nanoTime() + statusInterval;
            }
        }
        printStatus(progressCount, numFiles);

        for (BodyPart part : BodyPart.values()) {
            String fKey = "f" + part;
            Set<String> fNames = tmpMap.get(fKey);
            String[] fArray = new String[fNames.size()];
            fNames.toArray(fArray);
            assert MyArray.isSorted(fArray);
            knownFemaleAssets.put(part, fArray);

            String mKey = "m" + part;
            Set<String> mNames = tmpMap.get(mKey);
            String[] mArray = new String[mNames.size()];
            mNames.toArray(mArray);
            assert MyArray.isSorted(mArray);
            knownMaleAssets.put(part, mArray);
        }

        populateKeywords();
    }

    /**
     * Attempt to quickly populate the lists of geometries, animation names, and
     * keywords by reading them from a well-known file.
     *
     * @return true if successful, otherwise false
     */
    static boolean readMaps() {
        knownFemaleAssets.clear();
        knownMaleAssets.clear();
        knownAnimations.clear();

        File file = new File(summaryPathname);
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException exception) {
            return false;
        }

        try {
            readMaps(inputStream);
        } catch (IOException exception) {
            try {
                inputStream.close();
            } catch (IOException ex) {
            }
            return false;
        }

        return true;
    }

    /**
     * Attempt to write the lists of geometries, animation names, and keywords
     * to a well-known file.
     *
     * @return true if successful, otherwise false
     */
    static boolean writeMaps() {
        File file = new File(summaryPathname);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException exception) {
            return false;
        }

        try {
            writeMaps(outputStream);
        } catch (IOException exception) {
            try {
                outputStream.close();
            } catch (IOException ex) {
            }
            file.delete();

            return false;
        }

        return true;
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
    private static BodyPart bodyPart(
            String fileName, AssetManager assetManager) {
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
     * @param assetName (not null, not empty)
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

    /**
     * Enumerate all animation names for an animation asset.
     *
     * @param fileName the filename of the asset (ending in ".j3o")
     * @param assetManager (not null)
     * @return a new vector of names in lexicographic order
     */
    private static String[] listAnimations(
            String fileName, AssetManager assetManager) {
        String genderCode = fileName.substring(16, 17);
        String groupName = fileName.substring(11, 13);
        assert String.format("animations_%s_ho%s.j3o",
                groupName, genderCode).equals(fileName);

        String assetPath = assetPathPrefix + fileName;
        ModelKey modelKey = new ModelKey(assetPath);
        Spatial loadedNode = assetManager.loadAsset(modelKey);
        AnimControl animControl
                = loadedNode.getControl(AnimControl.class);
        Collection<String> animationNames = animControl.getAnimationNames();

        int numAnimations = animationNames.size();
        String[] result = new String[numAnimations];
        animationNames.toArray(result);
        Arrays.sort(result);

        assert MyArray.isSorted(result);
        assert result != null;
        return result;
    }

    /**
     * Populate the arrays of known animation keywords.
     */
    private static void populateKeywords() {
        for (String groupName : groupNameArray) {
            for (String genderCode : genderCodeArray) {
                Set<String> keywordSet = new TreeSet<>();
                String[] nameArray = knownAnimations(groupName, genderCode);
                for (String name : nameArray) {
                    String[] words = name.split("_");
                    for (String word : words) {
                        // trim trailing digits
                        while (word.matches("^.+[0-9]$")) {
                            word = word.substring(0, word.length() - 1);
                        }
                        // exclude short words and ones that appear very often
                        if (word.length() >= 3 && !word.equals("end")
                                && !word.equals("hof") && !word.equals("hom")) {
                            keywordSet.add(word);
                        }
                    }
                }
                int numKeywords = keywordSet.size();
                String[] keywords = new String[numKeywords];
                keywordSet.toArray(keywords);
                assert MyArray.isSorted(keywords);

                String key = groupName + genderCode;
                knownKeywords.put(key, keywords);
            }
        }
    }

    /**
     * Print a status message to System.out while preloading assets.
     *
     * @param progressCount the number of files processed (&ge;0)
     * @param numFiles the number of files to be processed (&gt;0)
     */
    private static void printStatus(int progressCount, int numFiles) {
        float percentage = (100f * progressCount) / numFiles;
        String msg = String.format("%d of %d files analyzed (%.0f%%)",
                progressCount, numFiles, percentage);
        System.out.println(msg);
        System.out.flush();
    }

    /**
     * Populate the lists of geometries, animation names, and keywords by
     * reading them from the specified Stream.
     *
     * @param listStream the Stream to read (not null)
     */
    private static void readMaps(InputStream listsStream) throws IOException {
        for (BodyPart part : BodyPart.values()) {
            String[] fArray = readStringArray(listsStream);
            knownFemaleAssets.put(part, fArray);

            String[] mArray = readStringArray(listsStream);
            knownMaleAssets.put(part, mArray);
        }

        for (String groupName : RyzomUtil.groupNameArray) {
            for (String genderCode : RyzomUtil.genderCodeArray) {
                String key = groupName + genderCode;

                String[] animationNames = readStringArray(listsStream);
                knownAnimations.put(key, animationNames);

                String[] animationKeywords = readStringArray(listsStream);
                knownKeywords.put(key, animationKeywords);
            }
        }
    }

    /**
     * Read a String from the specified Stream.
     *
     * @param inputStream (not null)
     * @return the String that was read (may be null)
     * @throws IOException
     */
    private static String readString(InputStream inputStream)
            throws IOException {
        int lengthInBytes = ByteUtils.readInt(inputStream);

        String result;
        if (lengthInBytes == -1) {
            result = null;
        } else {
            byte[] byteArray = new byte[lengthInBytes];
            ByteUtils.readData(byteArray, lengthInBytes, inputStream);
            result = new String(byteArray);
        }

        return result;
    }

    /**
     * Read an array of strings from the specified Stream.
     *
     * @param inputStream (not null)
     * @return the Array that was read (not null, may contains nulls)
     * @throws IOException
     */
    private static String[] readStringArray(InputStream inputStream)
            throws IOException {
        int length = ByteUtils.readInt(inputStream);
        String[] result = new String[length];
        for (int arrayIndex = 0; arrayIndex < length; ++arrayIndex) {
            result[arrayIndex] = readString(inputStream);
        }

        return result;
    }

    /**
     * Write the lists of geometries, animation names, and keywords to the
     * specified Stream.
     *
     * @param listStream the Stream to write (not null)
     */
    private static void writeMaps(OutputStream listsStream) throws IOException {
        for (BodyPart part : BodyPart.values()) {
            String[] fArray = knownFemaleAssets.get(part);
            writeStringArray(listsStream, fArray);

            String[] mArray = knownMaleAssets.get(part);
            writeStringArray(listsStream, mArray);
        }

        for (String groupName : RyzomUtil.groupNameArray) {
            for (String genderCode : RyzomUtil.genderCodeArray) {
                String key = groupName + genderCode;

                String[] animationNames = knownAnimations.get(key);
                writeStringArray(listsStream, animationNames);

                String[] animationKeywords = knownKeywords.get(key);
                writeStringArray(listsStream, animationKeywords);
            }
        }
    }

    /**
     * Write a String to the specified Stream.
     *
     * @param inputStream (not null)
     * @return the String to write (may be null)
     * @throws IOException
     */
    private static void writeString(OutputStream outputStream, String string)
            throws IOException {
        if (string == null) {
            ByteUtils.writeInt(outputStream, -1);
        } else {
            byte[] byteArray = string.getBytes();
            int lengthInBytes = byteArray.length;
            ByteUtils.writeInt(outputStream, lengthInBytes);
            outputStream.write(byteArray);
        }
    }

    /**
     * Write an array of strings to the specified Stream.
     *
     * @param outputStream (not null)
     * @param array the Array to write (not null, may contains nulls,
     * unaffected)
     * @throws IOException
     */
    private static void writeStringArray(
            OutputStream outputStream, String[] array) throws IOException {
        int length = array.length;
        ByteUtils.writeInt(outputStream, length);
        for (int arrayIndex = 0; arrayIndex < length; ++arrayIndex) {
            writeString(outputStream, array[arrayIndex]);
        }
    }
}
