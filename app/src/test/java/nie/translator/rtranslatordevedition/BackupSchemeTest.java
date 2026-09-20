/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.junit.Assert.assertTrue;

public class BackupSchemeTest {
    private static final File BACKUP_SCHEME =
            new File("src/main/res/xml/backup_scheme.xml");

    @Test
    public void backupScheme_excludesCredentialStorageAndMetadata() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(BACKUP_SCHEME);

        assertTrue("App-private files must be excluded to cover encrypted and legacy credentials",
                containsExclusion(document, "file", "."));
        assertTrue("Default preferences contain apiKeyFileName and must be excluded",
                containsExclusion(document, "sharedpref",
                        "nie.translator.rtranslatordevedition_preferences.xml"));
        assertTrue("Cached access-token preferences must remain excluded",
                containsExclusion(document, "sharedpref", "AccessTokenLoader.xml"));
    }

    private static boolean containsExclusion(Document document, String domain, String path) {
        NodeList exclusions = document.getElementsByTagName("exclude");
        for (int index = 0; index < exclusions.getLength(); index++) {
            Element exclusion = (Element) exclusions.item(index);
            if (domain.equals(exclusion.getAttribute("domain"))
                    && path.equals(exclusion.getAttribute("path"))) {
                return true;
            }
        }
        return false;
    }
}
