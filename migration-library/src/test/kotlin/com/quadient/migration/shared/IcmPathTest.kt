package com.quadient.migration.shared

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IcmPathTest {
    @Test
    fun root() {
        val rootPath = IcmPath.root()

        assertEquals(rootPath.toString(), "icm://")
    }

    @Test
    fun `from replaces vcs with icm`() {
        val path = IcmPath.from("vcs://BaseTemplate.wfd")

        assertEquals(path.toString(), "icm://BaseTemplate.wfd")
    }

    @Test
    fun `from replaces backslashes with forward slashes`() {
        val path = IcmPath.from("vcs:\\\\Templates\\BaseTemplate.wfd")

        assertEquals(path.toString(), "icm://Templates/BaseTemplate.wfd")
    }

    @Test
    fun `from removes leading and trailing slashes`() {
        val path = IcmPath.from("/BaseTemplate.wfd/")

        assertEquals(path.toString(), "BaseTemplate.wfd")
    }

    @Test
    fun `is absolute returns true for absolute paths`() {
        val path = IcmPath.from("icm://BaseTemplate.wfd")

        assertEquals(path.isAbsolute(), true)
    }

    @Test
    fun `is absolute returns false for relative paths`() {
        val path = IcmPath.from("BaseTemplate.wfd")

        assertEquals(path.isAbsolute(), false)
    }

    @Test
    fun `join when other is null`() {
        val path = IcmPath.from("icm://dir")
        val joinedPath = path.join(null as String?)
        val joinedPath2 = path.join(null as IcmPath?)

        assertEquals(joinedPath.toString(), "icm://dir")
        assertEquals(joinedPath2.toString(), "icm://dir")
    }

    @Test
    fun `join when other is blank`() {
        val path = IcmPath.from("icm://dir")
        val joinedPath = path.join("")
        val joinedPath2 = path.join(IcmPath.from(""))

        assertEquals(joinedPath.toString(), "icm://dir")
        assertEquals(joinedPath2.toString(), "icm://dir")
    }

    @Test
    fun `join when other is absolute`() {
        val path = IcmPath.from("icm://dir")

        val ex = assertThrows<IllegalArgumentException> { path.join("icm://otherDir") }

        assertEquals(ex.message, "Cannot join with absolute path 'icm://otherDir'")
    }

    @Test
    fun `is null or blank`() {
        val path: IcmPath? = null
        val path2: IcmPath? = IcmPath.from("")
        val path3: IcmPath? = IcmPath.from("icm://")

        assertEquals(path.isNullOrBlank(), true)
        assertEquals(path2.isNullOrBlank(), true)
        assertEquals(path3.isNullOrBlank(), false)
    }

    @Test
    fun `orDefault returns default when null`() {
        val path: IcmPath? = null
        val defaultPath = IcmPath.from("icm://default")

        assertEquals(path.orDefault(defaultPath).toString(), "icm://default")
    }

    @Test
    fun `string to icm path`() {
        val path = "icm://BaseTemplate.wfd".toIcmPath()

        assertEquals(path, IcmPath.from("icm://BaseTemplate.wfd"))
    }
}