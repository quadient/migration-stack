package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.DocumentContentEntity
import com.quadient.migration.persistence.migrationmodel.AreaEntity
import com.quadient.migration.persistence.migrationmodel.FirstMatchEntity
import com.quadient.migration.persistence.migrationmodel.ParagraphEntity
import com.quadient.migration.persistence.migrationmodel.SelectByLanguageEntity
import com.quadient.migration.persistence.migrationmodel.TableEntity
import com.quadient.migration.persistence.migrationmodel.DocumentObjectEntityRef
import com.quadient.migration.persistence.migrationmodel.ImageEntityRef
import com.quadient.migration.persistence.migrationmodel.AttachmentEntityRef
import com.quadient.migration.persistence.migrationmodel.RepeatedContentEntity
import com.quadient.migration.persistence.migrationmodel.VariableStringContentEntity

sealed interface DocumentContent {
    companion object {
        fun fromDbContent(entity: DocumentContentEntity): DocumentContent = when (entity) {
            is TableEntity -> Table.fromDb(entity)
            is ParagraphEntity -> Paragraph.fromDb(entity)
            is DocumentObjectEntityRef -> DocumentObjectRef.fromDb(entity)
            is ImageEntityRef -> ImageRef.fromDb(entity)
            is AttachmentEntityRef -> AttachmentRef.fromDb(entity)
            is AreaEntity -> Area.fromDb(entity)
            is FirstMatchEntity -> FirstMatch.fromDb(entity)
            is SelectByLanguageEntity -> SelectByLanguage.fromDb(entity)
            is RepeatedContentEntity -> RepeatedContent.fromDb(entity)
            is VariableStringContentEntity -> VariableStringContent.fromDb(entity)
        }
    }
}

fun List<DocumentContent>.toDb(): List<DocumentContentEntity> {
    return this.map {
        when (it) {
            is Table -> it.toDb()
            is Paragraph -> it.toDb()
            is Area -> it.toDb()
            is FirstMatch -> it.toDb()
            is SelectByLanguage -> it.toDb()
            is RepeatedContent -> it.toDb()
            is VariableStringContent -> it.toDb()
            is DocumentObjectRef -> it.toDb()
            is ResourceRef -> it.toDb()
        }
    }
}