# Project Standards

Source of truth for code quality in EasyTrain.
Claude must follow these rules when generating or reviewing any code.

---

## 1. Kotlin Standards

- Kotlin 2.x with K2; `explicitApi()` is **not** enabled, but every public type in `core/*` has a KDoc one-liner only when the name alone is not self-explanatory.
- Coroutines everywhere: `suspend` for one-shot work, `Flow` for streams. No callbacks, no RxJava, no `LiveData`.
- Inject dispatchers (`@Dispatcher(IO)` qualifier from `core/common`); never call `Dispatchers.IO` directly — it makes code untestable.
- Immutable by default: `val`, `data class`, `List` not `MutableList` in public APIs.
- Sealed interfaces for state and errors (`sealed interface UiState`, `sealed interface AppError`). No exceptions for expected failures — return `Result<T>`/`AppError` from repositories.
- Time: `java.time` (`Instant`, `LocalDate`, `ZoneId`). Store `Instant` as ISO-8601 in DTOs and epoch millis in Room. Never `java.util.Date`, never device-local time for sync cursors.
- IDs: `kotlin.uuid.Uuid` (stable stdlib) generated on the client; stored as `String` in Room and `uuid` in Postgres.
- Weights are **always kg** in models, entities, DTOs and SQL. Convert to lb only in the UI layer via `UnitsFormatter`.
- No `!!`. No `lateinit` outside Android-framework-mandated classes. No `GlobalScope`.
- ktlint (official style) is the formatter; CI fails on violations. Don't hand-format.

Anti-patterns — never do these:
- Business logic in a Composable or in `MainActivity`
- Calling Supabase or Room from a ViewModel (go through a repository)
- `runBlocking` outside tests
- Catching `Exception` broadly and swallowing it

---

## 2. Compose / UI Standards

- **State hoisting + UDF**: a screen is `XxxRoute` (collects the ViewModel, wires events) → `XxxScreen(state, onEvent…)` (stateless, previewable). Every `XxxScreen` has at least one `@Preview` with fake state.
- One `StateFlow<XxxUiState>` per ViewModel, built with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`. Events are plain functions on the ViewModel (`onSetCompleted(setId)`), not a giant `onEvent(Event)` sealed class unless a screen has >8 events.
- `UiState` is a sealed interface: `Loading`, `Success(data)`, `Error(error)`; or a single data class with nullable/loading fields when partial rendering matters (the workout logger).
- Material 3 only, through `core/designsystem` (`EasyTrainTheme`, `Tokens`). No hardcoded colors, dp or sp in feature modules — use theme tokens and the spacing scale.
- All strings in `res/values/strings.xml` of the owning module (`feature:workout` owns its own strings). English is the source; Hebrew (`values-iw`) is planned — do not hardcode LTR assumptions; use `Modifier.padding(start=…)` not `left`.
- Lists use `LazyColumn` with stable `key = { it.id }`; give Compose stable classes (`@Immutable` on domain models with collections).
- Navigation: `@Serializable` route objects in each feature's `XxxNavigation.kt` (`fun NavGraphBuilder.workoutScreen(...)`, `fun NavController.navigateToWorkout(id)`); `app` assembles them. Feature modules never reference another feature's routes.
- Accessibility: every icon-only button has `contentDescription`; touch targets ≥ 48dp; the set-logging row must be usable one-handed (large targets, numeric keyboard, no tiny steppers).
- Respect `WindowInsets` and `reduceMotion`. No custom splash — use `androidx.core:core-splashscreen`.

Anti-patterns:
- `remember { mutableStateOf() }` holding data that should live in the ViewModel
- Passing `NavController` into screens (pass lambdas)
- `LaunchedEffect(Unit)` to load data (load in `init`/`stateIn` instead)
- Recomposition-heavy list items doing formatting work — precompute in the ViewModel

---

## 3. Data Layer Standards (Room, repositories, mappers)

- Room entities live in `core/database`, are `internal`-ish (only `core/data` depends on the module), and carry the sync columns: `id`, `createdAt`, `updatedAt`, `deletedAt`, `syncState`, `localUpdatedAt`.
- DAOs expose `Flow<List<Entity>>` for reads and `suspend` upserts (`@Upsert`) for writes. Every query on a synced table filters `deleted_at IS NULL` unless it is the sync engine's own.
- `exportSchema = true`, schemas committed under `core/database/schemas/`. Every schema change ships a `Migration` + an `AutoMigration` test (`MigrationTestHelper`). **Never** `fallbackToDestructiveMigration` outside debug builds.
- Repositories (`core/data`) are interfaces with an `OfflineFirstXxxRepository` implementation bound in Hilt. Public API: `observeX(): Flow<…>`, `suspend fun saveX(…)`, `suspend fun deleteX(id)` (soft delete). No `getXBlocking`.
- Mappers are top-level extension functions next to the DTO/entity: `fun WorkoutDto.asEntity()`, `fun WorkoutEntity.asExternalModel()`. Mapping is total — no partially-mapped objects.
- Every synced table is registered exactly once in `SyncRegistry` (`core/data`) with its DAO, DTO serializer, PostgREST table name, user filter column and parent tables. See `/add-synced-table`.

---

## 4. Supabase / SQL Standards

- One concern per migration; file name `supabase migration new <verb>_<object>` (e.g. `create_workouts`). Migrations are append-only: **never edit an applied migration** — write a new one.
- Every new table, in the same migration: sync columns → `enable row level security` → policies (`select`, `insert`, `update`; no `delete` policy — soft delete only) → `set_updated_at` trigger → indexes on every column used in a policy or delta pull (`(trainee_id, updated_at)`, `(coach_id, updated_at)`) → `alter publication supabase_realtime add table …` when the app subscribes.
- Policies: always pair `using` with `with check` on `insert`/`update`. Use the helper functions (`auth_role()`, `is_coach_of()`, `my_coach_ids()`) — never inline `exists (select … from coach_trainees …)` in a policy; wrap `auth.uid()` as `(select auth.uid())` so it is evaluated once per statement.
- Server-only logic (invite acceptance, notification fan-out, anything with `security definer`) sets `set search_path = ''` and fully qualifies names (`public.coach_trainees`).
- Enums as `check` constraints on `text` columns (not Postgres enums — cheaper to evolve).
- `timestamptz` everywhere, `date` for calendar-day fields (`measured_on`, `logged_on`).
- Seed data (`seed.sql`) is idempotent (`on conflict (id) do update`) and safe to re-run.
- Every table gets a pgTAP test file `supabase/tests/<table>.test.sql` covering: owner can read/write, linked coach/trainee can read, stranger cannot read, stranger cannot write, role enforcement on writes.
- Edge functions: TypeScript, Deno, one function per directory, secrets via `Deno.env.get`, verify the webhook secret header, never trust the payload's `recipient_id` without re-reading the row with the service-role client.
- The Android app only ever holds the **publishable** key. The `service_role` key exists only in edge-function secrets and CI.

---

## 5. Sync Standards

- The UI never observes network state to decide what to show — it shows Room. Connectivity only affects a small "syncing / offline" indicator.
- Any repository write = Room upsert with `PENDING` + `SyncScheduler.requestSync()`. Nothing else. Features never call PostgREST.
- Push order: parents before children (`programs` → `program_days` → `program_exercises`; `workouts` → `workout_sets`). Pull order: the same.
- Pull must never overwrite a `PENDING` local row. Tests prove it.
- Cursors come from server `updated_at` values, never from `System.currentTimeMillis()`.
- Realtime is a poke, not a data path. If realtime breaks, periodic + foreground pulls still make the app correct.
- Sign-out: cancel workers, close the realtime channel, `clearAllTables()`, clear cursors, then clear the auth session — in that order.

---

## 6. Testing Standards

- Every ViewModel has a unit test using `MainDispatcherRule` + Turbine (`state.test { … }`) and fakes from `core/testing` — no mocking library.
- Every repository has a test against an in-memory Room DB + a `FakeRemoteDataSource`.
- `SyncEngine` has tests for: push marks synced, push failure keeps pending, pull skips pending rows, tombstones propagate, cursor advances only on success.
- Each screen has one Robolectric Compose test asserting the main states render (loading / empty / content / error) via `composeTestRule`.
- pgTAP tests for every RLS policy (see §4). CI runs them whenever `supabase/` changes.
- Test names read as sentences: `` fun `completing a set marks it pending and requests sync`() ``.
- No tests that sleep. Use `advanceUntilIdle()` / `runTest`.

---

## 7. Gradle / Build Standards

- All versions in `gradle/libs.versions.toml`; modules never write a version literal. Resolve versions live (`/resolve-versions`).
- Convention plugins in `build-logic/` own `compileSdk`, `minSdk`, Java toolchain, Compose, Hilt, Room and test config. A module `build.gradle.kts` is ≤ 20 lines: apply plugins, declare `namespace`, list dependencies.
- KSP only (Room, Hilt). No kapt.
- `buildConfig` fields for `SUPABASE_URL` / `SUPABASE_PUBLISHABLE_KEY` are read from `local.properties` locally and from env vars in CI; a missing value fails the build with a clear message rather than producing an app that points nowhere.
- R8 enabled for release with `proguard-rules.pro` covering kotlinx-serialization and supabase-kt. `isDebuggable = false`, `isMinifyEnabled = true`, `isShrinkResources = true` on release.
- Release signing config reads from env (`EASYTRAIN_KEYSTORE_PATH`, `_PASSWORD`, `_KEY_ALIAS`, `_KEY_PASSWORD`); absent → the release build type falls back to unsigned and prints a warning (so CI on PRs still compiles).

---

## 8. GitHub Actions CI/CD Standards

- Every `uses:` pinned to a full commit SHA with a trailing `# vX.Y.Z` comment, resolved live via the GitHub API (see `/resolve-versions`).
- Separate jobs: `lint`, `unit-test`, `build`, `db-test` (conditional on `supabase/**` changes), and a final `ci-ok` job that `needs` all of them — that job is the required status check.
- Gradle: `gradle/actions/setup-gradle` with cache; `--no-daemon --stacktrace`; JDK 17 via `actions/setup-java` (temurin).
- Secrets only via `${{ secrets.X }}` into env; never `echo` them; `SUPABASE_URL`/key for CI builds point to a dummy local value on PRs.
- `release.yml` runs on `v*.*.*` tags only, derives `versionName` from the tag and `versionCode` from `github.run_number` + offset, decodes the keystore from a base64 secret to a temp path, builds `bundleRelease`, uploads the AAB to the GitHub Release.
- Concurrency group per ref with `cancel-in-progress: true` for PR workflows.

---

## 9. Security Standards

Non-negotiable:
- No real credentials anywhere in the repo (not even in `.gitignore`d files that get committed by mistake — `local.properties`, `*.jks`, `google-services.json` are gitignored and checked by a CI grep).
- RLS enabled on every table before its first insert; a table without policies is a bug, not a TODO.
- No `service_role` key in Android code, Gradle files, or CI logs.
- Storage buckets are private; access is by RLS policy on `storage.objects`, never by public URL. The app uses signed URLs with short expiry for media.
- Auth deep link (`easytrain://auth-callback`) is declared with `android:autoVerify` and handled only by `MainActivity`; tokens from the link are handed straight to supabase-kt.
- Exported components: none except `MainActivity` and the FCM service. `android:allowBackup="false"` (Room contains another person's health data).
- Minimum: `minSdk 26`; network security config blocks cleartext except `10.0.2.2` in debug.

---

## 10. Non-Negotiable Rules

- No dumping files outside the defined module/directory structure
- No hardcoded secrets or keys
- No Supabase/Room calls outside `core/data`, `core/network`, `core/database`, `core/sync`
- No table without RLS and a pgTAP test
- No version literals outside the version catalog; no versions from memory
- No editing applied migrations
- No skipping the manifest update
