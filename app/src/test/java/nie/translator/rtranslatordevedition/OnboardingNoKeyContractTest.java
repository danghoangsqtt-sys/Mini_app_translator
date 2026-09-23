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

public class OnboardingNoKeyContractTest {
    @Test
    public void onboardingNoticeDoesNotRequireCloudBillingOrLegacyBranding() throws Exception {
        String englishNotice = readString("src/main/res/values/strings.xml", "description_notice");
        String italianNotice = readString("src/main/res/values-it/strings.xml", "description_notice");

        assertNoticeIsKeyless(englishNotice, "first year", "key");
        assertNoticeIsKeyless(italianNotice, "primo anno", "chiave");

        String layout = readFile("src/main/res/layout/fragment_notice.xml").toLowerCase(Locale.ROOT);
        assertFalse("The active onboarding must not show the former Cloud logo", layout.contains("google_cloud_logo"));
        assertFalse("The active onboarding must not show the legacy RTranslator wordmark", layout.contains("vertical_logo"));
    }

    @Test
    public void completedOnboardingLaunchesMainWithoutApiKeySetup() throws Exception {
        String userData = readFile("src/main/java/nie/translator/rtranslatordevedition/access/UserDataFragment.java");
        String accessActivity = readFile("src/main/java/nie/translator/rtranslatordevedition/access/AccessActivity.java");

        assertTrue(userData.contains("activity.startMainActivity()"));
        assertFalse(userData.contains("ApiManagementActivity"));
        assertTrue(accessActivity.contains("VoiceTranslationActivity.class"));
    }

    private static void assertNoticeIsKeyless(String notice, String firstYearPhrase,
                                              String keyWord) {
        String normalized = notice.toLowerCase(Locale.ROOT);
        assertTrue("The notice must explicitly allow a keyless onboarding", normalized.contains(keyWord));
        assertFalse(normalized.contains("300"));
        assertFalse(normalized.contains(firstYearPhrase));
        assertFalse(normalized.contains("rtranslator"));
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
