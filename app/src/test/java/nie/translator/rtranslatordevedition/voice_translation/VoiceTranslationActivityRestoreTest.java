package nie.translator.rtranslatordevedition.voice_translation;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class VoiceTranslationActivityRestoreTest {
    @Test public void staleConversationFallsBackToPairing() {
        assertEquals(VoiceTranslationActivity.PAIRING_FRAGMENT,
                VoiceTranslationActivity.resolveInitialFragment(
                        VoiceTranslationActivity.CONVERSATION_FRAGMENT, false));
    }

    @Test public void activeConversationCanRestore() {
        assertEquals(VoiceTranslationActivity.CONVERSATION_FRAGMENT,
                VoiceTranslationActivity.resolveInitialFragment(
                        VoiceTranslationActivity.CONVERSATION_FRAGMENT, true));
    }

    @Test public void walkieAndPairingRemainRestorable() {
        assertEquals(VoiceTranslationActivity.PAIRING_FRAGMENT,
                VoiceTranslationActivity.resolveInitialFragment(
                        VoiceTranslationActivity.PAIRING_FRAGMENT, false));
        assertEquals(VoiceTranslationActivity.WALKIE_TALKIE_FRAGMENT,
                VoiceTranslationActivity.resolveInitialFragment(
                        VoiceTranslationActivity.WALKIE_TALKIE_FRAGMENT, false));
    }

    @Test public void unknownPersistedModeFallsBackToPairing() {
        assertEquals(VoiceTranslationActivity.PAIRING_FRAGMENT,
                VoiceTranslationActivity.resolveInitialFragment(999, true));
    }
}
