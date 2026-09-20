# Phase 1 — Security & Stability Hardening (SPEC)

## Goal

Remove confirmed Critical/High defects before expanding the product UI or raising the Android target level.

## Paths

```text
app/src/main/java/nie/translator/rtranslatordevedition/api_management/KeyFileContainer.java
app/src/main/java/nie/translator/rtranslatordevedition/tools/Tools.java
app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/cloud_apis/voice/Recorder.java
app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/cloud_apis/voice/Recognizer.java
app/src/main/res/xml/backup_scheme.xml
app/src/test/
app/src/androidTest/
```

## Tasks

1. **1.1 / BUG-001** — Store credentials using Android Keystore-backed encryption, define migration/error behavior, and never serialize raw key material to logs.
2. **1.2 / BUG-002** — Exclude all credential storage paths from Auto Backup and data extraction rules; verify backup content on a test device.
3. **1.3 / BUG-003** — Increment the destination offset in `Tools.merge()` and add boundary tests for empty, single, and multiple arrays.
4. **1.4 / BUG-004** — Give recorder shutdown one owner, signal the recording loop, join it, then release `AudioRecord`; cover repeated start/stop.
5. **1.5 / BUG-005** — Propagate gRPC errors to service/UI state and make retry/teardown idempotent.
6. **1.6 / BUG-006** — Use Storage Access Framework for explicit JSON selection, validate content/schema/size, and remove broad external-storage access.

## Acceptance

- Each bug's request-level acceptance criteria pass.
- Security behavior is documented without embedding real credentials.
- Manual smoke test covers first launch, key import, Conversation mode, and WalkieTalkie mode.
