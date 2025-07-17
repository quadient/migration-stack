package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.ParagraphStyleDefinitionEntity
import com.quadient.migration.persistence.migrationmodel.TabEntity
import com.quadient.migration.persistence.migrationmodel.TabsEntity
import com.quadient.migration.service.RefValidatable
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TabType
import kotlinx.datetime.Instant

data class ParagraphStyleModel(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override val customFields: Map<String, String>,
    override val created: Instant,
    val lastUpdated: Instant,
    val definition: ParagraphStyleDefOrRefModel,
) : RefValidatable, MigrationObjectModel {
    override fun collectRefs(): List<RefModel> {
        return when (definition) {
            is ParagraphStyleDefinitionModel -> emptyList()
            is ParagraphStyleModelRef -> listOf(definition)
        }
    }
}

data class ParagraphStyleDefinitionModel(
    val leftIndent: Size?,
    val rightIndent: Size?,
    val defaultTabSize: Size?,
    val spaceBefore: Size?,
    val spaceAfter: Size?,
    val alignment: Alignment,
    val firstLineIndent: Size?,
    val lineSpacing: LineSpacing,
    val keepWithNextParagraph: Boolean?,
    val tabs: TabsModel?,
) : ParagraphStyleDefOrRefModel {
    companion object {
        fun fromDb(entity: ParagraphStyleDefinitionEntity) = ParagraphStyleDefinitionModel(
            leftIndent = entity.leftIndent,
            rightIndent = entity.rightIndent,
            defaultTabSize = entity.defaultTabSize,
            spaceBefore = entity.spaceBefore,
            spaceAfter = entity.spaceAfter,
            alignment = entity.alignment,
            firstLineIndent = entity.firstLineIndent,
            lineSpacing = entity.lineSpacing,
            keepWithNextParagraph = entity.keepWithNextParagraph,
            tabs = entity.tabs?.let { TabsModel.fromDb(it) }
        )
    }
}

data class TabsModel(val tabs: List<TabModel>, val useOutsideTabs: Boolean) {
    companion object {
        fun fromDb(entity: TabsEntity) = TabsModel(
            tabs = entity.tabs.map { TabModel.fromDb(it) },
            useOutsideTabs = entity.useOutsideTabs
        )
    }
}

data class TabModel(val position: Size, val type: TabType) {
    companion object {
        fun fromDb(entity: TabEntity) = TabModel(position = entity.position, type = entity.type)
    }
}

