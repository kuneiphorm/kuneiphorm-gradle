package org.kuneiphorm.versioning

/**
 * Represents a [Semantic Version](https://semver.org/) with optional pre-release
 * and build metadata components.
 *
 * Pre-release is restricted to release candidate format (`rc.N`) by design.
 * Build metadata (e.g. a commit hash) is carried but ignored for precedence,
 * as required by section 10 of the SemVer specification.
 *
 * @author Florent Guille
 * @since 0.1.0
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val rc: Int? = null,
    val build: String? = null,
) : Comparable<SemVer> {

    init {
        require(major >= 0) { "Major version must be non-negative: $major" }
        require(minor >= 0) { "Minor version must be non-negative: $minor" }
        require(patch >= 0) { "Patch version must be non-negative: $patch" }
        if (rc != null) require(rc >= 1) { "Release candidate number must be >= 1: $rc" }
        if (build != null) require(build.isNotEmpty()) { "Build metadata must not be empty" }
    }

    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        if (rc != null) append("-rc.$rc")
        if (build != null) append("+$build")
    }

    /** Returns the next patch version (e.g. `1.2.3` → `1.2.4`). Drops pre-release and build metadata. */
    fun nextFix(): SemVer = SemVer(major, minor, patch + 1)

    /** Returns the next minor version (e.g. `1.2.3` → `1.3.0`). Drops pre-release and build metadata. */
    fun nextMinor(): SemVer = SemVer(major, minor + 1, 0)

    /** Returns the next major version (e.g. `1.2.3` → `2.0.0`). Drops pre-release and build metadata. */
    fun nextMajor(): SemVer = SemVer(major + 1, 0, 0)

    /**
     * Bumps the release candidate number (e.g. `1.0.0-rc.1` → `1.0.0-rc.2`).
     *
     * @throws IllegalStateException if this version is not a release candidate
     */
    fun nextRc(): SemVer {
        check(rc != null) { "Current version $this is not a release candidate" }
        return copy(rc = rc + 1, build = null)
    }

    /**
     * Compares versions according to SemVer section 11 (precedence).
     * Build metadata is ignored. Pre-release versions have lower precedence
     * than the associated normal version (e.g. `1.0.0-rc.1 < 1.0.0`).
     */
    override fun compareTo(other: SemVer): Int {
        var cmp = major.compareTo(other.major)
        if (cmp != 0) return cmp
        cmp = minor.compareTo(other.minor)
        if (cmp != 0) return cmp
        cmp = patch.compareTo(other.patch)
        if (cmp != 0) return cmp
        // Section 11: pre-release has lower precedence than normal
        return when {
            rc == null && other.rc == null -> 0
            rc == null -> 1       // this is release, other is RC → this > other
            other.rc == null -> -1 // this is RC, other is release → this < other
            else -> rc.compareTo(other.rc)
        }
    }

    companion object {
        private val PATTERN = Regex("""^(\d+)\.(\d+)\.(\d+)(?:-rc\.(\d+))?(?:\+(.+))?$""")

        /**
         * Parses a semantic version string.
         *
         * Accepted formats:
         * - `1.2.3`
         * - `1.2.3-rc.1`
         * - `1.2.3+build`
         * - `1.2.3-rc.1+build`
         *
         * @throws IllegalArgumentException if the string does not match
         */
        fun parse(version: String): SemVer {
            val match = PATTERN.matchEntire(version)
                ?: throw IllegalArgumentException("Invalid semver: $version")
            return SemVer(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt(),
                rc = match.groupValues[4].ifEmpty { null }?.toInt(),
                build = match.groupValues[5].ifEmpty { null },
            )
        }
    }
}
