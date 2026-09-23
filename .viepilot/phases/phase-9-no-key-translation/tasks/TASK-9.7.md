# Task 9.7 — Full regression and release-candidate gate

## Objective

Prove the no-key flow across supported SDK levels and two physical phones before creating a release candidate.

## Paths

- `app/src/test/`
- `app/src/androidTest/`
- `.viepilot/phases/phase-9-no-key-translation/`
- `CHANGELOG.md`
- `README.md`

## Automated verification

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain
.\gradlew.bat connectedDebugAndroidTest --console=plain
```

## Manual matrix

- API 23, 31, 34, and 36 launch/relaunch/process recreation.
- Fresh install without credential.
- Microphone/Nearby permission grant, deny, revoke, and re-grant.
- Model download success/failure/cancel/delete and low-storage handling.
- Airplane-mode translation using a previously downloaded model.
- On-device recognizer present/absent and system fallback disclosure.
- Conversation between two phones over Bluetooth.
- WalkieTalkie alternating language turns.
- Light/dark, TalkBack, 200% font, adaptive icon/splash.
- No active RTranslator branding, $300 claim, or false BLE message.

## Release gate

- [ ] Zero automated failures and zero lint errors.
- [ ] No unresolved Critical/High defect.
- [ ] Exact APK hash/size and install evidence recorded.
- [ ] PM reviews diff, state files, and evidence.
- [ ] VersionCode/versionName, signing, tag, and push require separate maintainer approval.

