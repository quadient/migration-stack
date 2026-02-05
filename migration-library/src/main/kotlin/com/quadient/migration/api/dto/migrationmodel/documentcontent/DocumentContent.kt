package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.DocumentContentEntity
import com.quadient.migration.persistence.migrationmodel.AreaEntity
import com.quadient.migration.persistence.migrationmodel.FirstMatchEntity
import com.quadient.migration.persistence.migrationmodel.ParagraphEntity
import com.quadient.migration.persistence.migrationmodel.SelectByLanguageEntity
import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.persistence.migrationmodel.DocumentObjectEntityRef
import com.quadient.migration.persistence.migrationmodel.ImageEntityRef
import com.quadient.migration.persistence.migrationmodel.FileEntityRef

sealed interface DocumentContent {
    companion object {
        fun fromDbContent(entity: DocumentContentEntity): DocumentContent = when (entity) {
            is TableEntity -> Table.fromDb(entity)
            is ParagraphEntity -> Paragraph.fromDb(entity)
            is DocumentObjectEntityRef -> DocumentObjectRef.fromDb(entity)
            is ImageEntityRef -> ImageRef.fromDb(entity)
            is FileEntityRef -> FileRef.fromDb(entity)
            is AreaEntity -> Area.fromDb(entity)
            is FirstMatchEntity -> FirstMatch.fromDb(entity)
            is SelectByLanguageEntity -> SelectByLanguage.fromDb(entity)
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
            is FileRef -> it.toDb()
            is Area -> it.toDb()
            is FirstMatch -> it.toDb()
            is SelectByLanguage -> it.toDb()
        }
    }
}