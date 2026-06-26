package com.quadient.migration.api.repository

import com.quadient.migration.api.ProjectName
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.Mapping
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.persistence.migrationmodel.MappingItemEntity
import com.quadient.migration.persistence.repository.MappingInternalRepository
import com.quadient.migration.persistence.table.AttachmentTable
import com.quadient.migration.persistence.table.DisplayRuleTable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ImageTable
import com.quadient.migration.persistence.table.MigrationObjectTable
import com.quadient.migration.persistence.table.ParagraphStyleTable
import com.quadient.migration.persistence.table.TextStyleTable
import com.quadient.migration.persistence.table.VariableStructureTable
import com.quadient.migration.persistence.table.VariableTable
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import com.quadient.migration.tools.logger

class MappingRepository(
    private val projectName: ProjectName,
    private val documentObjectRepository: DocumentObjectRepository,
    private val imageRepository: ImageRepository,
    private val attachmentRepository: AttachmentRepository,
    private val textStyleRepository: TextStyleRepository,
    private val paragraphStyleRepository: ParagraphStyleRepository,
    private val variableRepository: VariableRepository,
    private val variableStructureRepository: VariableStructureRepository,
    private val displayRuleRepository: DisplayRuleRepository,
) {
    private val logger by logger()
    private val internalRepository = MappingInternalRepository(projectName.name)

    fun listAll(): List<Mapping> {
        return transaction { internalRepository.listAll().map { it.toDto() } }
    }

    fun applyAll() = applyAll {}

    fun applyAll(onError: (String) -> Unit) {
        applyAllDocumentObjectMappings()
        applyAllAreaMappings()
        applyAllImageMappings()
        applyAllAttachmentMappings()
        applyAllTextStyleMappings()
        applyAllParagraphStyleMappings()
        applyAllVariableMappings()
        applyAllVariableStructureMappings()
        applyAllDisplayRuleMappings()
        applyAllTableMappings(onError)
    }

    fun upsert(id: String, mapping: MappingItem): Mapping {
        return transaction { internalRepository.upsert(id, mapping.toDb()).toDto() }
    }

    fun upsertBatch(entries: Map<String, MappingItem>) {
        return transaction { internalRepository.upsertBatch(entries.map { (k, v) -> k to v.toDb() }) }
    }

    fun getDocumentObjectMapping(id: String): MappingItem.DocumentObject {
        return transaction {
            (internalRepository.find<MappingItemEntity.DocumentObject>(id) ?: MappingItemEntity.DocumentObject(
                name = null,
                internal = null,
                baseTemplate = null,
                targetFolder = null,
                type = null,
                variableStructureRef = null,
                skip = null,
            )).toDto() as MappingItem.DocumentObject
        }
    }

    fun applyDocumentObjectMapping(id: String) {
        val mapping = internalRepository.find<MappingItemEntity.DocumentObject>(id)
        val obj = documentObjectRepository.find(id)

        if (mapping == null || obj == null) {
            return
        }

        documentObjectRepository.upsert(mapping.apply(obj))
    }

    fun getAreaMapping(id: String): MappingItem.Area {
        return (internalRepository.find<MappingItemEntity.Area>(id) ?: MappingItemEntity.Area(
            name = null, areas = mutableMapOf(), flowToNextPage = mutableMapOf()
        )).toDto() as MappingItem.Area
    }

    fun applyAreaMapping(id: String) {
        val mapping = internalRepository.find<MappingItemEntity.Area>(id)
        val obj = documentObjectRepository.find(id)

        if (mapping == null || obj == null) {
            return
        }

        documentObjectRepository.upsert(mapping.apply(obj))
    }

    fun getImageMapping(id: String): MappingItem.Image {
        return (internalRepository.find<MappingItemEntity.Image>(id) ?: MappingItemEntity.Image(
            name = null,
            targetFolder = null,
            sourcePath = null,
            imageType = null,
            skip = null,
            alternateText = null,
        )).toDto() as MappingItem.Image
    }

    fun applyImageMapping(id: String) {
        val mapping = internalRepository.find<MappingItemEntity.Image>(id)
        val img = imageRepository.find(id)

        if (mapping == null || img == null) {
            return
        }

        imageRepository.upsert(mapping.apply(img))
    }

    fun getAttachmentMapping(id: String): MappingItem.Attachment {
        return (internalRepository.find<MappingItemEntity.Attachment>(id) ?: MappingItemEntity.Attachment(
            name = null,
            targetFolder = null,
            sourcePath = null,
            attachmentType = null,
            skip = null,
        )).toDto() as MappingItem.Attachment
    }

    fun applyAttachmentMapping(id: String) {
        val mapping = internalRepository.find<MappingItemEntity.Attachment>(id)
        val attachment = attachmentRepository.find(id)

        if (mapping == null || attachment == null) {
            return
        }

        attachmentRepository.upsert(mapping.apply(attachment))
    }

    fun applyAllAttachmentMappings() {
        applyAllResourceMappings<Attachment, MappingItemEntity.Attachment>(attachmentRepository, AttachmentTable)
    }

    fun applyAllAreaMappings() {
        applyAllResourceMappings<DocumentObject, MappingItemEntity.Area>(documentObjectRepository, DocumentObjectTable)
    }

    fun applyAllDocumentObjectMappings() {
        applyAllResourceMappings<DocumentObject, MappingItemEntity.DocumentObject>(documentObjectRepository, DocumentObjectTable)
    }

    fun applyAllImageMappings() {
        applyAllResourceMappings<Image, MappingItemEntity.Image>(imageRepository, ImageTable)
    }

    fun applyAllTextStyleMappings() {
        applyAllResourceMappings<TextStyle, MappingItemEntity.TextStyle>(textStyleRepository, TextStyleTable)
    }

    fun applyAllParagraphStyleMappings() {
        applyAllResourceMappings<ParagraphStyle, MappingItemEntity.ParagraphStyle>(paragraphStyleRepository, ParagraphStyleTable)
    }

    fun applyAllVariableMappings() {
        applyAllResourceMappings<Variable, MappingItemEntity.Variable>(variableRepository, VariableTable)
    }

    fun applyAllVariableStructureMappings() {
        applyAllResourceMappings<VariableStructure, MappingItemEntity.VariableStructure>(variableStructureRepository, VariableStructureTable)
    }

    fun applyAllDisplayRuleMappings() {
        applyAllResourceMappings<DisplayRule, MappingItemEntity.DisplayRule>(displayRuleRepository, DisplayRuleTable)
    }

    fun getTableMapping(id: String): MappingItem.Table {
        return (internalRepository.find<MappingItemEntity.Table>(id) ?: MappingItemEntity.Table(
            name = null, tables = emptyList()
        )).toDto() as MappingItem.Table
    }

    fun applyTableMapping(id: String) = applyTableMapping(id) {}

    fun applyTableMapping(id: String, onError: (String) -> Unit) {
        val mapping = internalRepository.find<MappingItemEntity.Table>(id)
        val obj = documentObjectRepository.find(id)

        if (mapping == null || obj == null) {
            return
        }

        documentObjectRepository.upsert(mapping.apply(obj, onError))
    }

    fun applyAllTableMappings() = applyAllTableMappings {}

    fun applyAllTableMappings(onError: (String) -> Unit) {
        applyAllResourceMappings<DocumentObject, MappingItemEntity.Table>(documentObjectRepository, DocumentObjectTable, onError)
    }

    fun getTextStyleMapping(id: String): MappingItem.TextStyle {
        return (internalRepository.find<MappingItemEntity.TextStyle>(id) ?: MappingItemEntity.TextStyle(
            name = null,
            targetId = null,
            definition = null,
        )).toDto() as MappingItem.TextStyle
    }

    fun applyTextStyleMapping(id: String) {
        val mapping = internalRepository.find<MappingItemEntity.TextStyle>(id)
        val textStyle = textStyleRepository.find(id)

        if (mapping == null || textStyle == null) {
            return
        }

        textStyleRepository.upsert(mapping.apply(textStyle))
    }

    fun getParagraphStyleMapping(id: String): MappingItem.ParagraphStyle {
        return (internalRepository.find<MappingItemEntity.ParagraphStyle>(id) ?: MappingItemEntity.ParagraphStyle(
            name = null, targetId = null, definition = null
        )).toDto() as MappingItem.ParagraphStyle
    }

    fun applyParagraphStyleMapping(id: String) {
        val mapping = internalRepository.find<MappingItemEntity.ParagraphStyle>(id)
        val paragraphStyle = paragraphStyleRepository.find(id)

        if (mapping == null || paragraphStyle == null) {
            return
        }

        paragraphStyleRepository.upsert(mapping.apply(paragraphStyle))
    }

    fun getVariableMapping(id: String): MappingItem.Variable {
        return (internalRepository.find<MappingItemEntity.Variable>(id) ?: MappingItemEntity.Variable(
            name = null, dataType = null
        )).toDto() as MappingItem.Variable
    }

    fun applyVariableMapping(id: String) {
        val mapping = internalRepository.find<MappingItemEntity.Variable>(id)
        val variable = variableRepository.find(id)

        if (mapping == null || variable == null) {
            return
        }

        variableRepository.upsert(mapping.apply(variable))
    }

    fun getVariableStructureMapping(id: String): MappingItem.VariableStructure {
        return (internalRepository.find<MappingItemEntity.VariableStructure>(id) ?: MappingItemEntity.VariableStructure(
            name = null, mappings = mutableMapOf(), languageVariable = null
        )).toDto() as MappingItem.VariableStructure
    }

    fun applyVariableStructureMapping(id: String) {
        val mapping = internalRepository.find<MappingItemEntity.VariableStructure>(id)
        val structure = variableStructureRepository.find(id) ?: VariableStructure(
            id = id,
            name = null,
            originLocations = emptyList(),
            customFields = CustomFieldMap(),
            created = kotlin.time.Clock.System.now(),
            lastUpdated = kotlin.time.Clock.System.now(),
            structure = mutableMapOf(),
            languageVariable = null,
        )

        if (mapping == null) {
            return
        }

        variableStructureRepository.upsert(mapping.apply(structure))
    }

    fun getDisplayRuleMapping(id: String): MappingItem.DisplayRule {
        return (internalRepository.find<MappingItemEntity.DisplayRule>(id) ?: MappingItemEntity.DisplayRule(
            name = null,
            targetId = null,
            internal = null,
            variableStructureRef = null,
            targetFolder = null,
            baseTemplate = null
        )).toDto() as MappingItem.DisplayRule
    }

    fun applyDisplayRuleMapping(id: String) {
        val mapping = internalRepository.find<MappingItemEntity.DisplayRule>(id)
        val displayRule = displayRuleRepository.find(id)

        if (mapping == null || displayRule == null) {
            return
        }

        displayRuleRepository.upsert(mapping.apply(displayRule))
    }

    fun deleteAll() {
        internalRepository.deleteAll()
    }

    private inline fun <reified O : MigrationObject, reified T : MappingItemEntity> applyAllResourceMappings(
        repo: Repository<O>,
        table: MigrationObjectTable,
        noinline onError: (String) -> Unit = {},
    ) {
        transaction {
            val mappings = internalRepository
                .listByType(T::class)
                .associate { it.resourceId.value to it.mapping as T }

            val migObjects = repo.list(table.id inList mappings.keys).associateBy { it.id }

            val migObjectsToUpsert = mappings
                .mapNotNull { (id, mapping) -> migObjects[id]?.let { mapping.apply(it, onError) as O } }

            val batches = migObjectsToUpsert.chunked(1000)
            batches.forEachIndexed { index, batch ->
                logger.info("Upserting ${T::class.simpleName} batch ${index + 1}/${batches.size} (${batch.size} items)")
                repo.upsertBatch(batch)
            }
        }
    }
}
