package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefOrRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Tabs
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Size

class ParagraphStyleBuilder(id: String) : DtoBuilderBase<ParagraphStyle, ParagraphStyleBuilder>(id) {
    var definition: ParagraphStyleDefOrRef? = null

    fun definition(builder: ParagraphStyleDefinitionBuilder.() -> ParagraphStyleDefinitionBuilder) = apply {
        this.definition = builder(ParagraphStyleDefinitionBuilder()).build()
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
    var lineSpacingValue: Size? = null
    var lineSpacing: LineSpacing = LineSpacing.Additional
    var keepWithNextParagraph: Boolean? = null
    var tabs: Tabs? = null

    fun spaceBefore(spaceBefore: Size?) = apply { this.spaceBefore = spaceBefore }
    fun spaceAfter(spaceAfter: Size?) = apply { this.spaceAfter = spaceAfter }
    fun defaultTabSize(defaultTabSize: Size?) = apply { this.defaultTabSize = defaultTabSize }
    fun rightIndent(rightIndent: Size?) = apply { this.rightIndent = rightIndent }
    fun leftIndent(leftIndent: Size?) = apply { this.leftIndent = leftIndent }
    fun alignment(alignment: Alignment) = apply { this.alignment = alignment }
    fun firstLineIndent(firstLineIndent: Size?) = apply { this.firstLineIndent = firstLineIndent }
    fun lineSpacingValue(lineSpacingValue: Size?) = apply { this.lineSpacingValue = lineSpacingValue }
    fun lineSpacing(lineSpacing: LineSpacing) = apply { this.lineSpacing = lineSpacing }
    fun keepWithNextParagraph(keepWithNextParagraph: Boolean?) =
        apply { this.keepWithNextParagraph = keepWithNextParagraph }

    fun tabs(tabs: Tabs?) = apply { this.tabs = tabs }

    fun build() = ParagraphStyleDefinition(
        leftIndent = leftIndent,
        rightIndent = rightIndent,
        defaultTabSize = defaultTabSize,
        spaceBefore = spaceBefore,
        spaceAfter = spaceAfter,
        alignment = alignment,
        firstLineIndent = firstLineIndent,
        lineSpacingValue = lineSpacingValue,
        lineSpacing = lineSpacing,
        keepWithNextParagraph = keepWithNextParagraph,
        tabs = tabs,
    )
}
