package nie.translator.rtranslatordevedition.voice_translation.engines.translation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import nie.translator.rtranslatordevedition.settings.SettingsActivity;
import nie.translator.rtranslatordevedition.settings.SettingsFragment;
import nie.translator.rtranslatordevedition.settings.TranslationModelsPreference;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MlKitTranslationInstrumentedTest {
    private static final long MODEL_TIMEOUT_MINUTES = 5L;

    @Test public void a_onlinePrepareTranslateAndDeleteAuxiliaryModel() throws Exception {
        List<String> supported = TranslateLanguage.getAllLanguages();
        assertTrue(supported.contains(TranslateLanguage.ENGLISH));
        assertTrue(supported.contains(TranslateLanguage.ITALIAN));
        assertTrue(supported.contains(TranslateLanguage.GERMAN));

        RemoteModelManager manager = RemoteModelManager.getInstance();
        DownloadConditions wifi = new DownloadConditions.Builder().requireWifi().build();
        TranslateRemoteModel italian = model(TranslateLanguage.ITALIAN);
        TranslateRemoteModel german = model(TranslateLanguage.GERMAN);

        Tasks.await(manager.download(italian, wifi), MODEL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        assertTrue(Tasks.await(manager.isModelDownloaded(italian)));

        Translator translator = translator(TranslateLanguage.ENGLISH, TranslateLanguage.ITALIAN);
        try {
            String translated = Tasks.await(translator.translate("Hello"), 1L, TimeUnit.MINUTES);
            assertTrue(translated != null && !translated.trim().isEmpty());
            assertFalse("hello".equalsIgnoreCase(translated.trim()));
        } finally {
            translator.close();
        }

        Tasks.await(manager.download(german, wifi), MODEL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        assertTrue(Tasks.await(manager.isModelDownloaded(german)));
        Tasks.await(manager.deleteDownloadedModel(german), 1L, TimeUnit.MINUTES);
        assertFalse(Tasks.await(manager.isModelDownloaded(german)));
        assertTrue(Tasks.await(manager.isModelDownloaded(italian)));
    }

    /** Run this method alone after adb network isolation; it never invokes a download API. */
    @Test public void b_offlineTranslateAlreadyDownloadedModelWithoutDownload() throws Exception {
        RemoteModelManager manager = RemoteModelManager.getInstance();
        TranslateRemoteModel italian = model(TranslateLanguage.ITALIAN);
        assertTrue("Online preparation must download Italian first",
                Tasks.await(manager.isModelDownloaded(italian)));

        Translator translator = translator(TranslateLanguage.ENGLISH, TranslateLanguage.ITALIAN);
        try {
            String translated = Tasks.await(translator.translate("Good morning"), 1L, TimeUnit.MINUTES);
            assertTrue(translated != null && !translated.trim().isEmpty());
            assertFalse("good morning".equalsIgnoreCase(translated.trim()));
        } finally {
            translator.close();
        }
    }

    @Test public void c_settingsContainsAndOpensModelManagementPreference() throws Throwable {
        Intent intent = new Intent(InstrumentationRegistry.getTargetContext(), SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        final AtomicReference<TranslationModelsPreference> preference = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override public void run() {
                SettingsFragment fragment = (SettingsFragment) ((SettingsActivity) activity)
                        .getSupportFragmentManager()
                        .findFragmentById(nie.translator.rtranslatordevedition.R.id.fragment_settings_container);
                assertNotNull(fragment);
                TranslationModelsPreference result = (TranslationModelsPreference) fragment
                        .findPreference("translationModels");
                assertNotNull(result);
                preference.set(result);
                result.performClick();
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertNotNull(preference.get());
        activity.finish();
    }

    private static TranslateRemoteModel model(String languageTag) {
        return new TranslateRemoteModel.Builder(languageTag).build();
    }

    private static Translator translator(String source, String target) {
        return Translation.getClient(new TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build());
    }
}
