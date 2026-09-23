# Task 9.4 — Implement Android SpeechRecognizer engine

## Objective

Provide bounded-utterance speech recognition using Android SpeechRecognizer, preferring on-device recognition when supported and safely falling back to the system recognizer.

## Paths

- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/`
- `app/src/androidTest/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/`

## Acceptance criteria

- [ ] API 31-only calls are guarded; API 23 remains supported.
- [ ] On-device availability is checked before construction.
- [ ] Recognizer callbacks are generation/session isolated and delivered on the expected thread.
- [ ] Cancel/stop/destroy behavior is deterministic and lifecycle-safe.
- [ ] Offline preference is a preference, not a promise; fallback disclosure is truthful.
- [ ] The engine does not run an indefinite continuous-recognition loop.
- [ ] Permission denial and recognizer absence are recoverable.

