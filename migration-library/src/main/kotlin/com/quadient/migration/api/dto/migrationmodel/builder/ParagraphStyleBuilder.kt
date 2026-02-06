package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefOrRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Tabs
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.ParagraphPdfTaggingRule
import com.quadient.migration.shared.Size

class ParagraphStyleBuilder(id: String) : DtoBuilderBase<ParagraphStyle, ParagraphStyleBuilder>(id) {
    var definition: ParagraphStyleDefOrRef? = null

    fun definition(builder: ParagraphStyleDefinitionBuilder.() -> Unit) = apply {
        this.definition = ParagraphStyleDefinitionBuilder().apply(builder).build()
    }

    fun definition(definition: ParagraphStyleDefinition) = apply { this.definition = definition }

    fun styleRef(id: String) = apply { this.definition = ParagraphStyleRef(id) }
    fun styleRef(ref: ParagraphStyleRef) = apply { this.definition = ref }

    override fun build(): ParagraphStyle {
        return ParagraphStyle(
            id = id,
            name = name,
            originLocations = originLocations,
            customFields = customFields,
            definition = definition ?: throw IllegalArgumentException("ParagraphStyleDefinition must be provided"),
        )
    }
}

class ParagraphStyleDefinitionBuilder {
    var leftIndent: Size? = null
    var rightIndent: Size? = null
    var defaultTabSize: Size? = null
    var spaceBefore: Size? = null
    var spaceAfter: Size? = null
    var alignment: Alignment = Alignment.Left
    var firstLineIndent: Size? = null
    var lineSpacing: LineSpacing = LineSpacing.Additional(null)
    var keepWithNextParagraph: Boolean? = null
    var tabs: Tabs? = null
    var pdfTaggingRule: ParagraphPdfTaggingRule? = null

    fun spaceBefore(spaceBefore: Size?) = apply { this.spaceBefore = spaceBefore }
    fun spaceAfter(spaceAfter: Size?) = apply { this.spaceAfter = spaceAfter }
    fun defaultTabSize(defaultTabSize: Size?) = apply { this.defaultTabSize = defaultTabSize }
    fun rightIndent(rightIndent: Size?) = apply { this.rightIndent = rightIndent }
    fun leftIndent(leftIndent: Size?) = apply { this.leftIndent = leftIndent }
    fun alignment(alignment: Alignment) = apply { this.alignment = alignment }
    fun firstLineIndent(firstLineIndent: Size?) = apply { this.firstLineIndent = firstLineIndent }

    fun lineSpacing(lineSpacing: LineSpacing) = apply { this.lineSpacing = lineSpacing }
    fun additionalLineSpacing(size: Size) = apply { this.lineSpacing = LineSpacing.Additional(size) }
    fun exactLineSpacing(size: Size) = apply { this.lineSpacing = LineSpacing.Exact(size) }
    fun atLeastLineSpacing(size: Size) = apply { this.lineSpacing = LineSpacing.AtLeast(size) }
    fun multipleOfLineSpacing(value: Double) = apply { this.lineSpacing = LineSpacing.MultipleOf(value) }
    fun exactFromPreviousLineSpacing(size: Size) = apply { this.lineSpacing = LineSpacing.ExactFromPrevious(size) }
    fun exactFromPreviousWithAdjustLegacyLineSpacing(size: Size) =
        apply { this.lineSpacing = LineSpacing.ExactFromPreviousWithAdjustLegacy(size) }

    fun exactFromPreviousWithAdjustLineSpacing(size: Size) =
        apply { this.lineSpacing = LineSpacing.ExactFromPreviousWithAdjust(size) }

    fun keepWithNextParagraph(keepWithNextParagraph: Boolean?) =
        apply { this.keepWithNextParagraph = keepWithNextParagraph }

    fun tabs(tabs: Tabs?) = apply { this.tabs = tabs }

    fun pdfTaggingRule(pdfTaggingRule: ParagraphPdfTaggingRule?) = apply { this.pdfTaggingRule = pdfTaggingRule }

    fun build() = ParagraphStyleDefinition(
        leftIndent = leftIndent,
        rightIndent = rightIndent,
        defaultTabSize = defaultTabSize,
        spaceBefore = spaceBefore,
        spaceAfter = spaceAfter,
        alignment = alignment,
        firstLineIndent = firstLineIndent,
        lineSpacing = lineSpacing,
        keepWithNextParagraph = keepWithNextParagraph,
        tabs = tabs,
        pdfTaggingRule = pdfTaggingRule,
    )
}
