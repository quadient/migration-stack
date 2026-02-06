package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.TextStyleDefinitionEntity
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript
import kotlinx.datetime.Instant

data class TextStyle @JvmOverloads constructor(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var definition: TextStyleDefOrRef,
    override val created: Instant? = null,
    override val lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return when (definition) {
            is TextStyleDefinition -> emptyList()
            is TextStyleRef -> listOf(definition as Ref)
        }
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
}