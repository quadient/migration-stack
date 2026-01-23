package com.quadient.migration.service.inspirebuilder

import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.migration.tools.shouldBeEqualTo
import com.quadient.migration.tools.shouldBeNull
import com.quadient.migration.tools.shouldNotBeNull
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
    fun `getColorByRGB returns null for non-existent color`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.addColor().setRGB(255, 0, 0) // Red

        // when
        val result = getColorByRGB(layout, 0, 255, 0) // Looking for green

        // then
        result.shouldBeNull()
    }

    @Test
    fun `getColorByRGB finds correct color among multiple`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.addColor().setRGB(255, 0, 0) // Red
        val targetColor = layout.addColor().setRGB(0, 255, 0) // Green
        layout.addColor().setRGB(0, 0, 255) // Blue

        // when
        val result = getColorByRGB(layout, 0, 255, 0)

        // then
        result.shouldNotBeNull()
        result.shouldBeEqualTo(targetColor)
    }

    @Test
    fun `getFillStyleByColor returns null for non-existent fill style`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        val color1 = layout.addColor().setRGB(255, 0, 0)
        layout.addFillStyle().setColor(color1)

        val color2 = layout.addColor().setRGB(0, 255, 0)

        // when
        val result = getFillStyleByColor(layout, color2)

        // then
        result.shouldBeNull()
    }

    @Test
    fun `getFillStyleByColor finds correct fill style among multiple`() {
        // given
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        val color1 = layout.addColor().setRGB(255, 0, 0)
        val color2 = layout.addColor().setRGB(0, 255, 0)
        val color3 = layout.addColor().setRGB(0, 0, 255)

        layout.addFillStyle().setColor(color1)
        val targetFillStyle = layout.addFillStyle().setColor(color2)
        layout.addFillStyle().setColor(color3)

        // when
        val result = getFillStyleByColor(layout, color2)

        // then
        result.shouldNotBeNull()
        result.shouldBeEqualTo(targetFillStyle)
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
}
