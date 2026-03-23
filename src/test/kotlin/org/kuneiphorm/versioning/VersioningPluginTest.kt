package org.kuneiphorm.versioning

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class VersioningPluginTest {

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir.resolve("settings.gradle.kts").writeText("""
            rootProject.name = "test-project"
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                `java-library`
                id("org.kuneiphorm.versioning")
            }
        """.trimIndent())

        git("init")
        git("config", "user.email", "test@test.com")
        git("config", "user.name", "Test")
    }

    // -- currentVersion --

    @Test
    fun `no tags produces 0-0-0 with commit hash`() {
        git("commit", "--allow-empty", "-m", "initial")
        val hash = gitOutput("rev-parse", "--short", "HEAD")

        assertEquals("0.0.0+$hash", runTask("currentVersion"))
    }

    @Test
    fun `on tagged commit produces exact version`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.0.0")

        assertEquals("1.0.0", runTask("currentVersion"))
    }

    @Test
    fun `commits after tag produce version with hash`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.0.0")
        git("commit", "--allow-empty", "-m", "next")
        val hash = gitOutput("rev-parse", "--short", "HEAD")

        assertEquals("1.0.0+$hash", runTask("currentVersion"))
    }

    @Test
    fun `release candidate tag produces rc version`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.0.0-rc.1")

        assertEquals("1.0.0-rc.1", runTask("currentVersion"))
    }

    @Test
    fun `commits after rc tag produce rc version with hash`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.0.0-rc.1")
        git("commit", "--allow-empty", "-m", "next")
        val hash = gitOutput("rev-parse", "--short", "HEAD")

        assertEquals("1.0.0-rc.1+$hash", runTask("currentVersion"))
    }

    @Test
    fun `latest tag wins when multiple tags exist`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.0.0")
        git("commit", "--allow-empty", "-m", "next")
        git("tag", "v1.1.0")

        assertEquals("1.1.0", runTask("currentVersion"))
    }

    // -- nextFix / nextMinor / nextMajor --

    @Test
    fun `nextFix from release tag`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3")

        assertEquals("1.2.4", runTask("nextFix"))
    }

    @Test
    fun `nextMinor from release tag`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3")

        assertEquals("1.3.0", runTask("nextMinor"))
    }

    @Test
    fun `nextMajor from release tag`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3")

        assertEquals("2.0.0", runTask("nextMajor"))
    }

    @Test
    fun `nextFix from rc tag`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3-rc.1")

        assertEquals("1.2.4", runTask("nextFix"))
    }

    @Test
    fun `nextMinor from rc tag`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3-rc.1")

        assertEquals("1.3.0", runTask("nextMinor"))
    }

    @Test
    fun `nextMajor from rc tag`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3-rc.1")

        assertEquals("2.0.0", runTask("nextMajor"))
    }

    @Test
    fun `next versions from no tags use 0-0-0 as base`() {
        git("commit", "--allow-empty", "-m", "initial")

        assertEquals("0.0.1", runTask("nextFix"))
        assertEquals("0.1.0", runTask("nextMinor"))
        assertEquals("1.0.0", runTask("nextMajor"))
    }

    // -- nextRc without parameter --

    @Test
    fun `nextRc bumps rc number when on rc`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3-rc.1")

        assertEquals("1.2.3-rc.2", runTask("nextRc"))
    }

    @Test
    fun `nextRc errors when not on rc`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3")

        val result = runTaskAndFail("nextRc")
        assertTrue(result.contains("not a release candidate"), "Expected error about not being an RC: $result")
    }

    @Test
    fun `nextRc errors when no tags`() {
        git("commit", "--allow-empty", "-m", "initial")

        val result = runTaskAndFail("nextRc")
        assertTrue(result.contains("not a release candidate"), "Expected error about not being an RC: $result")
    }

    // -- nextRc with -Ptype parameter --

    @Test
    fun `nextRc with type fix from release tag`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3")

        assertEquals("1.2.4-rc.1", runTask("nextRc", "-Ptype=fix"))
    }

    @Test
    fun `nextRc with type minor from release tag`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3")

        assertEquals("1.3.0-rc.1", runTask("nextRc", "-Ptype=minor"))
    }

    @Test
    fun `nextRc with type major from release tag`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3")

        assertEquals("2.0.0-rc.1", runTask("nextRc", "-Ptype=major"))
    }

    @Test
    fun `nextRc with type from no tags`() {
        git("commit", "--allow-empty", "-m", "initial")

        assertEquals("0.0.1-rc.1", runTask("nextRc", "-Ptype=fix"))
        assertEquals("0.1.0-rc.1", runTask("nextRc", "-Ptype=minor"))
        assertEquals("1.0.0-rc.1", runTask("nextRc", "-Ptype=major"))
    }

    @Test
    fun `nextRc with type errors when already on rc`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3-rc.1")

        val result = runTaskAndFail("nextRc", "-Ptype=fix")
        assertTrue(result.contains("already a release candidate"), "Expected error about being on RC: $result")
    }

    @Test
    fun `nextRc with invalid type errors`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.2.3")

        val result = runTaskAndFail("nextRc", "-Ptype=bogus")
        assertTrue(result.contains("Invalid type"), "Expected invalid type error: $result")
    }

    // -- currentVersion build metadata --

    @Test
    fun `currentVersion includes build metadata by default after tag`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.0.0")
        git("commit", "--allow-empty", "-m", "next")
        val hash = gitOutput("rev-parse", "--short", "HEAD")

        assertEquals("1.0.0+$hash", runTask("currentVersion"))
    }

    @Test
    fun `currentVersion on tagged commit has no build metadata`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.0.0")

        assertEquals("1.0.0", runTask("currentVersion"))
    }

    @Test
    fun `currentVersion with build false strips metadata`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.0.0")
        git("commit", "--allow-empty", "-m", "next")

        assertEquals("1.0.0", runTask("currentVersion", "-Pbuild=false"))
    }

    @Test
    fun `currentVersion with build false on tagged commit is unchanged`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.0.0")

        assertEquals("1.0.0", runTask("currentVersion", "-Pbuild=false"))
    }

    @Test
    fun `next tasks never include build metadata`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "v1.0.0")
        git("commit", "--allow-empty", "-m", "next")

        assertEquals("1.0.1", runTask("nextFix"))
        assertEquals("1.1.0", runTask("nextMinor"))
        assertEquals("2.0.0", runTask("nextMajor"))
    }

    // -- unrecognized tag format --

    @Test
    fun `unrecognized tag format raises error`() {
        git("commit", "--allow-empty", "-m", "initial")
        git("tag", "not-a-version-tag")

        val result = runTaskAndFail("currentVersion")
        assertTrue(result.contains("does not match"), "Expected format error: $result")
    }

    // -- helpers --

    private fun runTask(task: String, vararg args: String): String {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(task, "-q", *args)
            .build()

        return result.output.trim()
    }

    private fun runTaskAndFail(task: String, vararg args: String): String {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(task, "-q", *args)
            .buildAndFail()

        return result.output
    }

    private fun git(vararg args: String) {
        ProcessBuilder("git", *args)
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()
            .waitFor()
    }

    private fun gitOutput(vararg args: String): String {
        val process = ProcessBuilder("git", *args)
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()
        process.waitFor()
        return process.inputStream.bufferedReader().readText().trim()
    }
}
