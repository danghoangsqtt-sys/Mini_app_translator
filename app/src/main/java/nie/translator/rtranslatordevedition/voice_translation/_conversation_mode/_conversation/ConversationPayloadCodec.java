package nie.translator.rtranslatordevedition.voice_translation._conversation_mode._conversation;

import nie.translator.rtranslatordevedition.tools.CustomLocale;

/** Preserves the legacy text + languageCode + languageCode.length() framing exactly. */
public final class ConversationPayloadCodec {
    private ConversationPayloadCodec() { }
    public static String encode(String text, CustomLocale language) {
        String code = language.getCode();
        return text + code + code.length();
    }
    public static Payload decode(String payload) {
        if (payload == null || payload.length() < 3) { throw new IllegalArgumentException("Malformed conversation payload"); }
        char suffix = payload.charAt(payload.length() - 1);
        if (!Character.isDigit(suffix)) { throw new IllegalArgumentException("Malformed conversation payload"); }
        int codeLength = Character.digit(suffix, 10);
        int textEnd = payload.length() - codeLength - 1;
        if (codeLength < 2 || textEnd < 0) { throw new IllegalArgumentException("Malformed conversation payload"); }
        String code = payload.substring(textEnd, payload.length() - 1);
        if (!code.matches("[A-Za-z]{2,3}(-[A-Za-z]{2,8})?")) { throw new IllegalArgumentException("Malformed conversation payload"); }
        CustomLocale language = CustomLocale.getInstance(code);
        if (language == null || language.getLanguage().isEmpty()) { throw new IllegalArgumentException("Malformed conversation payload"); }
        return new Payload(payload.substring(0, textEnd), language);
    }
    public static final class Payload {
        private final String text; private final CustomLocale language;
        private Payload(String text, CustomLocale language) { this.text = text; this.language = language; }
        public String getText() { return text; }
        public CustomLocale getLanguage() { return language; }
    }
}
