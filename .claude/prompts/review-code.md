# Prompt: Review Code / PR

Use this prompt when reviewing a PR or a completed phase.
Give it to Claude Code with the diff or PR number.

---

Review the following code/PR against:
- `.claude/standards.md`
- `.claude/context.md` (data model, RLS summary, sync contract)

Check for:

**Correctness**
- [ ] Behaviour matches the spec in `context.md` / the phase's "Done when" list in `roadmap.md`
- [ ] Weights stored in kg; unit conversion only in the UI layer
- [ ] Time handling: `Instant`/`LocalDate`, server `updated_at` for cursors, no device clock in sync
- [ ] Error cases handled with `AppError`, no swallowed exceptions

**Supabase / SQL**
- [ ] New tables have the sync columns, `enable row level security`, `set_updated_at` trigger, indexes on policy/delta columns
- [ ] Every write policy has `with check`; no DELETE policy; helper functions instead of inline `exists`
- [ ] `security definer` functions set `search_path = ''` and fully qualify names
- [ ] Migration is a new file (no edit to an applied one), one concern, idempotent seed
- [ ] pgTAP tests cover allow + deny for each role and the stranger case
- [ ] Table added to the realtime publication if the app subscribes

**Offline-first / sync**
- [ ] UI reads only from Room via `Flow`; no Supabase call outside `core/*`
- [ ] Writes upsert to Room with `PENDING` then request sync — nothing else
- [ ] Table registered once in `SyncRegistry` with correct parents and filter column
- [ ] Pull never overwrites `PENDING` rows (test exists)

**Compose / architecture**
- [ ] `XxxRoute` / `XxxScreen` split; one `StateFlow<UiState>`; `collectAsStateWithLifecycle`
- [ ] No `NavController` in screens; typed routes; no cross-feature dependency
- [ ] Theme tokens only; strings in the module's `strings.xml`; `contentDescription` on icon buttons
- [ ] Previews for each meaningful state

**Tests**
- [ ] ViewModel test (Turbine) and Robolectric screen test present and meaningful
- [ ] Repository test with in-memory Room + fake remote
- [ ] No sleeps, no real dispatchers

**Build / CI**
- [ ] No version literals outside the catalog; versions resolved live (table in PR description)
- [ ] Actions SHA-pinned with version comment
- [ ] No secrets, keystores or `google-services.json` in the diff

**Security**
- [ ] No `service_role` key in app code, Gradle or workflow logs
- [ ] Storage access via policies + signed URLs, no public buckets
- [ ] Health data never logged (`Log.d` of sets/weights/messages is a blocker)

**Code quality**
- [ ] No "what" comments; only "why"
- [ ] Business logic not in Composables/Activities
- [ ] `PROJECT_MANIFEST.md` updated in this PR

Report findings as:
- BLOCKER — must fix before merge
- WARNING — should fix, not a merge blocker
- SUGGESTION — optional improvement
