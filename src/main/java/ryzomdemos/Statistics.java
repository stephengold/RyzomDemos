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
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryLoader;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.texture.plugins.AWTLoader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Console application to report statistics on assets exported from the Ryzom
 * Asset Repository by Alweth's RyzomConverter.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Statistics {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(Statistics.class.getName());
    // *************************************************************************
    // fields

    /**
     * locate, load, and cache assets
     */
    final private static AssetManager assetManager = new DesktopAssetManager();
    /**
     *
     */
    private static PrintStream out;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the Statistics application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        assetManager.registerLoader(AWTLoader.class, "png");
        assetManager.registerLoader(BinaryLoader.class, "j3o");
        assetManager.registerLoader(J3MLoader.class, "j3md");

        assetManager.registerLocator(RyzomUtil.assetRoot, FileLocator.class);
        assetManager.registerLocator(null, ClasspathLocator.class);

        boolean success = RyzomUtil.readMaps();
        if (!success) {
            RyzomUtil.preloadAssets(assetManager);
            RyzomUtil.writeMaps();
        }

        out = System.out;
        out.println();
        /*
         * Statistics on geometries assets.
         */
        long totalCombos = 0L;
        for (String genderCode : RyzomUtil.genderCodeArray) {
            long numCombosForGender = 1L;
            String genderName = genderCode.equals("f") ? "females" : "males";
            out.printf("For %s there are:%n", genderName);
            for (BodyPart part : BodyPart.values()) {
                String[] geometryNames
                        = RyzomUtil.knownGeometries(part, genderCode);
                int numGeometries = geometryNames.length;
                out.printf("%4d %s geometries assets%n", numGeometries, part);
                numCombosForGender *= numGeometries + 1; // +1 for no geometry
            }
            out.printf(" ... combining to form %d (%e) character bodies.%n",
                    numCombosForGender, (float) numCombosForGender);
            totalCombos += numCombosForGender;
        }
        out.printf("... for a total of %d (%e) character bodies.%n%n",
                totalCombos, (float) totalCombos);
        /*
         * Statistics on animation names and animation keywords.
         */
        Set<String> allAnimationNames = new TreeSet<>();
        Set<String> allAnimationKeywords = new TreeSet<>();
        for (String groupName : RyzomUtil.groupNameArray) {
            out.printf("The %s group provides:%n", groupName);

            for (String genderCode : RyzomUtil.genderCodeArray) {
                String[] keywordArray
                        = RyzomUtil.knownKeywords(groupName, genderCode);
                String[] nameArray
                        = RyzomUtil.knownAnimations(groupName, genderCode);

                String genderName
                        = genderCode.equals("f") ? "females" : "males";
                out.printf(
                        "%5d animations (and %d animation keywords) for %s%n",
                        nameArray.length, keywordArray.length, genderName);

                allAnimationNames.addAll(Arrays.asList(nameArray));
                allAnimationKeywords.addAll(Arrays.asList(keywordArray));
            }
        }
        out.printf("Overall, %d distinct names and %d distinct keywords.%n%n",
                allAnimationNames.size(), allAnimationKeywords.size());
        /*
         * Count how many animation keywords match each animation name.
         */
        for (String groupName : RyzomUtil.groupNameArray) {
            for (String genderCode : RyzomUtil.genderCodeArray) {
                String[] keywordArray
                        = RyzomUtil.knownKeywords(groupName, genderCode);
                String[] nameArray
                        = RyzomUtil.knownAnimations(groupName, genderCode);

                out.printf("For the %d %s_ho%s animations:%n", nameArray.length,
                        groupName, genderCode);
                reportNameStatistics(keywordArray, nameArray);
                reportKeywordStatistics(keywordArray, nameArray);
            }
        }
    }
    // *************************************************************************
    // private methods

    private static void reportNameStatistics(String[] keywordArray,
            String[] nameArray) {
        String mostMatchedName = null;
        int mostKeywordMatches = -1;
        Set<String> unmatchedNames = new TreeSet<>();

        for (String name : nameArray) {
            int numMatches = 0;
            for (String keyword : keywordArray) {
                if (name.contains("_" + keyword)) {
                    ++numMatches;
                }
            }

            if (numMatches == 0) {
                unmatchedNames.add(name);
            }
            if (numMatches > mostKeywordMatches) {
                mostMatchedName = name;
                mostKeywordMatches = numMatches;
            }
        }

        if (unmatchedNames.isEmpty()) {
            out.printf(" Every animation name matches at least one keyword.");
        } else {
            out.printf(" %d animation name(s) don't match any keyword: ",
                    unmatchedNames.size());
            for (String name : unmatchedNames) {
                out.printf(" %s", name);
            }
        }
        out.println();

        out.printf(" The most matched name is %s,", mostMatchedName);
        out.printf(" which matches %d keywords.%n", mostKeywordMatches);
    }

    private static void reportKeywordStatistics(String[] keywordArray,
            String[] nameArray) {
        String mostMatchedKeyword = null;
        int mostNameMatches = -1;

        for (String keyword : keywordArray) {
            int numMatches = 0;
            for (String name : nameArray) {
                if (name.contains("_" + keyword)) {
                    ++numMatches;
                }
            }

            if (numMatches > mostNameMatches) {
                mostMatchedKeyword = keyword;
                mostNameMatches = numMatches;
            }
        }

        out.printf(" The most matched keyword is %s,", mostMatchedKeyword);
        out.printf(" which matches %d names.%n", mostNameMatches);
    }
}
