package com.quadient.migration.service

import com.quadient.migration.data.DisplayRuleModelRef
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.data.RefModel
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository

interface RefValidatable {
    fun collectRefs(): List<RefModel>
}

class ReferenceValidator(
    private val documentObjectRepository: DocumentObjectInternalRepository,
    private val variableRepository: VariableInternalRepository,
    private val textStyleRepository: TextStyleInternalRepository,
    private val paragraphStyleRepository: ParagraphStyleInternalRepository,
    private val variableStructureRepository: VariableStructureInternalRepository,
    private val displayRuleRepository: DisplayRuleInternalRepository,
    private val imageRepository: ImageInternalRepository,
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
        val alreadyValidatedRefs = mutableSetOf<RefModel>()

        val missingRefs =
            (documentObjects + variables + paragraphStyles + textStyles + dataStructures + displayRules + images).mapNotNull {
                    validate(it, alreadyValidatedRefs).missingRefs.ifEmpty { null }
        }.flatten()

        return MissingRefs(missingRefs.distinct())
    }

    /**
     * Validates the references of a single object.
     * @return ValidationResult containing validated references and missing references
     */
    fun validate(input: RefValidatable, alreadyValidRefs: MutableSet<RefModel>): ValidationResult {
        val queue: MutableList<RefModel> = input.collectRefs().toMutableList()

        val missingRefs = mutableListOf<RefModel>()
        val validatedRefs = mutableListOf<RefModel>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (alreadyValidRefs.contains(current)) {
                continue
            }

            when (current) {
                is DocumentObjectModelRef -> {
                    val documentObject = documentObjectRepository.findModel(current.id)

                    if (documentObject == null) {
                        missingRefs.add(current)
                    } else {
                        validatedRefs.add(current)
                        queue.addAll(documentObject.collectRefs())
                        alreadyValidRefs.add(current)
                    }
                }

                is VariableModelRef -> {
                    val variable = variableRepository.findModel(current.id)

                    if (variable != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                    } else {
                        missingRefs.add(current)
                    }
                }

                is ParagraphStyleModelRef -> {
                    val style = paragraphStyleRepository.findModel(current.id)

                    if (style != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(style.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is TextStyleModelRef -> {
                    val style = textStyleRepository.findModel(current.id)

                    if (style != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(style.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is DisplayRuleModelRef -> {
                    val rule = displayRuleRepository.findModel(current.id)

                    if (rule != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(rule.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is ImageModelRef -> {
                    val image = imageRepository.findModel(current.id)

                    if (image != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(image.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }
            }
        }

        return ValidationResult(validatedRefs, missingRefs)
    }

    data class ValidationResult(val validatedRefs: List<RefModel>, val missingRefs: List<RefModel>)
    data class MissingRefs(val missingRefs: List<RefModel>)
}

