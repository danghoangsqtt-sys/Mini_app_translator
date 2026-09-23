# Task 9.6 — Make legacy Cloud optional and complete migration UX

## Objective

Remove Cloud setup from the default journey, preserve an explicit legacy/advanced mode where safe, and synchronize privacy, pricing, attribution, and settings copy.

## Paths

- `app/src/main/java/nie/translator/rtranslatordevedition/api_management/`
- `app/src/main/java/nie/translator/rtranslatordevedition/settings/`
- `app/src/main/java/nie/translator/rtranslatordevedition/Global.java`
- `app/src/main/res/`
- `README.md`
- `privacy/`
- `app/src/test/`

## Acceptance criteria

- [ ] Fresh/default path contains no Cloud key requirement or billing marketing.
- [ ] Legacy Cloud is clearly labeled opt-in and never auto-selected for a keyless user.
- [ ] No shared credential or secret is present in source, resources, APK, or test fixtures.
- [ ] Existing encrypted credential can be removed safely and is not uploaded.
- [ ] Privacy/attribution/network disclosures match actual engine behavior.
- [ ] Resource/source tests prevent reintroduction of obsolete RTranslator/$300 copy.

