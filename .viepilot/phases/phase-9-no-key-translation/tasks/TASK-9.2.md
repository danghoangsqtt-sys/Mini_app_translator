# Task 9.2 — Introduce engine contracts and capability model

## Objective

Create testable interfaces/factories for speech recognition, text translation, and speech output without changing the selected runtime engine yet.

## Paths

- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/cloud_apis/`
- `app/src/main/java/nie/translator/rtranslatordevedition/Global.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/`

## Acceptance criteria

- [ ] Contracts cover translate, supported languages, recognition lifecycle, cancellation, errors, and capability state.
- [ ] Existing Cloud clients are wrapped behind adapters; public UI/service code no longer needs Cloud-specific constructors for new work.
- [ ] No behavior switch and no credential regression.
- [ ] Unit tests cover factory selection and error/cancellation mapping.
- [ ] Full unit/lint/debug build passes.

