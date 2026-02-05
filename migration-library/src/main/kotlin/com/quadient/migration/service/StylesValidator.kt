package com.quadient.migration.service

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.FileRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
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

    private fun validateInternal(objects: List<DocumentObject>): ValidationResult {
        val refs = resolveDocumentObjects(objects).flatMap { it.collectRefs() }.toSet()
        val textStyles = mutableSetOf<TextStyle>()
        val paragraphStyles = mutableSetOf<ParagraphStyle>()

        val neededTextStyleIds = mutableSetOf<String>()
        val neededParagraphStyleIds = mutableSetOf<String>()

        val missingTextStyleIds = mutableSetOf<String>()
        val missingParagraphStyleIds = mutableSetOf<String>()

        for (style in refs) {
            when (style) {
                is ParagraphStyleRef -> {
                    val model = paragraphStyleRepository.findModel(style.id)
                    if (model == null) {
                        missingParagraphStyleIds.add(style.id)
                    } else {
                        paragraphStyles.add(model)
                    }
                }

                is TextStyleRef -> {
                    val model = textStyleRepository.findModel(style.id)
                    if (model == null) {
                        missingTextStyleIds.add(style.id)
                    } else {
                        textStyles.add(model)
                    }
                }

                is DisplayRuleRef -> {}
                is DocumentObjectRef -> {}
                is ImageRef -> {}
                is FileRef -> {}
                is VariableRef -> {}
                is VariableStructureRef -> {}
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

    private fun resolveDocumentObjects(objects: List<DocumentObject>): List<DocumentObject> {
        val result = mutableListOf<DocumentObject>()
        val visited = mutableSetOf<String>()
        val queue = objects.toMutableList()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (visited.contains(current.id)) {
                continue
            }
            result.add(current)
            visited.add(current.id)

            val refs = current.collectRefs().filterIsInstance<DocumentObjectRef>()
            for (ref in refs) {
                val model = documentObjectRepository.findModelOrFail(ref.id)
                if (!visited.contains(model.id)) {
                    queue.add(model)
                }
            }
        }

        return result
    }

    private fun TextStyle.resolveTextStyle(
        textStyleIds: MutableSet<String>, missingTextStyles: MutableSet<String>
    ) {
        when (this.definition) {
            is TextStyleDefinition -> {
                textStyleIds.add(this.nameOrId())
            }

            is TextStyleRef -> {
                val def = this.definition
                if (def is TextStyleRef) {
                    val model = textStyleRepository.findModel(def.id)
                    if (model != null) {
                        model.resolveTextStyle(textStyleIds, missingTextStyles)
                    } else {
                        missingTextStyles.add(this.id)
                    }
                }
            }
        }
    }

    private fun ParagraphStyle.resolveParagraphStyle(
        paragraphStyleIds: MutableSet<String>, missingParagraphStyles: MutableSet<String>
    ) {
        when (this.definition) {
            is ParagraphStyleDefinition -> {
                paragraphStyleIds.add(this.nameOrId())
            }

            is ParagraphStyleRef -> {
                val def = this.definition
                if (def is ParagraphStyleRef) {
                    val model = paragraphStyleRepository.findModel(def.id)
                    if (model != null) {
                        model.resolveParagraphStyle(paragraphStyleIds, missingParagraphStyles)
                    } else {
                        missingParagraphStyles.add(this.id)
                    }
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