package com.quadient.migration.service.ipsclient

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VersionTest {
    @Test
    fun `same version`() {
        val v1 = Version(1, 2, 3, 4)
        val v2 = Version(1, 2, 3, 4)

        assertEquals(0, v1.compareTo(v2))
    }

    @Test
    fun `major version differences`() {
        val v1 = Version(2, 0, 0, 0)
        val v2 = Version(1, 0, 0, 0)

        assertEquals(1, v1.compareTo(v2))
        assertEquals(-1, v2.compareTo(v1))
    }

    @Test
    fun `minor version differences`() {
        val v1 = Version(1, 3, 0, 0)
        val v2 = Version(1, 2, 0, 0)

        assertEquals(1, v1.compareTo(v2))
        assertEquals(-1, v2.compareTo(v1))
    }

    @Test
    fun `patch version differences`() {
        val v1 = Version(1, 2, 4, 0)
        val v2 = Version(1, 2, 3, 0)

        assertEquals(1, v1.compareTo(v2))
        assertEquals(-1, v2.compareTo(v1))
    }

    @Test
    fun `revision version differences`() {
        val v1 = Version(1, 2, 3, 5)
        val v2 = Version(1, 2, 3, 4)

        assertEquals(1, v1.compareTo(v2))
        assertEquals(-1, v2.compareTo(v1))
    }
}