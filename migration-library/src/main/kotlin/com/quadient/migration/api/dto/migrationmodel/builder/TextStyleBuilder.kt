package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefOrRef
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript

class TextStyleBuilder(id: String) : DtoBuilderBase<TextStyle, TextStyleBuilder>(id) {
    var definition: TextStyleDefOrRef? = null

    fun definition(builder: TextStyleDefinitionBuilder.() -> TextStyleDefinitionBuilder) = apply {
        this.definition = builder(TextStyleDefinitionBuilder()).build()
    }

    fun definition(definition: TextStyleDefinition) = apply { this.definition = definition }
    fun styleRef(id: String) = apply { this.definition = TextStyleRef(id) }
    fun styleRef(ref: TextStyleRef) = apply { this.definition = ref }

    override fun build(): TextStyle {
        return TextStyle(
            id = id,
            name = name,
            originLocations = originLocations,
            customFields = customFields,
            definition = definition ?: throw IllegalArgumentException("TextStyleDefinition must be provided"),
        )
    }
}

class TextStyleDefinitionBuilder {
    var fontFamily: String? = null
    var foregroundColor: Color? = null
    var size: Size? = null
    var bold: Boolean = false
    var italic: Boolean = false
    var underline: Boolean = false
    var strikethrough: Boolean = false
    var interspacing: Size? = null
    var superOrSubscript: SuperOrSubscript = SuperOrSubscript.None

    fun fontFamily(fontFamily: String?) = apply { this.fontFamily = fontFamily }
    fun size(size: Size?) = apply { this.size = size }
    fun bold(bold: Boolean) = apply { this.bold = bold }
    fun italic(italic: Boolean) = apply { this.italic = italic }
    fun underline(underline: Boolean) = apply { this.underline = underline }
    fun strikethrough(strikethrough: Boolean) = apply { this.strikethrough = strikethrough }
    fun superOrSubscript(superOrSubscript: SuperOrSubscript) = apply { this.superOrSubscript = superOrSubscript }
    fun superOrSubscript(superOrSubscript: String) =
        apply { this.superOrSubscript = SuperOrSubscript.valueOf(superOrSubscript) }
    fun interspacing(interspacing: Size?) = apply { this.interspacing = interspacing }

    /**
     * @throws When any of the components are outside [0, 1] range
     */
    fun foregroundColor(red: Double, green: Double, blue: Double) =
        apply { this.foregroundColor = Color(red, green, blue) }

    /**
     * @throws When any of the components are outside [0, 255] range
     */
    fun foregroundColor(red: Int, green: Int, blue: Int) = apply { this.foregroundColor = Color(red, green, blue) }
    fun foregroundColor(hex: String) = apply { this.foregroundColor = Color.fromHex(hex) }
    fun foregroundColor(color: Color) = apply { this.foregroundColor = color }

    fun build(): TextStyleDefinition {
        return TextStyleDefinition(
            fontFamily = fontFamily,
            foregroundColor = foregroundColor,
            size = size,
            bold = bold,
            italic = italic,
            underline = underline,
            strikethrough = strikethrough,
            superOrSubscript = superOrSubscript,
            interspacing = interspacing,
        )
    }
}
