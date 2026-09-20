# System Rules — RTranslator

## Architecture rules

- Keep credential handling (`api_management/`) isolated — never widen the scope of the GCP token beyond what's already granted, never add a second place that reads/writes the key file.
- Any new persistence must go through a single shared `AppDatabase` instance — do not repeat the BUG-007 pattern of opening independent `Room.databaseBuilder` instances for the same file.

## Coding rules

- No empty `catch` blocks or bare `printStackTrace()`-only handlers for new code — at minimum log with a level check and propagate a user-visible failure state where the current UX expects one.
- No new use of `AsyncTask` (deprecated) — prefer `ExecutorService`/`Handler`+`Looper` or (if migrating) coroutines/RxJava already present as a dependency.
- Any `Handler` scheduling delayed work tied to a `Service`/`Activity` lifecycle must cancel pending callbacks in `onDestroy()`.

## Comment standards

- Good: a comment explaining *why* a non-obvious workaround exists (e.g. the existing Italian comment in `AndroidManifest.xml` about `configChanges` avoiding a spurious Toast on rotation — keep that kind).
- Bad: comments restating what the code already says, or leftover debug comments.

## Versioning

- SemVer (`versionName` in `app/build.gradle`), bump `versionCode` on every release build per existing project convention (already followed: versionCode 13 ↔ 1.1.2).

## Git conventions

- This repo's existing commit history is informal ("Update README.md" etc.) — for ViePilot-tracked work going forward, prefer Conventional Commits (`fix:`, `feat:`, `chore:`) so `CHANGELOG.md` generation (once introduced) can be automated later. Do not rewrite existing history to match.

## Quality gates (for Phase 1 hardening work)

- A fix for a `BUG-*` request is not done until: root cause addressed, and (where testable) a unit/instrumented test added under `app/src/test` or `app/src/androidTest` covering the failure scenario described in the request file.
- Given current 0% real coverage (`ENH-013`), don't block a hotfix on writing a full test suite — one targeted regression test per fixed bug is the bar, not full coverage.

## Stack-specific notes (Android/Java, gRPC, Room)

- Room: always close `Cursor`/`Stream` resources; avoid synchronous DB calls on the main thread; keep one `AppDatabase` singleton.
- gRPC: always pair `channel.shutdown()` with `awaitTermination(...)` on a bounded timeout, and never leave `StreamObserver.onError` unimplemented.
- Bluetooth: don't assume the transport library encrypts payloads — if introducing new sensitive data over Bluetooth, add app-layer encryption rather than trusting the link alone (see `ENH-001`).
