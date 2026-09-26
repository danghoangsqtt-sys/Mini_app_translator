package nie.translator.rtranslatordevedition;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MigrationUxContractTest {
    @Test
    public void ordinarySettingsDoesNotConstructLegacyCloudTranslator() throws Exception {
        String source = readFile("src/main/java/nie/translator/rtranslatordevedition/settings/"
                + "SupportTtsQualityPreference.java");

        assertFalse(source.contains("cloud_apis.translation.Translator"));
        assertFalse(source.contains("new Translator("));
    }

    @Test
    public void missingTtsEventUsesHandlerKeyAndDoesNotFallThrough() throws Exception {
        String source = readFile("src/main/java/nie/translator/rtranslatordevedition/settings/"
                + "SettingsFragment.java");

        assertTrue(source.contains("bundle.putInt(\"type\", ON_MISSING_GOOGLE_TTS);"));
        assertFalse(source.contains("bundle.putInt(\"command\", ON_MISSING_GOOGLE_TTS);"));
        assertTrue(source.matches("(?s).*case ErrorCodes\\.MISSING_GOOGLE_TTS:\\s*"
                + "notifyMissingGoogleTTSDialog\\(\\);\\s*break;.*"));
    }

    @Test
    public void legacyCredentialCopyIsOptionalAndContainsNoObsoletePricing() throws Exception {
        for (String path : new String[]{"src/main/res/values/strings.xml",
                "src/main/res/values-it/strings.xml"}) {
            String title = readString(path, "title_activity_credit").toLowerCase(Locale.ROOT);
            String description = readString(path, "description_cost").toLowerCase(Locale.ROOT);
            String keyDescription = readString(path, "description_key").toLowerCase(Locale.ROOT);
            String combined = title + " " + description + " " + keyDescription;

            assertTrue(combined.contains("legacy"));
            assertTrue(combined.contains("optional") || combined.contains("opzional"));
            assertFalse(combined.contains("2.5"));
            assertFalse(combined.contains("2,5"));
            assertFalse(combined.contains("$300"));
            assertFalse(combined.contains("niedev/rtranslator"));
        }

        String englishPrivacy = readFile("src/main/res/values/strings.xml");
        String italianPrivacy = readFile("src/main/res/values-it/strings.xml");
        assertTrue(englishPrivacy.contains("danghoangsqtt-sys/Mini_app_translator"));
        assertTrue(italianPrivacy.contains("danghoangsqtt-sys/Mini_app_translator"));
        assertFalse(englishPrivacy.contains("niedev/RTranslator"));
        assertFalse(italianPrivacy.contains("niedev/RTranslator"));
    }

    @Test
    public void activeDocsDescribeOnDeviceDefaultInsteadOfCloudRequirement() throws Exception {
        String readme = readFile("../README.md").toLowerCase(Locale.ROOT);
        String architecture = readFile("../.viepilot/ARCHITECTURE.md").toLowerCase(Locale.ROOT);
        String projectContext = readFile("../.viepilot/PROJECT-CONTEXT.md")
                .toLowerCase(Locale.ROOT);

        assertTrue(readme.contains("android speechrecognizer"));
        assertTrue(readme.contains("ml kit translation"));
        assertFalse(readme.contains("một google cloud project đã bật"));
        assertFalse(readme.contains("## cấu hình google cloud"));
        assertTrue(architecture.contains("on-device default"));
        assertTrue(projectContext.contains("on-device default"));
    }

    private static String readString(String fileName, String stringName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(new File(fileName));
        NodeList strings = document.getElementsByTagName("string");
        for (int index = 0; index < strings.getLength(); index++) {
            Element string = (Element) strings.item(index);
            if (stringName.equals(string.getAttribute("name"))) {
                return string.getTextContent();
            }
        }
        throw new AssertionError("Missing string: " + stringName);
    }

    private static String readFile(String fileName) throws Exception {
        return new String(Files.readAllBytes(new File(fileName).toPath()), StandardCharsets.UTF_8);
    }
}
