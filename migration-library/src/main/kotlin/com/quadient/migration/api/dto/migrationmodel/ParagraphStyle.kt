package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.ParagraphStyleDefinitionModel
import com.quadient.migration.data.ParagraphStyleModel
import com.quadient.migration.data.TabModel
import com.quadient.migration.data.TabsModel
import com.quadient.migration.persistence.migrationmodel.ParagraphStyleDefinitionEntity
import com.quadient.migration.persistence.migrationmodel.TabEntity
import com.quadient.migration.persistence.migrationmodel.TabsEntity
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TabType

data class ParagraphStyle(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var definition: ParagraphStyleDefOrRef,
) : MigrationObject {
    companion object {
        fun fromModel(model: ParagraphStyleModel) = ParagraphStyle(
            id = model.id,
            name = model.name,
            originLocations = model.originLocations,
            customFields = CustomFieldMap(model.customFields.toMutableMap()),
            definition = ParagraphStyleDefOrRef.fromModel(model.definition),
        )
    }
}

data class ParagraphStyleDefinition(
    var leftIndent: Size?,
    var rightIndent: Size?,
    var defaultTabSize: Size?,
    var spaceBefore: Size?,
    var spaceAfter: Size?,
    var alignment: Alignment = Alignment.Left,
    var firstLineIndent: Size?,
    var lineSpacing: LineSpacing = LineSpacing.Additional(null),
    var keepWithNextParagraph: Boolean?,
    var tabs: Tabs?,
) : ParagraphStyleDefOrRef {
    companion object {
        fun fromModel(model: ParagraphStyleDefinitionModel) = ParagraphStyleDefinition(
            leftIndent = model.leftIndent,
            rightIndent = model.rightIndent,
            defaultTabSize = model.defaultTabSize,
            spaceBefore = model.spaceBefore,
            spaceAfter = model.spaceAfter,
            alignment = model.alignment,
            firstLineIndent = model.firstLineIndent,
            lineSpacing = model.lineSpacing,
            keepWithNextParagraph = model.keepWithNextParagraph,
            tabs = model.tabs?.let(Tabs::fromModel),
        )
    }

    override fun toDb() = ParagraphStyleDefinitionEntity(
        leftIndent = leftIndent,
        rightIndent = rightIndent,
        defaultTabSize = defaultTabSize,
        spaceBefore = spaceBefore,
        spaceAfter = spaceAfter,
        alignment = alignment,
        firstLineIndent = firstLineIndent,
        lineSpacing = lineSpacing,
        keepWithNextParagraph = keepWithNextParagraph,
        tabs = tabs?.toDb(),
    )

    override fun toModel() = ParagraphStyleDefinitionModel(
        leftIndent = leftIndent,
        rightIndent = rightIndent,
        defaultTabSize = defaultTabSize,
        spaceBefore = spaceBefore,
        spaceAfter = spaceAfter,
        alignment = alignment,
        firstLineIndent = firstLineIndent,
        lineSpacing = lineSpacing,
        keepWithNextParagraph = keepWithNextParagraph,
        tabs = tabs?.toModel(),
    )
}

data class Tabs(val tabs: List<Tab>, val useOutsideTabs: Boolean) {
    companion object {
        fun fromModel(db: TabsModel) = Tabs(
            tabs = db.tabs.map { Tab(it.position, it.type) },
            useOutsideTabs = db.useOutsideTabs,
        )
    }

    fun toDb() = TabsEntity(
        tabs = tabs.map {
            TabEntity(position = it.position, type = it.type)
        },
        useOutsideTabs = useOutsideTabs,
    )

    fun toModel() = TabsModel(
        tabs = tabs.map {
            TabModel(position = it.position, type = it.type)
        },
        useOutsideTabs = useOutsideTabs,
    )
}

data class Tab(val position: Size, val type: TabType)