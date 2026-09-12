# EasyTrain

Android app that connects personal trainers with their clients. Offline-first (Room is the source
of truth on device), Supabase backend, no custom API server.

## Run locally

1. `cp local.properties.example local.properties`
2. `supabase start`, then put the API URL and publishable key from `supabase status` into
   `local.properties` — use `http://10.0.2.2:54321` as the URL so the emulator can reach the host.
3. Open the repo root in Android Studio (JDK 17) and run `app` on an API 26+ device.
4. `./gradlew ktlintCheck lintDebug testDebugUnitTest :app:assembleDebug` runs what CI runs.
5. `supabase test db` runs the pgTAP suite; `supabase db reset` re-applies migrations and the seed.

What to build and how: `CLAUDE.md`, `.claude/context.md`, `.claude/standards.md`, `.claude/roadmap.md`.
