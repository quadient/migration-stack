package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.DocumentContentEntity
import com.quadient.migration.persistence.migrationmodel.DocumentObjectEntityRef
import com.quadient.migration.persistence.migrationmodel.FileEntityRef
import com.quadient.migration.persistence.migrationmodel.FirstMatchEntity
import com.quadient.migration.persistence.migrationmodel.AreaEntity
import com.quadient.migration.persistence.migrationmodel.ImageEntityRef
import com.quadient.migration.persistence.migrationmodel.ParagraphEntity
import com.quadient.migration.persistence.migrationmodel.SelectByLanguageEntity
import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.service.RefValidatable

sealed interface DocumentContentModel : RefValidatable {
    companion object {
        fun fromDbContent(entity: DocumentContentEntity) = when (entity) {
            is TableEntity -> TableModel.fromDb(entity)
            is ParagraphEntity -> ParagraphModel.fromDb(entity)
            is DocumentObjectEntityRef -> DocumentObjectModelRef.fromDb(entity)
            is ImageEntityRef -> ImageModelRef.fromDb(entity)
            is FileEntityRef -> FileModelRef.fromDb(entity)
            is AreaEntity -> AreaModel.fromDb(entity)
            is FirstMatchEntity -> FirstMatchModel.fromDb(entity)
            is SelectByLanguageEntity -> SelectByLanguageModel.fromDb(entity)
        }
    }
}

