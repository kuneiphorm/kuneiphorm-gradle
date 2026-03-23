import org.kuneiphorm.versioning.PrintVersionTask
import org.kuneiphorm.versioning.SemVer

val gitDescribe = providers.exec {
    commandLine("git", "describe", "--tags", "--long", "--always")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }

val gitTagMatch = Regex("""^v(\d+\.\d+\.\d+(?:-rc\.\d+)?)-(\d+)-g([0-9a-f]+)$""")
val gitBareHashMatch = Regex("""^[0-9a-f]+$""")

val baseVersion: SemVer = run {
    val raw = gitDescribe.get()

    if (raw.isEmpty() || gitBareHashMatch.matches(raw)) {
        return@run SemVer(0, 0, 0, build = raw.ifEmpty { "uncommitted" })
    }

    val match = gitTagMatch.matchEntire(raw)
        ?: error("git describe output does not match any known tag format: $raw")

    val semver = SemVer.parse(match.groupValues[1])
    val commitCount = match.groupValues[2].toInt()
    val commitHash = match.groupValues[3]

    if (commitCount == 0) semver else semver.copy(build = commitHash)
}

fun currentVersionString(): String {
    val excludeBuild = project.findProperty("build")?.toString()?.toBooleanStrictOrNull() == false
    if (excludeBuild) return baseVersion.copy(build = null).toString()
    return baseVersion.toString()
}

version = baseVersion.toString()

tasks.register<PrintVersionTask>("currentVersion") {
    description = "Prints the current project version. Use -Pbuild=false to strip build metadata."
    group = "versioning"
    version.set(provider { currentVersionString() })
}

tasks.register<PrintVersionTask>("nextFix") {
    description = "Prints the next fix (patch) version"
    group = "versioning"
    version.set(provider { baseVersion.nextFix().toString() })
}

tasks.register<PrintVersionTask>("nextMinor") {
    description = "Prints the next minor version"
    group = "versioning"
    version.set(provider { baseVersion.nextMinor().toString() })
}

tasks.register<PrintVersionTask>("nextMajor") {
    description = "Prints the next major version"
    group = "versioning"
    version.set(provider { baseVersion.nextMajor().toString() })
}

tasks.register<PrintVersionTask>("nextRc") {
    description = "Prints the next release candidate version. Use -Ptype=fix|minor|major to start a new RC series."
    group = "versioning"
    version.set(provider {
        val type = project.findProperty("type")?.toString()
        if (type != null) {
            if (baseVersion.rc != null) {
                error("Current version $baseVersion is already a release candidate — finalize or abandon it before starting a new RC series")
            }
            val target = when (type) {
                "fix" -> baseVersion.nextFix()
                "minor" -> baseVersion.nextMinor()
                "major" -> baseVersion.nextMajor()
                else -> error("Invalid type '$type' — expected fix, minor, or major")
            }
            target.copy(rc = 1).toString()
        } else {
            baseVersion.nextRc().toString()
        }
    })
}
