package com.quadient.migration.service

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.quadient.migration.data.DisplayRuleModelRef
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.FileModelRef
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphStyleDefinitionModel
import com.quadient.migration.data.ParagraphStyleModel
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.data.TextStyleDefinitionModel
import com.quadient.migration.data.TextStyleModel
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.data.VariableStructureModelRef
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.service.deploy.DeployClient
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.service.ipsclient.IpsService

class StylesValidator(
    private val documentObjectRepository: DocumentObjectInternalRepository,
    private val textStyleRepository: TextStyleInternalRepository,
    private val paragraphStyleRepository: ParagraphStyleInternalRepository,
    private val documentObjectBuilder: InspireDocumentObjectBuilder,
    private val deployClient: DeployClient,
    private val ipsService: IpsService,
) {
    fun validateAll(): ValidationResult {
        val documentObjects = deployClient.getAllDocumentObjectsToDeploy()
        return validateInternal(documentObjects)
    }

    fun validate(ids: List<String>): ValidationResult {
        val documentObjects = deployClient.getDocumentObjectsToDeploy(ids)
        return validateInternal(documentObjects)
    }

    private fun validateInternal(objects: List<DocumentObjectModel>): ValidationResult {
        val refs = resolveDocumentObjects(objects).flatMap { it.collectRefs() }.toSet()
        val textStyles = mutableSetOf<TextStyleModel>()
        val paragraphStyles = mutableSetOf<ParagraphStyleModel>()

        val neededTextStyleIds = mutableSetOf<String>()
        val neededParagraphStyleIds = mutableSetOf<String>()

        val missingTextStyleIds = mutableSetOf<String>()
        val missingParagraphStyleIds = mutableSetOf<String>()

        for (style in refs) {
            when (style) {
                is ParagraphStyleModelRef -> {
                    val model = paragraphStyleRepository.findModel(style.id)
                    if (model == null) {
                        missingParagraphStyleIds.add(style.id)
                    } else {
                        paragraphStyles.add(model)
                    }
                }

                is TextStyleModelRef -> {
                    val model = textStyleRepository.findModel(style.id)
                    if (model == null) {
                        missingTextStyleIds.add(style.id)
                    } else {
                        textStyles.add(model)
                    }
                }

                is DisplayRuleModelRef -> {}
                is DocumentObjectModelRef -> {}
                is ImageModelRef -> {}
                is FileModelRef -> {}
                is VariableModelRef -> {}
                is VariableStructureModelRef -> {}
            }
        }

        for (textStyle in textStyles) {
            textStyle.resolveTextStyle(neededTextStyleIds, missingTextStyleIds)
        }
        for (paragraphStyle in paragraphStyles) {
            paragraphStyle.resolveParagraphStyle(neededParagraphStyleIds, missingParagraphStyleIds)
        }

        val styleDefPath = documentObjectBuilder.getStyleDefinitionPath()

        val exists = try {
            ipsService.fileExists(styleDefPath)
        } catch (ex: Exception) {
            throw RuntimeException("Failed to check whether style definition $styleDefPath exists", ex)
        }

        if (!exists) {
            throw RuntimeException("Style definition $styleDefPath does not exist, cannot validate.")
        }

        val xmlString = try {
            ipsService.wfd2xml(documentObjectBuilder.getStyleDefinitionPath())
        } catch (ex: Exception) {
            throw RuntimeException("wfd2xml failed, cannot validate.", ex)
        }

        val xml = XmlMapper().readTree(xmlString)

        val xmlParaStyles = xml["Layout"]?.let { it["Layout"] }?.let {
            if (it["ParaStyle"] is ArrayNode) {
                it["ParaStyle"]?.mapNotNull { s -> s["Name"]?.asText() }?.toSet() ?: emptySet()
            } else {
                it["ParaStyle"]?.let { s -> s["Name"]?.asText() }?.let { s -> setOf(s) } ?: emptySet()
            }
        } ?: emptySet()
        val xmlTextStyles = xml["Layout"]?.let { it["Layout"] }?.let {
            if (it["TextStyle"] is ArrayNode) {
                it["TextStyle"]?.mapNotNull { s -> s["Name"]?.asText() }?.toSet() ?: emptySet()
            } else {
                it["TextStyle"]?.let { s -> s["Name"]?.asText() }?.let { s -> setOf(s) } ?: emptySet()
            }
        } ?: emptySet()

        missingTextStyleIds.addAll(neededTextStyleIds subtract xmlTextStyles)
        missingParagraphStyleIds.addAll(neededParagraphStyleIds subtract xmlParaStyles)

        return ValidationResult(
            textStyles = (neededTextStyleIds intersect xmlTextStyles).toList(),
            paragraphStyles = (neededParagraphStyleIds intersect xmlParaStyles).toList(),
            missingTextStyles = missingTextStyleIds.toList(),
            missingParagraphStyles = missingParagraphStyleIds.toList(),
        )
    }

    private fun resolveDocumentObjects(objects: List<DocumentObjectModel>): List<DocumentObjectModel> {
        val result = mutableListOf<DocumentObjectModel>()
        val visited = mutableSetOf<String>()
        val queue = objects.toMutableList()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (visited.contains(current.id)) {
                continue
            }
            result.add(current)
            visited.add(current.id)

            val refs = current.collectRefs().filterIsInstance<DocumentObjectModelRef>()
            for (ref in refs) {
                val model = documentObjectRepository.findModelOrFail(ref.id)
                if (!visited.contains(model.id)) {
                    queue.add(model)
                }
            }
        }

        return result
    }

    private fun TextStyleModel.resolveTextStyle(
        textStyleIds: MutableSet<String>, missingTextStyles: MutableSet<String>
    ) {
        when (this.definition) {
            is TextStyleDefinitionModel -> {
                textStyleIds.add(this.nameOrId())
            }

            is TextStyleModelRef -> {
                val model = textStyleRepository.findModel(this.definition.id)
                if (model != null) {
                    model.resolveTextStyle(textStyleIds, missingTextStyles)
                } else {
                    missingTextStyles.add(this.id)
                }
            }
        }
    }

    private fun ParagraphStyleModel.resolveParagraphStyle(
        paragraphStyleIds: MutableSet<String>, missingParagraphStyles: MutableSet<String>
    ) {
        when (this.definition) {
            is ParagraphStyleDefinitionModel -> {
                paragraphStyleIds.add(this.nameOrId())
            }

            is ParagraphStyleModelRef -> {
                val model = paragraphStyleRepository.findModel(this.definition.id)
                if (model != null) {
                    model.resolveParagraphStyle(paragraphStyleIds, missingParagraphStyles)
                } else {
                    missingParagraphStyles.add(this.id)
                }
            }
        }
    }
}

data class ValidationResult(
    val textStyles: List<String>,
    val paragraphStyles: List<String>,
    val missingTextStyles: List<String>,
    val missingParagraphStyles: List<String>
)