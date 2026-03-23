# kuneiphorm-gradle

Shared Gradle convention plugins for all [kuneiphorm](https://github.com/kuneiphorm) Java modules. Provides consistent versioning, publishing, testing, code coverage, mutation testing, and formatting across the project.

## Plugins

All plugins are precompiled Kotlin script plugins packaged in a single artifact. Consuming projects apply them individually by ID.

### `org.kuneiphorm.conventions`

Publishing and identity.

- Applies `maven-publish` and `org.kuneiphorm.versioning`.
- Sets `group` to `org.kuneiphorm`.
- Configures the Java 21 toolchain when the `java` or `java-library` plugin is present.
- Registers a `mavenJava` publication from the `java` component when the `java` or `java-library` plugin is present.

**Prerequisite:** the consuming project must apply `java` or `java-library`.

### `org.kuneiphorm.versioning`

Git-tag-based [Semantic Versioning](https://semver.org/).

The plugin derives the project version from `git describe --tags --long --always` at configuration time. The version string is fully SemVer-compliant, including pre-release (`-rc.N`) and build metadata (`+<hash>`) components.

#### Version resolution

| Git state | Resolved version | Example |
|---|---|---|
| On a release tag `vX.Y.Z` | `X.Y.Z` | `1.2.3` |
| On an RC tag `vX.Y.Z-rc.N` | `X.Y.Z-rc.N` | `1.2.3-rc.1` |
| N commits after a tag | `X.Y.Z+<hash>` | `1.2.3+a1b2c3d` |
| N commits after an RC tag | `X.Y.Z-rc.N+<hash>` | `1.2.3-rc.1+a1b2c3d` |
| No tags in repository | `0.0.0+<hash>` | `0.0.0+a1b2c3d` |
| No tags, no commits | `0.0.0+uncommitted` | `0.0.0+uncommitted` |

Build metadata (`+<hash>`) is informational only and is ignored for version precedence, as required by SemVer section 10.

#### Tasks

All tasks are registered under the `versioning` group.

| Task | Output | Description |
|---|---|---|
| `currentVersion` | `X.Y.Z[+<hash>]` | Prints the current resolved version. Use `-Pbuild=false` to strip build metadata. |
| `nextFix` | `X.Y.(Z+1)` | Prints the next patch version. |
| `nextMinor` | `X.(Y+1).0` | Prints the next minor version. |
| `nextMajor` | `(X+1).0.0` | Prints the next major version. |
| `nextRc` | `X.Y.Z-rc.(N+1)` | Bumps the current RC number. Only valid when the current version is an RC. |
| `nextRc -Ptype=fix` | `X.Y.(Z+1)-rc.1` | Starts a new RC series for the next patch. Only valid when **not** on an RC. |
| `nextRc -Ptype=minor` | `X.(Y+1).0-rc.1` | Starts a new RC series for the next minor. Only valid when **not** on an RC. |
| `nextRc -Ptype=major` | `(X+1).0.0-rc.1` | Starts a new RC series for the next major. Only valid when **not** on an RC. |

#### Release candidate rules

- `nextRc` without `-Ptype`: bumps the RC number on the current RC. Fails if the current version is not an RC.
- `nextRc -Ptype=...`: starts a new RC series from a clean release. Fails if the current version is already an RC, finalize or abandon the current RC first.

#### Typical release workflow

```
0.0.0                           Initial state (no tags)
./gradlew nextRc -Ptype=minor   → 0.1.0-rc.1
git tag v0.1.0-rc.1

./gradlew nextRc                → 0.1.0-rc.2     (fix found during RC)
git tag v0.1.0-rc.2

git tag v0.1.0                                      (RC accepted, tag directly)

./gradlew nextRc -Ptype=fix    → 0.1.1-rc.1      (hotfix cycle)
git tag v0.1.1-rc.1
```

#### `SemVer` data class

The plugin exposes `org.kuneiphorm.versioning.SemVer`, a Kotlin data class implementing `Comparable<SemVer>`. It models the full SemVer specification:

- Fields: `major`, `minor`, `patch`, `rc` (optional), `build` (optional).
- Parsing: `SemVer.parse("1.2.3-rc.1+abc")`.
- Precedence: follows SemVer section 11. Build metadata is ignored. Pre-release versions have lower precedence than the associated normal version.
- Validation: non-negative integers, RC >= 1, build metadata non-empty when present.

### `org.kuneiphorm.testing`

Unit testing and code coverage.

- Adds JUnit 5 dependencies (`junit-bom`, `junit-jupiter`, `junit-platform-launcher`).
- Configures all `Test` tasks to use JUnit Platform.
- Configures JaCoCo to produce both XML and HTML reports, finalized after each test run.

### `org.kuneiphorm.pitest`

Mutation testing.

- Applies the [PITest](https://pitest.org/) Gradle plugin with the [JUnit 5 PITest plugin](https://github.com/pitest/pitest-junit5-plugin) (`1.2.1`).
- Disables timestamped reports, output goes directly to `build/reports/pitest/`.

### `org.kuneiphorm.spotless`

Code formatting.

- Applies [Spotless](https://github.com/diffplug/spotless) with:
  - [Google Java Format](https://github.com/google/google-java-format)
  - Unused import removal
  - Trailing whitespace trimming
  - Trailing newline enforcement

## Usage

In a consuming module's `build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    id("org.kuneiphorm.conventions")
    id("org.kuneiphorm.spotless")
    id("org.kuneiphorm.testing")
    id("org.kuneiphorm.pitest")
}
```

All four plugin IDs resolve to the same `kuneiphorm-gradle` artifact. Gradle downloads it once; each `id(...)` line selects which plugin logic to apply.

## Local development

Publish to Maven Local so that other modules can resolve the plugins:

```sh
./gradlew publishToMavenLocal
```

Consuming modules must include `mavenLocal()` in their plugin resolution repositories:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

## Requirements

- Java 21+
- Git (for versioning)
