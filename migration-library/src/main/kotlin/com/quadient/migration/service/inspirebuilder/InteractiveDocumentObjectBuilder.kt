package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.data.AreaModel
import com.quadient.migration.data.DocumentContentModel
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.ImageModel
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository
import com.quadient.migration.service.getBaseTemplateFullPath
import com.quadient.migration.service.getFolder
import com.quadient.migration.service.imageExtension
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import kotlin.collections.find

class InteractiveDocumentObjectBuilder(
    documentObjectRepository: DocumentObjectInternalRepository,
    textStyleRepository: TextStyleInternalRepository,
    paragraphStyleRepository: ParagraphStyleInternalRepository,
    variableRepository: VariableInternalRepository,
    variableStructureRepository: VariableStructureInternalRepository,
    displayRuleRepository: DisplayRuleInternalRepository,
    imageRepository: ImageInternalRepository,
    projectConfig: ProjectConfig,
    private val ipsService: IpsService,
) : InspireDocumentObjectBuilder(
    documentObjectRepository,
    textStyleRepository,
    paragraphStyleRepository,
    variableRepository,
    variableStructureRepository,
    displayRuleRepository,
    imageRepository,
    projectConfig
) {
    private val mainFlowId = "Def.MainFlow"

    private val xmlMapper by lazy { XmlMapper().registerKotlinModule() }
    private val baseTemplatesInteractiveFlowNames = mutableMapOf<String, List<String>>()

    override fun getDocumentObjectPath(documentObject: DocumentObjectModel): String {
        return "icm://Interactive/${projectConfig.interactiveTenant}/${documentObject.type.toInteractiveFolder()}/${
            getFolder(projectConfig, documentObject.targetFolder)
        }${documentObject.nameOrId()}.jld"
    }

    override fun getImagePath(image: ImageModel): String {
        return "icm://Interactive/${projectConfig.interactiveTenant}/Resources/Images/${
            getFolder(projectConfig, image.targetFolder)
        }${image.nameOrId()}${imageExtension(image)}"
    }

    override fun getStyleDefinitionPath(): String {
        return "icm://Interactive/${projectConfig.interactiveTenant}/CompanyStyles/${getFolder(projectConfig)}${projectConfig.name}Styles.wfd"
    }

    override fun buildDocumentObject(documentObject: DocumentObjectModel): String {
        logger.debug("Starting to build document object '${documentObject.nameOrId()}'.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()

        val baseTemplatePath = getBaseTemplateFullPath(projectConfig, documentObject.baseTemplate)
        val variableStructure = initVariableStructure(layout)

        val interactiveFlowsWithContent = mutableMapOf<String, MutableList<DocumentContentModel>>()
        if (documentObject.type == DocumentObjectType.Page) {
            documentObject.content.forEach {
                if (it is AreaModel && !it.interactiveFlowName.isNullOrBlank()) {
                    val interactiveFlowId = if (it.interactiveFlowName.startsWith("Def.")) {
                        it.interactiveFlowName
                    } else {
                        getInteractiveFlowIdByName(it.interactiveFlowName, baseTemplatePath)
                    }

                    if (interactiveFlowId.isNullOrBlank()) {
                        val errorMessage =
                            "Failed to find interactive flow '${it.interactiveFlowName}' in base template '$baseTemplatePath'."
                        logger.error(errorMessage)
                        error(errorMessage)
                    }

                    val interactiveFlowContent =
                        interactiveFlowsWithContent.getOrPut(interactiveFlowId) { mutableListOf() }
                    interactiveFlowContent.addAll(it.content)
                } else {
                    val interactiveFlowContent = interactiveFlowsWithContent.getOrPut(mainFlowId) { mutableListOf() }
                    interactiveFlowContent.add(it)
                }
            }
        } else {
            interactiveFlowsWithContent.put(mainFlowId, documentObject.content.toMutableList())
        }

        interactiveFlowsWithContent.forEach {
            val interactiveFlowText =
                layout.addFlow().setId(it.key).setType(Flow.Type.SIMPLE).setSectionFlow(true).addParagraph().addText()

            val contentFlows =
                buildDocumentContentAsFlows(layout, variableStructure, it.value, documentObject.nameOrId())

            if (documentObject.displayRuleRef == null) {
                contentFlows.forEach { contentFlow -> interactiveFlowText.appendFlow(contentFlow) }
            } else {
                interactiveFlowText.appendFlow(
                    contentFlows.toSingleFlow(
                        layout, variableStructure, documentObject.nameOrId(), documentObject.displayRuleRef
                    )
                )
            }
        }

        logger.debug("Successfully built document object '${documentObject.nameOrId()}'")
        return builder.buildLayoutDelta()
    }

    fun getInteractiveFlowIdByName(interactiveFlowName: String, baseTemplatePath: String): String? {
        val baseTemplateInteractiveFlowNames = getBaseTemplateInteractiveFlowNames(baseTemplatePath)

        val interactiveFlowIndex = baseTemplateInteractiveFlowNames.indexOf(interactiveFlowName)
        return if (interactiveFlowIndex > -1) {
            "Def.InteractiveFlow$interactiveFlowIndex"
        } else {
            null
        }
    }

    fun getBaseTemplateInteractiveFlowNames(baseTemplatePath: String): List<String> {
        val baseTemplateInteractiveFlowNames = baseTemplatesInteractiveFlowNames[baseTemplatePath]
        if (baseTemplateInteractiveFlowNames != null) {
            return baseTemplateInteractiveFlowNames
        }

        try {
            val baseTemplateXml = ipsService.wfd2xml(baseTemplatePath)
            val baseTemplateXmlTree = xmlMapper.readTree(baseTemplateXml.trimIndent())
            val interactiveFlowNames =
                baseTemplateXmlTree["Property"].find { it["Name"].textValue() == "InteractiveFlowsNames" }
                    ?.let { it["Value"].textValue() }?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

            baseTemplatesInteractiveFlowNames[baseTemplatePath] = interactiveFlowNames
            return interactiveFlowNames
        } catch (e: Exception) {
            logger.warn("Failed to load interactive flow names from base template '${baseTemplatePath}'.", e)
            return emptyList()
        }
    }
}