package com.quadient.migration.persistence.table

import com.quadient.migration.data.DisplayRuleModelRef
import com.quadient.migration.data.DocumentContentModel
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.persistence.migrationmodel.DocumentContentEntity
import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.json.jsonb

object DocumentObjectTable : MigrationObjectTable("document_object") {
    val type = varchar("type", 50)
    val content = jsonb<List<DocumentContentEntity>>("content", Json).nullable()
    val internal = bool("internal")
    val targetFolder = varchar("target_folder", 255).nullable()
    val displayRuleRef = varchar("display_rule_ref", 255).nullable()
    val baseTemplate = varchar("base_template", 255).nullable()
    val options = jsonb<DocumentObjectOptions>("options", Json).nullable()

    fun fromResultRow(result: ResultRow): DocumentObjectModel {
        return DocumentObjectModel(
            id = result[id].value,
            name = result[name],
            type = DocumentObjectType.valueOf(result[type]),
            content = result[content]?.map(DocumentContentModel::fromDbContent) ?: emptyList(),
            internal = result[internal],
            targetFolder = result[targetFolder],
            originLocations = result[originLocations],
            customFields = result[customFields],
            created = result[created],
            lastUpdated = result[lastUpdated],
            displayRuleRef = result[displayRuleRef]?.let { DisplayRuleModelRef(it) },
            baseTemplate = result[baseTemplate],
            options = result[options],
        )
    }
}
