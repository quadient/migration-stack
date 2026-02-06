package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.ParagraphStyleDefinitionEntity
import com.quadient.migration.persistence.migrationmodel.TabEntity
import com.quadient.migration.persistence.migrationmodel.TabsEntity
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.ParagraphPdfTaggingRule
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TabType
import kotlinx.datetime.Instant

data class ParagraphStyle @JvmOverloads constructor(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var definition: ParagraphStyleDefOrRef,
    override val created: Instant? = null,
    override val lastUpdated: Instant? = null,
) : MigrationObject, RefValidatable {
    override fun collectRefs(): List<Ref> {
        return when (definition) {
            is ParagraphStyleDefinition -> emptyList()
            is ParagraphStyleRef -> listOf(definition as Ref)
        }
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
    var pdfTaggingRule: ParagraphPdfTaggingRule?,
) : ParagraphStyleDefOrRef {
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
        pdfTaggingRule = pdfTaggingRule,
    )
}

data class Tabs(val tabs: List<Tab>, val useOutsideTabs: Boolean) {
    fun toDb() = TabsEntity(
        tabs = tabs.map {
            TabEntity(position = it.position, type = it.type)
        },
        useOutsideTabs = useOutsideTabs,
    )
}

data class Tab(val position: Size, val type: TabType)