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

package nie.translator.rtranslatordevedition;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StorageAccessManifestTest {
    private static final File MANIFEST = new File("src/main/AndroidManifest.xml");
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String TOOLS_NAMESPACE = "http://schemas.android.com/tools";

    @Test
    public void manifest_doesNotRequestLegacyExternalStorageAccess() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(MANIFEST);

        assertTrue(isRemovedFromMergedManifest(document,
                "android.permission.READ_EXTERNAL_STORAGE"));
        assertFalse(hasPermission(document, "android.permission.WRITE_EXTERNAL_STORAGE"));
        Element application = (Element) document.getElementsByTagName("application").item(0);
        assertFalse(application.hasAttributeNS(ANDROID_NAMESPACE, "requestLegacyExternalStorage"));
    }

    private static boolean hasPermission(Document document, String permission) {
        NodeList permissions = document.getElementsByTagName("uses-permission");
        for (int index = 0; index < permissions.getLength(); index++) {
            Element element = (Element) permissions.item(index);
            if (permission.equals(element.getAttributeNS(ANDROID_NAMESPACE, "name"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRemovedFromMergedManifest(Document document, String permission) {
        NodeList permissions = document.getElementsByTagName("uses-permission");
        for (int index = 0; index < permissions.getLength(); index++) {
            Element element = (Element) permissions.item(index);
            if (permission.equals(element.getAttributeNS(ANDROID_NAMESPACE, "name"))) {
                return "remove".equals(element.getAttributeNS(TOOLS_NAMESPACE, "node"));
            }
        }
        return true;
    }
}
