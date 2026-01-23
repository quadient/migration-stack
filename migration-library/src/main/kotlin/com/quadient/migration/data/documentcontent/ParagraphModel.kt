package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.ParagraphEntity
import com.quadient.migration.persistence.migrationmodel.ParagraphEntity.TextEntity
import com.quadient.migration.service.RefValidatable

data class ParagraphModel(
    val content: List<TextModel>,
    val styleRef: ParagraphStyleModelRef?,
    val displayRuleRef: DisplayRuleModelRef?
) : DocumentContentModel, RefValidatable {
    companion object {
        fun fromDb(entity: ParagraphEntity): ParagraphModel = ParagraphModel(
            styleRef = entity.styleRef?.let { ParagraphStyleModelRef.fromDb(it) },
            displayRuleRef = entity.displayRuleRef?.let { DisplayRuleModelRef.fromDb(it) },
            content = entity.content.map { TextModel.fromDb(it) })
    }

    override fun collectRefs(): List<RefModel> {
        val result = content.flatMap {
            it.collectRefs()
        }.toMutableList()

        if (styleRef != null) {
            result.add(styleRef)
        }

        if (displayRuleRef != null) {
            result.add(displayRuleRef)
        }

        return result
    }

    data class TextModel(
        val content: List<TextContentModel>,
        val styleRef: TextStyleModelRef?,
        val displayRuleRef: DisplayRuleModelRef?
    ) : RefValidatable {
        override fun collectRefs(): List<RefModel> {
            val result: MutableList<RefModel> = content.mapNotNull {
                when (it) {
                    is VariableModelRef -> listOf(it)
                    is StringModel -> null
                    is HyperlinkModel -> null
                    is TableModel -> it.collectRefs()
                    is DocumentObjectModelRef -> listOf(it)
                    is ImageModelRef -> listOf(it)
                    is FirstMatchModel -> it.collectRefs()
                }
            }.flatten().toMutableList()

            if (styleRef != null) {
                result.add(styleRef)
            }

            if (displayRuleRef != null) {
                result.add(displayRuleRef)
            }

            return result
        }

        companion object {
            fun fromDb(entity: TextEntity) = TextModel(
                styleRef = entity.styleRef?.let { TextStyleModelRef.fromDb(it) },
                displayRuleRef = entity.displayRuleRef?.let { DisplayRuleModelRef.fromDb(it) },
                content = entity.content.map { TextContentModel.fromDb(it) })
        }
    }
}
