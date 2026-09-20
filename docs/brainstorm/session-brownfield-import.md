# Brownfield Import — RTranslator

- **Import source**: `vp-crystallize --brownfield`
- **Import date**: 2026-09-19
- **Repo**: https://github.com/niedev/RTranslator
- **Checked-out branch at import time**: `master` (stale v1.x lineage — `git describe` = `1.1.3-21-gdcd77a0`). Remote default branch is `v3.00` (tags up to `3.0.0-beta1`). See `.viepilot/requests/ENH-015.md`.

## Scan Report

```yaml
schema_version: 1
gap_tier: DETECTED
root:
  project_name: RTranslator
  package_id: nie.translator.rtranslatordevedition
  current_version: "1.1.2"
  version_code: 13
  primary_language: Java
  frameworks:
    - Android SDK (compileSdk 29, minSdk 23, targetSdk 29)
    - AndroidX (Room 2.1.0, ConstraintLayout, Preference)
    - gRPC 1.11.0 / Protobuf (Google Cloud Speech-to-Text, Translation)
    - google-auth-library-oauth2-http 0.8.0 (OAuth2 for Google Cloud)
    - nimbus-jose-jwt 5.1 (JWS parsing)
    - com.github.niedev:BluetoothCommunicator 1.0.6 (P2P Bluetooth transport, external dep)
  build_tooling:
    android_gradle_plugin: 3.6.1
    gradle_wrapper: 5.6.4
    build_tools_version: "28.0.3"
  repo_url: https://github.com/niedev/RTranslator
  license: Apache-2.0
  first_commit: 2020-03-30
  git_tags: [1.0.2, 1.1.0, 1.1.1, 1.1.2, 1.1.3, 2.0.0, 2.0.1, 2.0.2, 2.0.3, 2.0.4, 2.1.0, 2.1.1, 2.1.2, 2.1.3, 2.1.4, 2.1.5, 3.0.0-beta1]
  git_default_branch_remote: v3.00
  git_checked_out_branch: master (stale, v1.x lineage)
modules:
  - name: app
    type: root
    gap_tier: DETECTED
    primary_language: Java
    framework: Android SDK
    module_purpose: Bluetooth-based real-time speech translator (Conversation mode + WalkieTalkie mode), using Google Cloud Speech-to-Text + Translation APIs
    entry_point: nie.translator.rtranslatordevedition.LoadingActivity
    loc: 24809
    file_count: 122
    test_coverage: near-zero (2 boilerplate files only)
open_questions:
  - "Should future ViePilot work target the stale `master` branch or switch to origin's `v3.00` default branch first?"
  - "Is `.agents/skills/` (untracked, contains unrelated pdf-translate/Python content) intentional or accidental clutter?"
```

## Purpose

This stub exists so `vp-audit` and other ViePilot tools recognize this project as a valid brownfield import (no greenfield brainstorm session was run). Full architecture/context detail lives in `.viepilot/ARCHITECTURE.md` and `.viepilot/PROJECT-CONTEXT.md`. Findings from the codebase audit that motivated this import are tracked individually under `.viepilot/requests/`.
