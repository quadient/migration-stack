package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.ParagraphPdfTaggingRule
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TabType
import kotlinx.serialization.Serializable

@Serializable
data class ParagraphStyleDefinitionEntity(
    val leftIndent: Size? = null,
    val rightIndent: Size? = null,
    val defaultTabSize: Size? = null,
    val spaceBefore: Size? = null,
    val spaceAfter: Size? = null,
    val alignment: Alignment = Alignment.Left,
    val firstLineIndent: Size? = null,
    val lineSpacing: LineSpacing = LineSpacing.Additional(null),
    val keepWithNextParagraph: Boolean? = null,
    val tabs: TabsEntity? = null,
    val pdfTaggingRule: ParagraphPdfTaggingRule? = null,
)

@Serializable
data class TabsEntity(val tabs: List<TabEntity>, val useOutsideTabs: Boolean)

@Serializable
data class TabEntity(val position: Size, val type: TabType)
