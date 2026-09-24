package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import org.junit.Test;

public class MlKitLanguageMapperTest {
    private final MlKitLanguageMapper mapper = new MlKitLanguageMapper(
            Arrays.asList("en", "it", "vi", "zh"));

    @Test public void mapsSupportedBcp47AndCustomLocale() {
        assertEquals("it", mapper.toSupportedTag("it"));
        assertEquals("vi", mapper.toSupportedTag(new CustomLocale("vi", "VN")));
    }

    @Test public void lowersCountryAndScriptSpecificLocalesToSupportedLanguage() {
        assertEquals("en", mapper.toSupportedTag("en-US"));
        assertEquals("zh", mapper.toSupportedTag("zh-Hant-TW"));
        assertEquals("it", mapper.toSupportedTag("it_IT"));
    }

    @Test public void englishUsesBuiltInTag() {
        assertTrue(mapper.isEnglish("en-GB"));
    }

    @Test public void supportedCollectionsAreImmutableAndDefensive() {
        List<String> tags = mapper.getSupportedTags();
        assertEquals(Arrays.asList("en", "it", "vi", "zh"), tags);
        try {
            tags.add("de");
            throw new AssertionError("Expected immutable tags");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
        List<CustomLocale> locales = mapper.getSupportedLocales(new CustomLocale(Locale.ENGLISH));
        try {
            locales.clear();
            throw new AssertionError("Expected immutable locales");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsUndeterminedLanguage() {
        mapper.toSupportedTag("und");
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsUnsupportedLanguage() {
        mapper.toSupportedTag("xx-ZZ");
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsBlankLanguage() {
        mapper.toSupportedTag("  ");
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsNullLocale() {
        mapper.toSupportedTag((CustomLocale) null);
    }
}
