package org.kuneiphorm.versioning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SemVerTest {

    // -- init validation --

    @Test
    fun `rejects negative major`() {
        assertThrows<IllegalArgumentException> { SemVer(-1, 0, 0) }
    }

    @Test
    fun `rejects negative minor`() {
        assertThrows<IllegalArgumentException> { SemVer(0, -1, 0) }
    }

    @Test
    fun `rejects negative patch`() {
        assertThrows<IllegalArgumentException> { SemVer(0, 0, -1) }
    }

    @Test
    fun `rejects rc zero`() {
        assertThrows<IllegalArgumentException> { SemVer(0, 0, 0, rc = 0) }
    }

    @Test
    fun `rejects negative rc`() {
        assertThrows<IllegalArgumentException> { SemVer(0, 0, 0, rc = -1) }
    }

    @Test
    fun `rejects empty build metadata`() {
        assertThrows<IllegalArgumentException> { SemVer(0, 0, 0, build = "") }
    }

    @Test
    fun `accepts valid version with all components`() {
        val v = SemVer(1, 2, 3, rc = 1, build = "abc123")
        assertEquals(1, v.major)
        assertEquals(2, v.minor)
        assertEquals(3, v.patch)
        assertEquals(1, v.rc)
        assertEquals("abc123", v.build)
    }

    // -- parse --

    @Test
    fun `parse release version`() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("1.2.3"))
    }

    @Test
    fun `parse ground version`() {
        assertEquals(SemVer(0, 0, 0), SemVer.parse("0.0.0"))
    }

    @Test
    fun `parse rc version`() {
        assertEquals(SemVer(1, 2, 3, rc = 1), SemVer.parse("1.2.3-rc.1"))
    }

    @Test
    fun `parse version with build metadata`() {
        assertEquals(SemVer(1, 2, 3, build = "abc123"), SemVer.parse("1.2.3+abc123"))
    }

    @Test
    fun `parse rc version with build metadata`() {
        assertEquals(SemVer(1, 2, 3, rc = 1, build = "abc123"), SemVer.parse("1.2.3-rc.1+abc123"))
    }

    @Test
    fun `parse rejects invalid input`() {
        assertThrows<IllegalArgumentException> { SemVer.parse("not-a-version") }
    }

    @Test
    fun `parse rejects non-rc prerelease`() {
        assertThrows<IllegalArgumentException> { SemVer.parse("1.2.3-alpha.1") }
    }

    // -- toString --

    @Test
    fun `toString for release`() {
        assertEquals("1.2.3", SemVer(1, 2, 3).toString())
    }

    @Test
    fun `toString for rc`() {
        assertEquals("1.2.3-rc.2", SemVer(1, 2, 3, rc = 2).toString())
    }

    @Test
    fun `toString with build metadata`() {
        assertEquals("1.2.3+abc123", SemVer(1, 2, 3, build = "abc123").toString())
    }

    @Test
    fun `toString for rc with build metadata`() {
        assertEquals("1.2.3-rc.1+abc123", SemVer(1, 2, 3, rc = 1, build = "abc123").toString())
    }

    @Test
    fun `toString round trips through parse`() {
        val versions = listOf("1.2.3", "1.2.3-rc.1", "1.2.3+abc", "1.2.3-rc.1+abc")
        for (v in versions) {
            assertEquals(v, SemVer.parse(v).toString())
        }
    }

    // -- nextFix --

    @Test
    fun `nextFix from release`() {
        assertEquals(SemVer(1, 2, 4), SemVer(1, 2, 3).nextFix())
    }

    @Test
    fun `nextFix from rc drops prerelease`() {
        assertEquals(SemVer(1, 2, 4), SemVer(1, 2, 3, rc = 1).nextFix())
    }

    @Test
    fun `nextFix drops build metadata`() {
        assertEquals(SemVer(1, 2, 4), SemVer(1, 2, 3, build = "abc").nextFix())
    }

    // -- nextMinor --

    @Test
    fun `nextMinor from release`() {
        assertEquals(SemVer(1, 3, 0), SemVer(1, 2, 3).nextMinor())
    }

    @Test
    fun `nextMinor from rc drops prerelease`() {
        assertEquals(SemVer(1, 3, 0), SemVer(1, 2, 3, rc = 1).nextMinor())
    }

    @Test
    fun `nextMinor drops build metadata`() {
        assertEquals(SemVer(1, 3, 0), SemVer(1, 2, 3, build = "abc").nextMinor())
    }

    // -- nextMajor --

    @Test
    fun `nextMajor from release`() {
        assertEquals(SemVer(2, 0, 0), SemVer(1, 2, 3).nextMajor())
    }

    @Test
    fun `nextMajor from rc drops prerelease`() {
        assertEquals(SemVer(2, 0, 0), SemVer(1, 2, 3, rc = 1).nextMajor())
    }

    @Test
    fun `nextMajor drops build metadata`() {
        assertEquals(SemVer(2, 0, 0), SemVer(1, 2, 3, build = "abc").nextMajor())
    }

    // -- nextRc --

    @Test
    fun `nextRc from rc bumps rc number`() {
        assertEquals(SemVer(1, 2, 3, rc = 2), SemVer(1, 2, 3, rc = 1).nextRc())
    }

    @Test
    fun `nextRc from rc drops build metadata`() {
        assertEquals(SemVer(1, 2, 3, rc = 2), SemVer(1, 2, 3, rc = 1, build = "abc").nextRc())
    }

    @Test
    fun `nextRc throws on non-rc version`() {
        val ex = assertThrows<IllegalStateException> { SemVer(1, 2, 3).nextRc() }
        assertTrue(ex.message!!.contains("not a release candidate"))
    }

    @Test
    fun `nextRc throws on zero version`() {
        assertThrows<IllegalStateException> { SemVer(0, 0, 0).nextRc() }
    }

    // -- compareTo --

    @Test
    fun `major version takes precedence`() {
        assertTrue(SemVer(2, 0, 0) > SemVer(1, 9, 9))
    }

    @Test
    fun `minor version takes precedence over patch`() {
        assertTrue(SemVer(1, 3, 0) > SemVer(1, 2, 9))
    }

    @Test
    fun `patch version comparison`() {
        assertTrue(SemVer(1, 2, 4) > SemVer(1, 2, 3))
    }

    @Test
    fun `equal versions compare as zero`() {
        assertEquals(0, SemVer(1, 2, 3).compareTo(SemVer(1, 2, 3)))
    }

    @Test
    fun `rc has lower precedence than release`() {
        assertTrue(SemVer(1, 2, 3, rc = 1) < SemVer(1, 2, 3))
    }

    @Test
    fun `release has higher precedence than rc`() {
        assertTrue(SemVer(1, 2, 3) > SemVer(1, 2, 3, rc = 99))
    }

    @Test
    fun `rc numbers are compared numerically`() {
        assertTrue(SemVer(1, 2, 3, rc = 2) > SemVer(1, 2, 3, rc = 1))
    }

    @Test
    fun `equal rc versions compare as zero`() {
        assertEquals(0, SemVer(1, 2, 3, rc = 1).compareTo(SemVer(1, 2, 3, rc = 1)))
    }

    @Test
    fun `build metadata is ignored for precedence`() {
        assertEquals(0, SemVer(1, 2, 3, build = "a").compareTo(SemVer(1, 2, 3, build = "b")))
    }

    // -- equals --

    @Test
    fun `data class equality includes all fields`() {
        assertEquals(SemVer(1, 2, 3, rc = 1, build = "abc"), SemVer(1, 2, 3, rc = 1, build = "abc"))
    }

    @Test
    fun `data class equality distinguishes build metadata`() {
        assertNotEquals(SemVer(1, 2, 3, build = "a"), SemVer(1, 2, 3, build = "b"))
    }
}