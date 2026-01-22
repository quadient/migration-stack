package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.ParagraphPdfTaggingRule
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TabType
import kotlinx.serialization.Serializable

@Serializable
data class ParagraphStyleDefinitionEntity(
    val leftIndent: Size?,
    val rightIndent: Size?,
    val defaultTabSize: Size?,
    val spaceBefore: Size?,
    val spaceAfter: Size?,
    val alignment: Alignment,
    val firstLineIndent: Size?,
    val lineSpacing: LineSpacing,
    val keepWithNextParagraph: Boolean?,
    val tabs: TabsEntity?,
    val pdfTaggingRule: ParagraphPdfTaggingRule?,
): ParagraphStyleDefOrRefEntity

@Serializable
data class TabsEntity(val tabs: List<TabEntity>, val useOutsideTabs: Boolean)

@Serializable
data class TabEntity(val position: Size, val type: TabType)