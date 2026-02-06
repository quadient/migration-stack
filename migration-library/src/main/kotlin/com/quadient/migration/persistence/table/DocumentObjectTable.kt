package com.quadient.migration.persistence.table

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.persistence.migrationmodel.DocumentContentEntity
import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.SkipOptions
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.json.jsonb

object DocumentObjectTable : MigrationObjectTable("document_object") {
    val type = varchar("type", 50)
    val content = jsonb<List<DocumentContentEntity>>("content", Json).nullable()
    val internal = bool("internal")
    val targetFolder = varchar("target_folder", 255).nullable()
    val displayRuleRef = varchar("display_rule_ref", 255).nullable()
    val variableStructureRef = varchar("variable_structure_ref", 255).nullable()
    val baseTemplate = varchar("base_template", 255).nullable()
    val options = jsonb<DocumentObjectOptions>("options", Json).nullable()
    val metadata = jsonb<Map<String, List<MetadataPrimitive>>>("metadata", Json)
    val skip = jsonb<SkipOptions>("skip", Json)
    val subject = varchar("subject", 255).nullable()

    fun fromResultRow(result: ResultRow): DocumentObject {
        return DocumentObject(
            id = result[id].value,
            name = result[name],
            type = DocumentObjectType.valueOf(result[type]),
            content = result[content]?.map(DocumentContent::fromDbContent) ?: emptyList(),
            internal = result[internal],
            targetFolder = result[targetFolder],
            originLocations = result[originLocations],
            customFields = CustomFieldMap(result[customFields].toMutableMap()),
            created = result[created],
            lastUpdated = result[lastUpdated],
            displayRuleRef = result[displayRuleRef]?.let { DisplayRuleRef(it) },
            variableStructureRef = result[variableStructureRef]?.let { VariableStructureRef(it) },
            baseTemplate = result[baseTemplate],
            options = result[options],
            metadata = result[metadata],
            skip = result[skip],
            subject = result[subject],
        )
    }
}
