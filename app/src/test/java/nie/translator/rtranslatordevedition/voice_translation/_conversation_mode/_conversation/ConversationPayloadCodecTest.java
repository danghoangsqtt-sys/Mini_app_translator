package nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import nie.translator.rtranslatordevedition.tools.CustomLocale;
import org.junit.Test;

public class ConversationPayloadCodecTest {
    @Test public void preservesLegacyGetCodeForRegionScriptAndVariantLocales() {
        assertEquals("helloen-US5", ConversationPayloadCodec.encode("hello", code("en-US")));
        // Legacy peers receive getCode(), which deliberately omits script/variant details.
        assertEquals("xinzh-CN5", ConversationPayloadCodec.encode("xin", code("zh-CN")));
        ConversationPayloadCodec.Payload decoded = ConversationPayloadCodec.decode("helloen-US5");
        assertEquals("hello", decoded.getText()); assertEquals("en", decoded.getLanguage().getLanguage()); assertEquals("US", decoded.getLanguage().getCountry());
    }
    @Test public void rejectsMalformedFramingRecoverably() {
        String[] invalid = { null, "", "ab", "texten-USx", "texten-US9", "text1", "text--2", "textzz-1" };
        for (String payload : invalid) {
            try { ConversationPayloadCodec.decode(payload); fail("accepted " + payload); }
            catch (IllegalArgumentException expected) { }
        }
    }
    private static CustomLocale code(final String value) {
        return new CustomLocale("en", "US") { @Override public String getCode() { return value; } };
    }
}
