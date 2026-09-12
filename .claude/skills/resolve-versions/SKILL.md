---
name: resolve-versions
description: Resolve the current stable version of a library (Google Maven, Maven Central, Gradle plugin portal) or the commit SHA of a GitHub Action release from the network, so versions are never written from memory. Use before editing gradle/libs.versions.toml, any GitHub workflow `uses:` line, or supabase/config.toml image tags.
argument-hint: "[library-or-action ...]"
allowed-tools: Bash(curl -s *) Bash(python3 -c *) Bash(xmllint *)
---

# Resolve versions live

Targets: `$ARGUMENTS` (if empty, resolve everything currently referenced in
`gradle/libs.versions.toml` and `.github/workflows/*.yml`).

Training-data versions are stale by definition. Every version that ends up in a file must have
been printed by one of the commands below **in this session**. Paste the resolved list into the PR
description.

## Libraries

Pick the repository by group id, fetch `maven-metadata.xml`, take `<release>` (or the highest
non-`alpha`/`beta`/`rc`/`dev` entry in `<versions>` when `<release>` is a pre-release):

| Group | Metadata URL pattern |
|---|---|
| `androidx.*`, `com.android.tools.build`, `com.google.dagger` (Hilt is on Central too), `com.google.firebase` | `https://dl.google.com/android/maven2/<group/as/path>/<artifact>/maven-metadata.xml` |
| everything else (`io.github.jan-tennert.supabase`, `io.ktor`, `org.jetbrains.kotlin*`, `io.coil-kt.coil3`, `app.cash.turbine`, `org.robolectric`, `com.google.devtools.ksp`) | `https://repo1.maven.org/maven2/<group/as/path>/<artifact>/maven-metadata.xml` |
| Gradle plugins (`org.jlleitschuh.gradle.ktlint`, `com.google.gms.google-services`) | `https://plugins.gradle.org/m2/<plugin/id/as/path>/<plugin.id>.gradle.plugin/maven-metadata.xml` |

```bash
latest() { curl -s "\$1" | python3 -c '
import sys,re
xml=sys.stdin.read()
rel=re.search(r"<release>([^<]+)</release>",xml)
vs=[v for v in re.findall(r"<version>([^<]+)</version>",xml) if not re.search(r"alpha|beta|rc|dev|snapshot",v,re.I)]
print(rel.group(1) if rel and not re.search(r"alpha|beta|rc|dev",rel.group(1),re.I) else vs[-1])'; }

latest https://dl.google.com/android/maven2/androidx/compose/compose-bom/maven-metadata.xml
latest https://repo1.maven.org/maven2/io/github/jan-tennert/supabase/bom/maven-metadata.xml
latest https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/maven-metadata.xml
```

Special cases:
- **AGP ↔ Gradle**: after resolving AGP, read the required Gradle version from
  `https://developer.android.com/build/releases/gradle-plugin` (WebFetch) and set it in
  `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl` … `-bin.zip`).
- **Kotlin ↔ KSP**: KSP left the `<kotlin>-<ksp>` scheme at 2.3.0 and now ships standalone semver
  (latest `<release>` wins), decoupled from the Kotlin version. Its plugin marker is on Maven Central,
  not the Gradle plugin portal. Older `2.2.x-2.0.x` entries still appear in the metadata — ignore them.
- **Compose compiler**: the `org.jetbrains.kotlin.plugin.compose` plugin version **equals** the Kotlin version.
- **supabase-kt ↔ Ktor**: check the supabase-kt release notes for the Ktor major it targets; use the newest Ktor of that major.
- **Room / Hilt via KSP**: no extra rule, but re-run the build after bumping — KSP2 mismatches show up only at compile time.

## GitHub Actions (commit SHA pinning)

```bash
sha_for() {   # usage: sha_for actions/checkout
  tag=$(curl -s "https://api.github.com/repos/\$1/releases/latest" | python3 -c 'import sys,json;print(json.load(sys.stdin)["tag_name"])')
  ref=$(curl -s "https://api.github.com/repos/\$1/git/ref/tags/$tag")
  type=$(echo "$ref" | python3 -c 'import sys,json;print(json.load(sys.stdin)["object"]["type"])')
  sha=$(echo "$ref"  | python3 -c 'import sys,json;print(json.load(sys.stdin)["object"]["sha"])')
  if [ "$type" = "tag" ]; then
    sha=$(curl -s "https://api.github.com/repos/\$1/git/tags/$sha" | python3 -c 'import sys,json;print(json.load(sys.stdin)["object"]["sha"])')
  fi
  echo "uses: \$1@$sha  # $tag"
}
sha_for actions/checkout; sha_for actions/setup-java; sha_for gradle/actions; sha_for supabase/setup-cli
```
Note `gradle/actions` releases cover the `setup-gradle` sub-action: write `gradle/actions/setup-gradle@<sha>`.

(`\$1` above is the shell's positional parameter, escaped so the skill's own `$N` substitution
leaves it alone — copy the functions verbatim.)

## Output

Print a table `component | source URL | resolved version/sha`, then edit the catalog/workflows.
Never round, never "bump to match", never keep a version you couldn't resolve — say so and ask.
