package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.DocumentObjectModelRef
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
                    content = it.content.map {
                    when (it) {
                        is StringModel -> StringValue.fromModel(it)
                        is VariableModelRef -> VariableRef.fromModel(it)
                        is DocumentObjectModelRef -> DocumentObjectRef.fromModel(it)
                        is TableModel -> Table.fromModel(it)
                        is ImageModelRef -> ImageRef.fromModel(it)
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
        content = content.map {
            TextEntity(it.content.map {
                when (it) {
                    is Table -> it.toDb()
                    is DocumentObjectRef -> it.toDb()
                    is ImageRef -> it.toDb()
                    is StringValue -> it.toDb()
                    is VariableRef -> it.toDb()
                }
            }.toMutableList(), it.styleRef?.toDb(), it.displayRuleRef?.toDb())
        }.toMutableList(),
    )

    data class Text(val content: List<TextContent>, val styleRef: TextStyleRef?, val displayRuleRef: DisplayRuleRef?)
}
