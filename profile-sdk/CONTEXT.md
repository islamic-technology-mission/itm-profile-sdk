# Profile SDK — Session Context

This file lets Claude recall everything about this project without re-reading source files.
Last updated: 2026-06-30

---

## What this project is

A **Kotlin Multiplatform (KMP)** SDK (`com.itm.profilesdk:profile-sdk:1.0.4`) that runs on Android and iOS. It manages user profiles, screen time, subscriptions, profile views, and nearby users for the Islam360 app. Base URL: `https://sandbox.theislam360.com/`

---

## Repository layout

```
profile-sdk/
├── src/
│   ├── commonMain/kotlin/com/itm/profile_sdk/
│   │   ├── core/
│   │   │   ├── ISDKClient.kt          ← public singleton entry point (all SDK functions)
│   │   │   └── SDKState.kt            ← internal: userId, coroutine scope, repo ref
│   │   ├── auth/
│   │   │   ├── TokenManager.kt        ← caches token, auto-renews 60s before expiry
│   │   │   ├── InternalTokenService.kt← calls POST /api/v1/internal/generate-token
│   │   │   ├── GenerateTokenRequest.kt
│   │   │   ├── GenerateTokenResponse.kt
│   │   │   └── TokenData.kt
│   │   ├── network/
│   │   │   ├── ApiConstants.kt        ← BASE_URL + endpoint helpers
│   │   │   ├── ApiException.kt        ← typed exceptions: BadRequest/Unauthorized/etc
│   │   │   ├── HttpClientFactory.kt   ← Ktor client: JSON, logging, error handling
│   │   │   └── UserProfileApiService.kt ← internal interface
│   │   ├── Implementation/
│   │   │   └── UserProfileApiServiceImpl.kt ← Ktor HTTP calls
│   │   ├── repository/
│   │   │   └── UserProfileRepository.kt ← business logic, caching, optimistic updates
│   │   ├── local/
│   │   │   ├── AppDatabase.kt         ← Room DB v4, 3 entities
│   │   │   ├── Migrations.kt          ← MIGRATION_1_2, _2_3, _3_4
│   │   │   ├── dao/
│   │   │   │   ├── UserProfileDao.kt
│   │   │   │   ├── ScreenTimeDao.kt
│   │   │   │   └── ScreenTimeTargetDao.kt
│   │   │   ├── entity/
│   │   │   │   ├── UserProfileEntity.kt
│   │   │   │   ├── ScreenTimeEntity.kt    ← PK: (userId, date)
│   │   │   │   └── ScreenTimeTargetEntity.kt
│   │   │   └── mapper/
│   │   │       └── EntityMappers.kt
│   │   ├── models/
│   │   │   ├── UserProfile.kt         ← has isPublic() helper
│   │   │   ├── UserLocation.kt
│   │   │   ├── UpsertProfileRequest.kt
│   │   │   ├── UpdateProfileRequest.kt
│   │   │   ├── ScreenTimeRequest.kt
│   │   │   ├── ScreenTimeEntry.kt
│   │   │   ├── ScreenTimeProgress.kt  ← daily/weekly/monthly progress
│   │   │   ├── ScreenTimeResponse.kt
│   │   │   ├── ScreenTimeData.kt
│   │   │   ├── Subscription.kt
│   │   │   ├── NearbyUser.kt
│   │   │   ├── NearbyUsersData.kt
│   │   │   ├── NearbyUsersResponse.kt
│   │   │   ├── ProfileViewer.kt
│   │   │   ├── ProfileViews.kt
│   │   │   ├── ProfileViewsData.kt
│   │   │   ├── ProfileViewsResponse.kt
│   │   │   ├── UserProfileData.kt
│   │   │   └── UserProfileResponse.kt
│   │   └── util/
│   │       ├── Result.kt              ← sealed class: Success / Error / Loading
│   │       └── Cancellable.kt         ← returned by observe* functions
│   ├── androidMain/
│   │   ├── AndroidManifest.xml
│   │   ├── kotlin/.../Platform.android.kt
│   │   └── kotlin/.../local/AppDatabase.android.kt  ← Room + OkHttp
│   └── iosMain/
│       ├── kotlin/.../Platform.ios.kt
│       └── kotlin/.../local/AppDatabase.ios.kt      ← NSDocumentDirectory path
├── schemas/com.itm.profile_sdk.local.AppDatabase/   ← Room schema exports (v1-v4)
├── build.gradle.kts                   ← KMP config, cocoapods, maven-publish
├── Profile-SDK.podspec                ← CocoaPods spec (framework name: Profile_SDK)
├── Profile_SDK.podspec                ← duplicate podspec
├── SDK_DOCUMENTATION.md              ← full markdown docs (with architecture SVG ref)
├── architecture.svg                  ← architecture diagram (referenced in .md)
├── SDK_DOCUMENTATION.docx            ← Google Docs-ready Word version
└── CONTEXT.md                        ← this file
```

---

## Key architecture facts

- **ISDKClient** is an `object` (singleton). Call `initialize(userId, context)` once before anything else.
- **Android** requires `Application` context in `initialize()`. iOS passes nothing (or `Unit`).
- **Token** is fetched via `generateToken(internalKey)`, cached in `TokenManager`, auto-renewed 60s before expiry. Token is cleared on `initialize()` with a new userId.
- **Profile and ScreenTime** are cached in Room; all other data (Subscription, ProfileViews, NearbyUsers) is always fetched live from the API.
- **updateProfile** is optimistic: writes to DB immediately, rolls back on API failure.
- **observeProfile / observeScreenTime** return a `Cancellable` — must call `.cancel()` to stop.
- **ScreenTimeProgress** is computed locally from cached entries: daily/weekly/monthly totals vs target (default 15 min/day).
- Room DB name: `app_database.db`, current schema version: **4**.
- iOS DB path: `NSDocumentDirectory/app_database.db`.
- Android DB path: `context.getDatabasePath("app_database.db")`.

---

## Public API surface (ISDKClient)

| Function | Returns | Cached |
|---|---|---|
| `initialize(userId, context)` | Unit | — |
| `generateToken(internalKey, onResult)` | `Result<String>` (idToken) | Yes (TokenManager) |
| `observeProfile(token, onEach, onError)` | `Cancellable` | Yes (Room Flow) |
| `observeProfile(userId, token, onEach, onError)` | `Cancellable` | Yes |
| `upsertProfile(token, request, onResult)` | `Result<UserProfile>` | Writes to Room |
| `updateProfile(token, request, onResult)` | `Result<UserProfile>` | Optimistic |
| `updateProfile(userId, token, request, onResult)` | `Result<UserProfile>` | Optimistic |
| `getSubscription(token, onResult)` | `Result<Subscription>` | No |
| `getProfileViews(token, cursor, limit, onResult)` | `Result<ProfileViewsData>` | No |
| `observeScreenTime(token, days, onEach, onError, onComplete)` | `Cancellable` | Yes |
| `postScreenTime(token, request, days, onResult)` | `Result<Unit>` | Writes to Room |
| `getNearbyUsers(token, lat, lng, onResult)` | `Result<List<NearbyUser>>` | No |

---

## REST API endpoints

| Method | Path | SDK function |
|---|---|---|
| POST | `/api/v1/internal/generate-token` | `generateToken()` |
| POST | `/api/v1/users/{userId}/profile` | `upsertProfile()` |
| PATCH | `/api/v1/users/{userId}/profile` | `updateProfile()` |
| GET | `/api/v1/users/{userId}/profile` | `observeProfile()` / `getSubscription()` |
| GET | `/api/v1/users/{userId}/profile-views` | `getProfileViews()` |
| POST | `/api/v1/users/{userId}/screen-time` | `postScreenTime()` |
| GET | `/api/v1/users/{userId}/screen-time?days={n}` | `observeScreenTime()` |
| GET | `/api/v1/users/nearby?lat&lng` | `getNearbyUsers()` |

---

## Documentation files

### SDK_DOCUMENTATION.md
Full markdown reference doc. Includes architecture SVG reference, all APIs with Kotlin + Swift examples, all data models, error handling, local DB section.

### architecture.svg
Standalone SVG architecture diagram. Referenced from SDK_DOCUMENTATION.md as `![Profile SDK Architecture](architecture.svg)`. Shows 5 layers: ISDKClient → TokenManager/SDKState → Repository → ApiServiceImpl/AppDatabase → REST API. Color coded: purple = core SDK, teal = local DB/DAOs, coral = network, gray = infrastructure.

### SDK_DOCUMENTATION.docx
Word document for Google Docs import. Generated by the build script below. Validates at 651 paragraphs.

---

## DOCX build script

**Location:** `/Users/ammarali/Library/Application Support/Claude/local-agent-mode-sessions/3b59ef67-c4cf-46bb-b8c5-e8f106dff96b/b0f3e51b-9b48-4856-9a71-18ac779060de/local_13242f5c-2419-4202-8323-3e14f1a37e67/outputs/build_doc.js`

**Note:** The outputs folder is a temporary scratchpad — it may be cleared between sessions. If the script is gone, it needs to be recreated. The workspace path that persists is the profile-sdk folder above.

**To rebuild the docx after changes:**
```bash
cd /sessions/elegant-brave-ride/mnt/outputs
npm install docx   # if not already installed
node build_doc.js
```

**Bash path mapping** (Claude's shell uses different paths than file tools):
- Workspace folder → `/sessions/elegant-brave-ride/mnt/profile-sdk/`
- Outputs folder → `/sessions/elegant-brave-ride/mnt/outputs/`
- Skills folder → `/sessions/elegant-brave-ride/mnt/.claude/skills/`

**Validate the docx:**
```bash
python /sessions/elegant-brave-ride/mnt/.claude/skills/docx/scripts/office/validate.py \
  /sessions/elegant-brave-ride/mnt/profile-sdk/SDK_DOCUMENTATION.docx
```

### Key design decisions in build_doc.js

- **Code blocks use table cells** (not paragraph shading) — the only way to get true internal padding inside a gray box that works in both Word and Google Docs. Paragraph `indent` only moves the text, not the shading edge.
- **Leading spaces → non-breaking spaces (` `)** — Google Docs strips regular leading spaces on import; NBSP is preserved.
- **Soft line breaks (`break: 1`)** inside a single TextRun paragraph — keeps all code lines in one paragraph so the gray background is unbroken and Google Docs doesn't collapse the block.
- **Table cell margins:** `top/bottom: 280`, `left/right: 400` twips.
- **Line spacing inside code:** `line: 320, lineRule: "auto"` (1.33×).
- **Fonts:** Body = Arial 11pt, Code = Courier New 9pt.
- **Table column widths** always use `WidthType.DXA` (never percentage — breaks in Google Docs).

---

## Sections removed from the docx (by user request on 2026-06-30)

- **Generate Token** code examples (Kotlin + Swift) — removed from Authentication section
- **Entities** table (`user_profile`, `screen_time`, `screen_time_target`)
- **Migration history** table (v1→v2→v3→v4)

These sections still exist in `SDK_DOCUMENTATION.md`.

---

## Build / distribution

- **Android:** Published to GitHub Packages Maven (`https://maven.pkg.github.com/islamic-technology-mission/itm-profile-sdk`). Credentials via `github.actor` / `github.token` in `local.properties` or env vars.
- **iOS:** CocoaPod via `Profile-SDK.podspec`. Framework name: `Profile_SDK`. Run `./gradlew :profile-sdk:generateDummyFramework` once before first `pod install` (creates a placeholder framework so CocoaPods can parse the Podspec — the real framework builds via `syncFramework` script phase in Xcode).
- **Min Android SDK:** 24. **iOS deployment target:** 15.0.
