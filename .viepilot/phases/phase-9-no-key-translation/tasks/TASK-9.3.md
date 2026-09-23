# Task 9.3 — Implement ML Kit translation and model management

## Objective

Implement the default on-device text translation engine with explicit model download, progress, metered-network policy, offline use, and model deletion.

## Paths

- `app/build.gradle`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/`
- `app/src/main/java/nie/translator/rtranslatordevedition/settings/`
- `app/src/main/res/`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/`
- `app/src/androidTest/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/`

## Acceptance criteria

- [ ] Uses official ML Kit Translation dependency and supported APIs.
- [ ] No API key or Cloud project is required.
- [ ] Missing model is a recoverable state with explicit user action/progress.
- [ ] Downloaded model translates a supported pair in airplane mode.
- [ ] Translators are closed; concurrent/cancelled work cannot leak callbacks.
- [ ] Required attribution and quality limitations are represented in user-facing documentation.
- [ ] Unit/instrumentation and full Gradle gates pass.

