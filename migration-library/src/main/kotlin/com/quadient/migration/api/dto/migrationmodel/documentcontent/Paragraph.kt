package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.FileModelRef
import com.quadient.migration.data.FirstMatchModel
import com.quadient.migration.data.HyperlinkModel
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphModel
import com.quadient.migration.data.StringModel
import com.quadient.migration.data.TableModel
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.persistence.migrationmodel.ParagraphEntity
import com.quadient.migration.persistence.migrationmodel.ParagraphEntity.TextEntity

data class Paragraph(
    val content: List<Text>,
    val styleRef: ParagraphStyleRef?,
    val displayRuleRef: DisplayRuleRef?
) : DocumentContent {
    companion object {
        fun fromModel(model: ParagraphModel) = Paragraph(
            styleRef = model.styleRef?.let { ParagraphStyleRef.fromModel(it) },
            displayRuleRef = model.displayRuleRef?.let { DisplayRuleRef.fromModel(it) },
            content = model.content.map {
                Text(
                    styleRef = it.styleRef?.let { TextStyleRef.fromModel(it) },
                    displayRuleRef = it.displayRuleRef?.let { DisplayRuleRef.fromModel(it) },
                    content = it.content.map { textContent ->
                        when (textContent) {
                            is StringModel -> StringValue.fromModel(textContent)
                            is VariableModelRef -> VariableRef.fromModel(textContent)
                            is DocumentObjectModelRef -> DocumentObjectRef.fromModel(textContent)
                            is TableModel -> Table.fromModel(textContent)
                            is ImageModelRef -> ImageRef.fromModel(textContent)
                            is FileModelRef -> FileRef.fromModel(textContent)
                            is FirstMatchModel -> FirstMatch.fromModel(textContent)
                            is HyperlinkModel -> Hyperlink.fromModel(textContent)
                        }
                    })
            },
        )
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
                    is ImageRef -> textContent.toDb()
                    is FileRef -> textContent.toDb()
                    is StringValue -> textContent.toDb()
                    is VariableRef -> textContent.toDb()
                    is FirstMatch -> textContent.toDb()
                    is Hyperlink -> textContent.toDb()
                }
            }.toMutableList(), it.styleRef?.toDb(), it.displayRuleRef?.toDb())
        }.toMutableList(),
    )

    data class Text(val content: List<TextContent>, val styleRef: TextStyleRef?, val displayRuleRef: DisplayRuleRef?)
}
