package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import com.google.mlkit.nl.translate.TranslateLanguage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import nie.translator.rtranslatordevedition.tools.CustomLocale;

/** Normalizes BCP-47 locales to the language tags explicitly supported by ML Kit Translation. */
public final class MlKitLanguageMapper {
    private static final String UNDETERMINED = "und";
    private final Set<String> supportedTags;

    public MlKitLanguageMapper() {
        this(TranslateLanguage.getAllLanguages());
    }

    MlKitLanguageMapper(Collection<String> supportedTags) {
        Objects.requireNonNull(supportedTags, "supportedTags");
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : supportedTags) {
            if (tag != null && !tag.trim().isEmpty()) {
                normalized.add(tag.trim().toLowerCase(Locale.ROOT));
            }
        }
        this.supportedTags = Collections.unmodifiableSet(normalized);
    }

    public String toSupportedTag(CustomLocale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("Language is required");
        }
        return toSupportedTag(locale.toLanguageTag());
    }

    public String toSupportedTag(String bcp47Tag) {
        if (bcp47Tag == null || bcp47Tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Language is required");
        }
        String candidate = bcp47Tag.trim().replace('_', '-');
        if (UNDETERMINED.equalsIgnoreCase(candidate)) {
            throw new IllegalArgumentException("Language is undetermined");
        }
        Locale locale = Locale.forLanguageTag(candidate);
        String language = locale.getLanguage();
        if (language == null || language.isEmpty() || UNDETERMINED.equalsIgnoreCase(language)) {
            throw new IllegalArgumentException("Language tag is invalid");
        }
        String normalizedTag = locale.toLanguageTag().toLowerCase(Locale.ROOT);
        if (supportedTags.contains(normalizedTag)) {
            return normalizedTag;
        }
        String normalizedLanguage = language.toLowerCase(Locale.ROOT);
        if (supportedTags.contains(normalizedLanguage)) {
            return normalizedLanguage;
        }
        throw new IllegalArgumentException("Language is not supported");
    }

    public boolean isEnglish(String languageTag) {
        return TranslateLanguage.ENGLISH.equals(toSupportedTag(languageTag));
    }

    public CustomLocale toCustomLocale(String languageTag) {
        return new CustomLocale(Locale.forLanguageTag(toSupportedTag(languageTag)));
    }

    public List<String> getSupportedTags() {
        return Collections.unmodifiableList(new ArrayList<>(supportedTags));
    }

    public List<CustomLocale> getSupportedLocales(final CustomLocale displayLanguage) {
        Objects.requireNonNull(displayLanguage, "displayLanguage");
        final Locale displayLocale = displayLanguage.getLocale();
        List<CustomLocale> locales = new ArrayList<>();
        for (String tag : supportedTags) {
            locales.add(new CustomLocale(Locale.forLanguageTag(tag)));
        }
        Collections.sort(locales, new Comparator<CustomLocale>() {
            @Override public int compare(CustomLocale left, CustomLocale right) {
                return left.getDisplayName(displayLocale).compareToIgnoreCase(right.getDisplayName(displayLocale));
            }
        });
        return Collections.unmodifiableList(locales);
    }
}
