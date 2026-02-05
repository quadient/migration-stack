package com.quadient.migration.service

import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.FileRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.dto.migrationmodel.RefValidatable
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.FileInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository

class ReferenceValidator(
    private val documentObjectRepository: DocumentObjectInternalRepository,
    private val variableRepository: VariableInternalRepository,
    private val textStyleRepository: TextStyleInternalRepository,
    private val paragraphStyleRepository: ParagraphStyleInternalRepository,
    private val variableStructureRepository: VariableStructureInternalRepository,
    private val displayRuleRepository: DisplayRuleInternalRepository,
    private val imageRepository: ImageInternalRepository,
    private val fileRepository: FileInternalRepository,
) {
    /**
     * Validates all objects in the database.
     * When an object is found to have missing references,
     * it is marked as invalid and all invalid references are returned in a List.
     * @return MissingRefs containing list of missing references
     */
    fun validateAll(): MissingRefs {
        val documentObjects = documentObjectRepository.listAllModel()
        val variables = variableRepository.listAllModel()
        val paragraphStyles = paragraphStyleRepository.listAllModel()
        val textStyles = textStyleRepository.listAllModel()
        val dataStructures = variableStructureRepository.listAllModel()
        val displayRules = displayRuleRepository.listAllModel()
        val images = imageRepository.listAllModel()
        val files = fileRepository.listAllModel()
        val alreadyValidatedRefs = mutableSetOf<Ref>()

        val missingRefs =
            (documentObjects + variables + paragraphStyles + textStyles + dataStructures + displayRules + images + files).mapNotNull {
                    validate(it, alreadyValidatedRefs).missingRefs.ifEmpty { null }
        }.flatten()

        return MissingRefs(missingRefs.distinct())
    }

    /**
     * Validates the references of a single object.
     * @return ValidationResult containing validated references and missing references
     */
    fun validate(input: RefValidatable, alreadyValidRefs: MutableSet<Ref>): ValidationResult {
        val queue: MutableList<Ref> = input.collectRefs().toMutableList()

        val missingRefs = mutableListOf<Ref>()
        val validatedRefs = mutableListOf<Ref>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (alreadyValidRefs.contains(current)) {
                continue
            }

            when (current) {
                is DocumentObjectRef -> {
                    val documentObject = documentObjectRepository.findModel(current.id)

                    if (documentObject == null) {
                        missingRefs.add(current)
                    } else {
                        validatedRefs.add(current)
                        queue.addAll(documentObject.collectRefs())
                        alreadyValidRefs.add(current)
                    }
                }

                is VariableRef -> {
                    val variable = variableRepository.findModel(current.id)

                    if (variable != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                    } else {
                        missingRefs.add(current)
                    }
                }

                is ParagraphStyleRef -> {
                    val style = paragraphStyleRepository.findModel(current.id)

                    if (style != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(style.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is TextStyleRef -> {
                    val style = textStyleRepository.findModel(current.id)

                    if (style != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(style.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is DisplayRuleRef -> {
                    val rule = displayRuleRepository.findModel(current.id)

                    if (rule != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(rule.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is ImageRef -> {
                    val image = imageRepository.findModel(current.id)

                    if (image != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(image.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is FileRef -> {
                    val file = fileRepository.findModel(current.id)

                    if (file != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(file.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is VariableStructureRef -> {
                    val variableStructure = variableStructureRepository.findModel(current.id)
                    
                    if (variableStructure != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(variableStructure.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }
            }
        }

        return ValidationResult(validatedRefs, missingRefs)
    }

    data class ValidationResult(val validatedRefs: List<Ref>, val missingRefs: List<Ref>)
    data class MissingRefs(val missingRefs: List<Ref>)
}

