/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nie.translator.rtranslatordevedition.tools.gui.graph;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Source-level reachability proof, not a runtime reproduction of the vendored throw branch.
 */
public class SecondScaleReachabilityProofTest {
    @Test
    public void applicationDoesNotCreateOrConfigureTheSecondScale() throws IOException {
        File productionRoot = new File("src/main/java");
        List<File> productionJavaFiles = collectJavaFiles(productionRoot);

        assertTrue("production source must be available for this reachability proof",
                productionRoot.isDirectory() && !productionJavaFiles.isEmpty());
        for (File file : productionJavaFiles) {
            String path = file.getPath().replace('\\', '/');
            String source = read(file);
            if (referencesSecondScale(source)) {
                assertFalse("a SecondScale-related path must not use reflection: " + path,
                        usesReflection(source));
            }
            if (!path.contains("/tools/gui/graph/")) {
                assertFalse("application code must not call GraphView.getSecondScale(): " + path,
                        source.contains("getSecondScale("));
                assertFalse("application code must not configure SecondScale: " + path,
                        source.contains("SecondScale"));
            }
        }
    }

    @Test
    public void secondScaleHasNoPublicPathToAutomaticYAxisBounds() throws IOException {
        String source = read(new File("src/main/java/nie/translator/rtranslatordevedition/"
                + "tools/gui/graph/SecondScale.java"));

        assertTrue(source.contains("private boolean mYAxisBoundsManual = true;"));
        assertTrue(source.contains("//public void setYAxisBoundsManual(boolean mYAxisBoundsManual)"));
        assertFalse(source.contains("\n    public void setYAxisBoundsManual("));
    }

    @Test
    public void layoutsOnlyInstantiateTheApplicationDateGraph() throws IOException {
        File layoutRoot = new File("src/main/res/layout");
        List<File> layouts = collectXmlFiles(layoutRoot);
        int graphLayoutCount = 0;

        for (File layout : layouts) {
            String source = read(layout);
            if (source.contains("GraphView") || source.contains("DateGraph")) {
                graphLayoutCount++;
                assertTrue(layout.getPath().replace('\\', '/').endsWith("component_credit_graph.xml"));
                assertTrue(source.contains("CustomDayGraphsPagerAdapter$DateGraph"));
            }
        }
        assertTrue("the app should retain one known GraphView layout", graphLayoutCount == 1);
    }

    private static List<File> collectJavaFiles(File directory) {
        return collectFilesWithSuffix(directory, ".java");
    }

    private static List<File> collectXmlFiles(File directory) {
        return collectFilesWithSuffix(directory, ".xml");
    }

    private static List<File> collectFilesWithSuffix(File directory, String suffix) {
        List<File> files = new ArrayList<File>();
        collectFilesWithSuffix(directory, suffix, files);
        return files;
    }

    private static void collectFilesWithSuffix(File directory, String suffix, List<File> files) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectFilesWithSuffix(child, suffix, files);
            } else if (child.getName().endsWith(suffix)) {
                files.add(child);
            }
        }
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
    }

    private static boolean referencesSecondScale(String source) {
        return source.contains("SecondScale") || source.contains("getSecondScale")
                || source.contains("mYAxisBoundsManual");
    }

    private static boolean usesReflection(String source) {
        return source.contains("Class.forName") || source.contains(".getDeclaredMethod(")
                || source.contains(".getMethod(") || source.contains("Method.invoke")
                || source.contains("setAccessible");
    }
}
