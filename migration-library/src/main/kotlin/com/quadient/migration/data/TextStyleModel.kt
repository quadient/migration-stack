package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.TextStyleDefinitionEntity
import com.quadient.migration.service.RefValidatable
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript
import kotlinx.datetime.Instant

data class TextStyleModel (
    override val id: String,
    override val name: String? = null,
    override val originLocations: List<String> = emptyList(),
    override val customFields: Map<String, String>,
    override val created: Instant,
    val lastUpdated: Instant,
    val definition: TextStyleDefOrRefModel,
) : RefValidatable, MigrationObjectModel {
    override fun collectRefs(): List<RefModel> {
        return when (definition) {
            is TextStyleDefinitionModel -> emptyList()
            is TextStyleModelRef -> listOf(definition)
        }
    }
}

data class TextStyleDefinitionModel(
    val fontFamily: String? = null,
    val foregroundColor: Color? = null,
    val size: Size? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val superOrSubscript: SuperOrSubscript,
    val interspacing: Size?,
): TextStyleDefOrRefModel {
    companion object {
        fun fromDb(entity: TextStyleDefinitionEntity) = TextStyleDefinitionModel(
            fontFamily = entity.fontFamily,
            foregroundColor = entity.foregroundColor,
            size = entity.size,
            bold = entity.bold,
            italic = entity.italic,
            underline = entity.underline,
            strikethrough = entity.strikethrough,
            superOrSubscript = entity.superOrSubscript,
            interspacing = entity.interspacing,
        )
    }
}