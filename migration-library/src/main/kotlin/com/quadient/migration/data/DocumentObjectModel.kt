package com.quadient.migration.data

import com.quadient.migration.service.RefValidatable
import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import kotlinx.datetime.Instant

data class DocumentObjectModel(
    override val id: String,
    override val name: String?,
    val type: DocumentObjectType,
    val content: List<DocumentContentModel>,
    val internal: Boolean,
    val targetFolder: IcmPath? = null,
    override val originLocations: List<String>,
    override val customFields: Map<String, String>,
    override val created: Instant,
    val lastUpdated: Instant,
    val displayRuleRef: DisplayRuleModelRef? = null,
    val variableStructureRef: VariableStructureModelRef? = null,
    val baseTemplate: String?,
    val options: DocumentObjectOptions?,
) : RefValidatable, MigrationObjectModel {
    override fun collectRefs(): List<RefModel> {
        return this.content.map {
            when (it) {
                is RefModel -> listOf(it)
                is TableModel -> it.collectRefs()
                is ParagraphModel -> it.collectRefs()
                is AreaModel -> it.collectRefs()
                is FirstMatchModel -> it.collectRefs()
            }
        }.flatten() + (displayRuleRef?.let { listOf(it) } ?: emptyList())
    }

}