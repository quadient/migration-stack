package com.quadient.migration.service

import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository

class ReferenceValidator(
    private val documentObjectRepository: DocumentObjectRepository,
    private val variableRepository: VariableRepository,
    private val textStyleRepository: TextStyleRepository,
    private val paragraphStyleRepository: ParagraphStyleRepository,
    private val variableStructureRepository: VariableStructureRepository,
    private val displayRuleRepository: DisplayRuleRepository,
    private val imageRepository: ImageRepository,
    private val attachmentRepository: AttachmentRepository,
) {
    /**
     * Validates all objects in the database.
     * When an object is found to have missing references,
     * it is marked as invalid and all invalid references are returned in a List.
     * @return MissingRefs containing list of missing references
     */
    fun validateAll(): MissingRefs {
        val documentObjects = documentObjectRepository.listAll()
        val variables = variableRepository.listAll()
        val paragraphStyles = paragraphStyleRepository.listAll()
        val textStyles = textStyleRepository.listAll()
        val dataStructures = variableStructureRepository.listAll()
        val displayRules = displayRuleRepository.listAll()
        val images = imageRepository.listAll()
        val attachments = attachmentRepository.listAll()
        val alreadyValidatedRefs = mutableSetOf<Ref>()

        val missingRefs =
            (documentObjects + variables + paragraphStyles + textStyles + dataStructures + displayRules + images + attachments).mapNotNull {
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
                    val documentObject = documentObjectRepository.find(current.id)

                    if (documentObject == null) {
                        missingRefs.add(current)
                    } else {
                        validatedRefs.add(current)
                        queue.addAll(documentObject.collectRefs())
                        alreadyValidRefs.add(current)
                    }
                }

                is VariableRef -> {
                    val variable = variableRepository.find(current.id)

                    if (variable != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                    } else {
                        missingRefs.add(current)
                    }
                }

                is ParagraphStyleRef -> {
                    val style = paragraphStyleRepository.find(current.id)

                    if (style != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(style.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is TextStyleRef -> {
                    val style = textStyleRepository.find(current.id)

                    if (style != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(style.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is DisplayRuleRef -> {
                    val rule = displayRuleRepository.find(current.id)

                    if (rule != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(rule.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is ImageRef -> {
                    val image = imageRepository.find(current.id)

                    if (image != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(image.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is AttachmentRef -> {
                    val attachment = attachmentRepository.find(current.id)

                    if (attachment != null) {
                        validatedRefs.add(current)
                        alreadyValidRefs.add(current)
                        queue.addAll(attachment.collectRefs())
                    } else {
                        missingRefs.add(current)
                    }
                }

                is VariableStructureRef -> {
                    val variableStructure = variableStructureRepository.find(current.id)
                    
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

