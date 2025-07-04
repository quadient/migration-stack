package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.TextStyleDefinitionModel
import com.quadient.migration.data.TextStyleModel
import com.quadient.migration.persistence.migrationmodel.TextStyleDefinitionEntity
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript

data class TextStyle(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var definition: TextStyleDefOrRef,
) : MigrationObject {
    companion object {
        fun fromModel(model: TextStyleModel) = TextStyle(
            id = model.id,
            name = model.name,
            originLocations = model.originLocations,
            customFields = CustomFieldMap(model.customFields.toMutableMap()),
            definition = TextStyleDefOrRef.fromModel(model.definition),
        )
    }
}

data class TextStyleDefinition(
    var fontFamily: String? = null,
    var foregroundColor: Color? = null,
    var size: Size? = null,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var underline: Boolean = false,
    var strikethrough: Boolean = false,
    var superOrSubscript: SuperOrSubscript = SuperOrSubscript.None,
    var interspacing: Size?,
) : TextStyleDefOrRef {
    companion object {
        fun fromModel(model: TextStyleDefinitionModel) = TextStyleDefinition(
            fontFamily = model.fontFamily,
            foregroundColor = model.foregroundColor,
            size = model.size,
            bold = model.bold,
            italic = model.italic,
            underline = model.underline,
            strikethrough = model.strikethrough,
            interspacing = model.interspacing,
            superOrSubscript = model.superOrSubscript,
        )
    }

    override fun toDb() = TextStyleDefinitionEntity(
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

    override fun toModel() = TextStyleDefinitionModel(
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