# Task 9.5 — Integrate no-key engines into Conversation and WalkieTalkie

## Objective

Route both modes through the new engine contracts and make on-device-first the default selection without breaking Bluetooth message compatibility.

## Paths

- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/`
- `app/src/main/res/`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/`
- `app/src/androidTest/java/nie/translator/rtranslatordevedition/voice_translation/`

## Acceptance criteria

- [ ] Conversation and WalkieTalkie do not construct Cloud Translator/Recognizer directly.
- [ ] Supported-language selection reflects both recognizer and translator capability.
- [ ] One supported pair completes speech → translation → TTS without a Cloud key.
- [ ] Existing Bluetooth payload compatibility is preserved.
- [ ] Mode/service teardown closes engines and ignores stale callbacks.
- [ ] Error states are actionable and do not terminate the activity.

