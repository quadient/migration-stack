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
    private val textStyleRepository: TextStyleRepository,
    private val paragraphStyleRepository: ParagraphStyleRepository,
    private val variableRepository: VariableRepository,
    private val variableStructureRepository: VariableStructureRepository
) {
    private val internalRepository = MappingInternalRepository(projectName)

    fun listAll(): List<Mapping> {
        return internalRepository.listAll().map { it.toDto() }
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

    fun getImageMapping(id: String): MappingItem.Image {
        return (internalRepository.find<MappingItemEntity.Image>(id) ?: MappingItemEntity.Image(
            name = null,
            targetFolder = null,
            sourcePath = null,
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
            name = null, dataType = null, inspirePath = null
        )).toDto() as MappingItem.Variable
    }

    fun applyVariableMapping(id: String, structureId: String) {
        val mapping = internalRepository.find<MappingItemEntity.Variable>(id)
        val variable = variableRepository.find(id)
        val structure = variableStructureRepository.find(structureId) ?: VariableStructure(
            id = structureId,
            name = null,
            originLocations = emptyList(),
            customFields = CustomFieldMap(),
            structure = mutableMapOf()
        )

        if (mapping == null || variable == null) {
            return
        }

        val (newVar, newStructure) = mapping.apply(variable, structure)

        variableRepository.upsert(newVar)
        variableStructureRepository.upsert(newStructure)
    }

    fun deleteAll() {
        internalRepository.deleteAll()
    }
}