package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.DocumentContentModel
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.FlowAreaModel
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphModel
import com.quadient.migration.data.TableModel
import com.quadient.migration.persistence.migrationmodel.DocumentContentEntity

sealed interface DocumentContent {
    companion object {
        fun fromModelContent(model: DocumentContentModel) = when (model) {
            is TableModel -> Table.fromModel(model)
            is ParagraphModel -> Paragraph.fromModel(model)
            is DocumentObjectModelRef -> DocumentObjectRef.fromModel(model)
            is ImageModelRef -> ImageRef.fromModel(model)
            is FlowAreaModel -> FlowArea.fromModel(model)
        }
    }
}

fun List<DocumentContent>.toDb(): List<DocumentContentEntity> {
    return this.map {
        when (it) {
            is Table -> it.toDb()
            is Paragraph -> it.toDb()
            is DocumentObjectRef -> it.toDb()
            is ImageRef -> it.toDb()
            is FlowArea -> it.toDb()
        }
    }
}