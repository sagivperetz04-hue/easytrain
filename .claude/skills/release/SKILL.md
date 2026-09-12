---
name: release
description: Cut an EasyTrain release — verify master is green, bump versionName, write the changelog section in PROJECT_MANIFEST.md, create the vX.Y.Z tag that triggers release.yml, and show (never run) the Play Console upload steps. User-invoked only.
disable-model-invocation: true
argument-hint: "[version e.g. 0.3.0]"
allowed-tools: Bash(git status *) Bash(git log *) Bash(git tag *) Bash(git describe *) Bash(gh run list *) Bash(gh release view *) Bash(./gradlew bundleRelease *)
---

# Release $0

Releases are tags on `master`. `release.yml` does the building and signing; this skill only
prepares and tags. Anything that pushes or uploads is shown to the user to run.

## 0. Preconditions (stop if any fails)

Working tree: !`git status --short`
Local master: !`git log --oneline -1`
Latest tag: !`git describe --tags --abbrev=0 2>/dev/null || echo v0.0.0`

- Working tree clean, on `master`, up to date with `origin/master` (`git fetch && git status`).
- Latest `master` CI run is green: run `gh run list --branch master --limit 3` and check.
- `$0` is a valid semver strictly greater than the latest tag above.

## 1. Version

`versionName` lives in **one place**: `gradle/libs.versions.toml` → `[versions] easytrain = "…"`.
Set it to `$0`. `versionCode` is computed in `release.yml` from the tag (never edited by hand).

## 2. Changelog

Add a `## v$0 — <date>` section to `PROJECT_MANIFEST.md` → *Release history*, listing merged
`ET-NNN` tickets since the previous tag. Merges since the last tag:

!`git log $(git describe --tags --abbrev=0 2>/dev/null || git rev-list --max-parents=0 HEAD)..HEAD --merges --pretty=format:'- %s'`

Update `Last updated`. Commit as `chore: release v$0`.

## 3. Local sanity build (unsigned is fine)

```bash
./gradlew bundleRelease --no-daemon
```
Must succeed with R8 — a missing keep rule shows up here, not on CI.

## 4. Tag — show, don't run

```
git push origin master
git tag -a v$0 -m "EasyTrain v$0"
git push origin v$0
```
`release.yml` then builds the signed AAB and attaches it to the GitHub Release
(`gh release view v$0` to confirm).

## 5. Play Console (manual until ET-011 automates it)

1. Download `app-release.aab` from the GitHub Release.
2. Play Console → EasyTrain → Testing → Internal testing → Create new release → upload.
3. Release notes = the changelog section above.
4. Roll out to internal testers; verify on one physical device: sign-in → hub → open trainee → log a set on the trainee device → appears on coach → chat round-trip → push received.

## Rollback

There is no server deploy to roll back; only migrations matter. Migrations are append-only and
additive, so an older APK keeps working against a newer schema. If a release must be pulled,
halt the rollout in Play Console — do not delete the tag or the GitHub Release.
