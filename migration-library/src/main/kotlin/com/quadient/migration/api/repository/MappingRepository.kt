package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.Mapping
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.persistence.migrationmodel.MappingItemEntity
import com.quadient.migration.persistence.repository.MappingInternalRepository

class MappingRepository(
    private val projectName: String,
    private val documentObjectRepository: DocumentObjectRepository,
    private val imageRepository: ImageRepository,
    private val attachmentRepository: AttachmentRepository,
    private val textStyleRepository: TextStyleRepository,
    private val paragraphStyleRepository: ParagraphStyleRepository,
    private val variableRepository: VariableRepository,
    private val variableStructureRepository: VariableStructureRepository
) {
    private val internalRepository = MappingInternalRepository(projectName)

    fun listAll(): List<Mapping> {
        return internalRepository.listAll().map { it.toDto() }
    }

    fun applyAll() {
        for (mapping in listAll()) {
            when (mapping.mapping) {
                is MappingItem.DocumentObject -> applyDocumentObjectMapping(mapping.id)
                is MappingItem.Image -> applyImageMapping(mapping.id)
                is MappingItem.Attachment -> applyAttachmentMapping(mapping.id)
                is MappingItem.TextStyle -> applyTextStyleMapping(mapping.id)
                is MappingItem.ParagraphStyle -> applyParagraphStyleMapping(mapping.id)
                is MappingItem.Variable -> applyVariableMapping(mapping.id)
                is MappingItem.Area -> applyAreaMapping(mapping.id)
                is MappingItem.VariableStructure -> applyVariableStructureMapping(mapping.id)
            }
        }
    }

    fun upsert(id: String, mapping: MappingItem): Mapping {
        return internalRepository.upsert(id, mapping.toDb()).toDto()
    }

    fun getDocumentObjectMapping(id: String): MappingItem.DocumentObject {
        return (internalRepository.find<MappingItemEntity.DocumentObject>(id) ?: MappingItemEntity.DocumentObject(
            name = null,
            internal = null,
            baseTemplate = null,
            targetFolder = null,
            type = null,
            variableStructureRef = null,
            skip = null,
        )).toDto() as MappingItem.DocumentObject
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

    fun getTextStyleMapping(id: String): MappingItem.TextStyle {
        return (internalRepository.find<MappingItemEntity.TextStyle>(id) ?: MappingItemEntity.TextStyle(
            name = null,
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
            name = null, definition = null
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
            created = kotlinx.datetime.Clock.System.now(),
            lastUpdated = kotlinx.datetime.Clock.System.now(),
            structure = mutableMapOf(),
            languageVariable = null,
        )

        if (mapping == null) {
            return
        }

        variableStructureRepository.upsert(mapping.apply(structure))
    }

    fun deleteAll() {
        internalRepository.deleteAll()
    }
}