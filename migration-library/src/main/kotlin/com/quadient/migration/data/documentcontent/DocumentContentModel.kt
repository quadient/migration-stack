package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.DocumentContentEntity
import com.quadient.migration.persistence.migrationmodel.DocumentObjectEntityRef
import com.quadient.migration.persistence.migrationmodel.FlowAreaEntity
import com.quadient.migration.persistence.migrationmodel.ImageEntityRef
import com.quadient.migration.persistence.migrationmodel.ParagraphEntity
import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.service.RefValidatable

sealed interface DocumentContentModel : RefValidatable {
    companion object {
        fun fromDbContent(entity: DocumentContentEntity) = when (entity) {
            is TableEntity -> TableModel.fromDb(entity)
            is ParagraphEntity -> ParagraphModel.fromDb(entity)
            is DocumentObjectEntityRef -> DocumentObjectModelRef.fromDb(entity)
            is ImageEntityRef -> ImageModelRef.fromDb(entity)
            is FlowAreaEntity -> FlowAreaModel.fromDb(entity)
        }
    }
}

