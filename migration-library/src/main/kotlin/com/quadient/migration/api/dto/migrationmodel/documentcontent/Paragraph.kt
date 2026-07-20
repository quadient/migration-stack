package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.ParagraphEntity
import com.quadient.migration.persistence.migrationmodel.ParagraphEntity.TextEntity

data class Paragraph(
    val content: List<Text>,
    val styleRef: ParagraphStyleRef?,
    val displayRuleRef: DisplayRuleRef?
) : DocumentContent, RefValidatable {
    override val pathName = "par"
    override fun toPreview(nameResolver: (DocumentContent) -> String?): String {
        val preview = content.flatMap { it.content }.joinToString("") {
            when (it) {
                is DocumentContent -> it.toPreview(nameResolver)
                is Hyperlink -> it.displayText ?: it.url
            }
        }.replace('\r', ' ').replace('\n', ' ').take(50)
        return "$pathName: $preview"
    }

    override fun collectRefs(): Set<Ref> {
        return (content.flatMap { it.collectRefs() } + listOfNotNull(styleRef, displayRuleRef)).toSet()
    }

    companion object {
        fun fromDb(entity: ParagraphEntity): Paragraph = Paragraph(
            styleRef = entity.styleRef?.let { ParagraphStyleRef.fromDb(it) },
            displayRuleRef = entity.displayRuleRef?.let { DisplayRuleRef.fromDb(it) },
            content = entity.content.map { Text.fromDb(it) })
    }

    constructor(ref: VariableRef) : this(
        styleRef = null,
        displayRuleRef = null,
        content = listOf(
            Text(
                styleRef = null,
                displayRuleRef = null,
                content = listOf(ref)
            )
        )
    )

    constructor(simpleValues: List<String>) : this(
        styleRef = null,
        displayRuleRef = null,
        content = listOf(
            Text(
                styleRef = null,
                displayRuleRef = null,
                content = simpleValues.map { StringValue(it) }
            )
        )
    )

    constructor(simpleValue: String) : this(listOf(simpleValue))

    fun toDb(): ParagraphEntity = ParagraphEntity(
        styleRef = styleRef?.toDb(),
        displayRuleRef = displayRuleRef?.toDb(),
        content = content.map { it ->
            TextEntity(it.content.map { textContent ->
                when (textContent) {
                    is Table -> textContent.toDb()
                    is DocumentObjectRef -> textContent.toDb()
                    is ResourceRef -> textContent.toDb()
                    is StringValue -> textContent.toDb()
                    is VariableRef -> textContent.toDb()
                    is FirstMatch -> textContent.toDb()
                    is Hyperlink -> textContent.toDb()
                    is ColumnLayout -> textContent.toDb()
                    is Barcode -> textContent.toDb()
                }
            }.toMutableList(), it.styleRef?.toDb(), it.displayRuleRef?.toDb())
        }.toMutableList(),
    )

    data class Text(val content: List<TextContent>, val styleRef: TextStyleRef?, val displayRuleRef: DisplayRuleRef?) : RefValidatable {
        override fun collectRefs(): Set<Ref> {
            return (content.flatMap {
                when (it) {
                    is RefValidatable -> it.collectRefs()
                    else -> emptyList()
                }
            } + listOfNotNull(styleRef, displayRuleRef)).toSet()
        }

        companion object {
            fun fromDb(entity: TextEntity) = Text(
                styleRef = entity.styleRef?.let { TextStyleRef.fromDb(it) },
                displayRuleRef = entity.displayRuleRef?.let { DisplayRuleRef.fromDb(it) },
                content = entity.content.map { TextContent.fromDb(it) })
        }
    }
}
