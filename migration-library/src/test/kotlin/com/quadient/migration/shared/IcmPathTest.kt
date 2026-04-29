package com.quadient.migration.shared

import kotlinx.serialization.json.Json
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
    fun `extension replaces existing extension`() {
        val path = IcmPath.from("icm://Templates/BaseTemplate.wfd")

        assertEquals(path.extension("jld").toString(), "icm://Templates/BaseTemplate.jld")
    }

    @Test
    fun `extension replaces existing extension when prefixed with dot`() {
        val path = IcmPath.from("icm://Templates/BaseTemplate.wfd")

        assertEquals(path.extension(".jld").toString(), "icm://Templates/BaseTemplate.jld")
    }

    @Test
    fun `extension appends when no extension exists`() {
        val path = IcmPath.from("icm://Templates/BaseTemplate")

        assertEquals(path.extension("wfd").toString(), "icm://Templates/BaseTemplate.wfd")
    }

    @Test
    fun `extension replaces only the last extension`() {
        val path = IcmPath.from("icm://Templates/Base.Template.wfd")

        assertEquals(path.extension("jld").toString(), "icm://Templates/Base.Template.jld")
    }

    @Test
    fun `string to icm path`() {
        val path = "icm://BaseTemplate.wfd".toIcmPath()

        assertEquals(path, IcmPath.from("icm://BaseTemplate.wfd"))
    }

    @Test
    fun `round-trip kotlinx serialization`() {
        val original = IcmPath.from("icm://test/dir/template.wfd")

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<IcmPath>(json)

        assertEquals(original, deserialized)
    }

    @Test
    fun `equals returns true for same IcmPath instances`() {
        val path1 = IcmPath.from("icm://Templates/Base.wfd")
        val path2 = IcmPath.from("icm://Templates/Base.wfd")

        assertEquals(true, path1 == path2)
        assertEquals(true, path1.equals(path2))
    }

    @Test
    fun `equals returns false for different IcmPath instances`() {
        val path1 = IcmPath.from("icm://Templates/Base.wfd")
        val path2 = IcmPath.from("icm://Templates/Other.wfd")

        assertEquals(false, path1 == path2)
    }

    @Test
    fun `equals returns true when compared with matching String`() {
        val path = IcmPath.from("icm://Templates/Base.wfd")

        assertEquals(true, path.equals("icm://Templates/Base.wfd"))
    }

    @Test
    fun `equals returns false when compared with non-matching String`() {
        val path = IcmPath.from("icm://Templates/Base.wfd")

        assertEquals(false, path.equals("icm://Templates/Other.wfd"))
    }

    @Test
    fun `equals returns false when compared with null`() {
        val path = IcmPath.from("icm://Templates/Base.wfd")

        assertEquals(false, path.equals(null))
    }

    @Test
    fun `equals returns false when compared with unrelated type`() {
        val path = IcmPath.from("icm://Templates/Base.wfd")

        assertEquals(false, path.equals(42))
    }

    @Test
    fun `equals after normalization from vcs`() {
        val fromVcs = IcmPath.from("vcs://Templates/Base.wfd")
        val fromIcm = IcmPath.from("icm://Templates/Base.wfd")

        assertEquals(true, fromVcs == fromIcm)
    }

    @Test
    fun `hashCode is consistent for equal IcmPath instances`() {
        val path1 = IcmPath.from("icm://Templates/Base.wfd")
        val path2 = IcmPath.from("icm://Templates/Base.wfd")

        assertEquals(path1.hashCode(), path2.hashCode())
    }

    @Test
    fun `hashCode differs for different paths`() {
        val path1 = IcmPath.from("icm://Templates/Base.wfd")
        val path2 = IcmPath.from("icm://Templates/Other.wfd")

        assertEquals(false, path1.hashCode() == path2.hashCode())
    }
}