package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.shared.Color
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import com.quadient.migration.tools.shouldNotBeNull
import com.quadient.wfdxml.WfdXmlBuilder
import org.junit.jupiter.api.Test

class InspireBuilderUtilsTest {
    @Test
    fun `getTextStyleByName returns null for non-existent style`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.addTextStyle().setName("ExistingStyle")

        // when
        val result = getTextStyleByName(layout, "NonExistentStyle")

        // then
        result.shouldBeNull()
    }

    @Test
    fun `getTextStyleByName finds correct style among multiple`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.addTextStyle().setName("Style1")
        val targetStyle = layout.addTextStyle().setName("Style2")
        layout.addTextStyle().setName("Style3")

        // when
        val result = getTextStyleByName(layout, "Style2")

        // then
        result.shouldNotBeNull()
        result.shouldBeEqualTo(targetStyle)
    }

    @Test
    fun `getFontByName returns null for non-existent font`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.addFont().setName("Arial").setFontName("Arial")

        // when
        val result = getFontByName(layout, "TimesNewRoman")

        // then
        result.shouldBeNull()
    }

    @Test
    fun `getFontByName finds correct font among multiple`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.addFont().setName("Arial").setFontName("Arial")
        val targetFont = layout.addFont().setName("Helvetica").setFontName("Helvetica")
        layout.addFont().setName("Courier").setFontName("Courier")

        // when
        val result = getFontByName(layout, "Helvetica")

        // then
        result.shouldNotBeNull()
        result.shouldBeEqualTo(targetFont)
    }

    @Test
    fun `getImageByName returns null for non-existent image`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.addImage().setName("ExistingImage")

        // when
        val result = getImageByName(layout, "NonExistentImage")

        // then
        result.shouldBeNull()
    }

    @Test
    fun `getImageByName finds correct image among multiple`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.addImage().setName("Image1")
        val targetImage = layout.addImage().setName("Image2")
        layout.addImage().setName("Image3")

        // when
        val result = getImageByName(layout, "Image2")

        // then
        result.shouldNotBeNull()
        result.shouldBeEqualTo(targetImage)
    }

    @Test
    fun `sanitizeVariablePart basic input`() {
        val result = sanitizeVariablePart("Name-1")

        result.shouldBeEqualTo("Name_1")
    }

    @Test
    fun `sanitizeVariablePart sanitizes more complex input correctly`() {
        val result = sanitizeVariablePart("My  Special Name-1(Joe).:?!")

        result.shouldBeEqualTo("My__Special_Name_1_Joe_____")
    }

    @Test
    fun `Color resolve creates color with correct name and displayName`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        val color = Color(26, 43, 60)

        // when
        val fillStyle = color.resolve(layout)

        // then
        val layoutColor = (fillStyle as com.quadient.wfdxml.internal.layoutnodes.FillStyleImpl).color
        layoutColor.name.shouldBeEqualTo("Color R26 G43 B60")
        layoutColor.displayName.shouldBeEqualTo("Color R26 G43 B60")
    }

    @Test
    fun `Color resolve creates fillStyle with correct name and displayName`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        val color = Color(26, 43, 60)

        // when
        val fillStyle = color.resolve(layout)

        // then
        fillStyle.shouldNotBeNull()
        fillStyle!!.name.shouldBeEqualTo("FillStyle R26 G43 B60")
        fillStyle.displayName.shouldBeEqualTo("FillStyle R26 G43 B60")
    }

    @Test
    fun `Color resolve reuses existing color and fillStyle`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        val color = Color(26, 43, 60)

        // when
        val first = color.resolve(layout)
        val second = color.resolve(layout)

        // then
        first.shouldNotBeNull()
        second.shouldNotBeNull()
        first.shouldBeEqualTo(second)
    }
}
