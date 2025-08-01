package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.databind.node.ArrayNode
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
import com.quadient.migration.service.imageExtension
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.resolveTargetDir
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.orDefault
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    private val baseTemplatesInteractiveFlowNamesToIds = mutableMapOf<String, Map<String, String>>()

    override fun getDocumentObjectPath(documentObject: DocumentObjectModel): String {
        val fileName = "${documentObject.nameOrId()}.jld"

        if (documentObject.targetFolder?.isAbsolute() == true) {
            return documentObject.targetFolder.join(fileName).toString()
        }

        val tenant = projectConfig.interactiveTenant
        val documentObjectType = documentObject.type.toInteractiveFolder()

        return IcmPath.root()
            .join("Interactive")
            .join(tenant)
            .join(documentObjectType)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, documentObject.targetFolder))
            .join(fileName)
            .toString()
    }

    override fun getImagePath(image: ImageModel): String {
        val fileName = "${image.nameOrId()}${imageExtension(image)}"

        if (image.targetFolder?.isAbsolute() == true) {
            return image.targetFolder.join(fileName).toString()
        }

        val imageConfigPath = projectConfig.paths.images

        return IcmPath.root()
            .join("Interactive")
            .join(projectConfig.interactiveTenant)
            .join(imageConfigPath.orDefault("Resources/Images"))
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, image.targetFolder))
            .join(fileName)
            .toString()
    }

    override fun getStyleDefinitionPath(): String {
        return IcmPath.root()
            .join("Interactive")
            .join(projectConfig.interactiveTenant)
            .join("CompanyStyles")
            .join(resolveTargetDir(projectConfig.defaultTargetFolder))
            .join("${projectConfig.name}Styles.wfd")
            .toString()
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
                        getInteractiveFlowIdByName(it.interactiveFlowName, baseTemplatePath.toString())
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
        val baseTemplateInteractiveFlowNames = getBaseTemplateInteractiveFlowNamesToIds(baseTemplatePath)

        return baseTemplateInteractiveFlowNames[interactiveFlowName]
    }

    fun getBaseTemplateInteractiveFlowNamesToIds(baseTemplatePath: String): Map<String, String> {
        val baseTemplateInteractiveFlowNamesToIds = baseTemplatesInteractiveFlowNamesToIds[baseTemplatePath]
        if (baseTemplateInteractiveFlowNamesToIds != null) {
            return baseTemplateInteractiveFlowNamesToIds
        }

        try {
            val baseTemplateXml = ipsService.wfd2xml(baseTemplatePath)
            val baseTemplateXmlTree = xmlMapper.readTree(baseTemplateXml.trimIndent())
            val layoutXmlTree = baseTemplateXmlTree["Layout"]["Layout"]

            val interactiveFlowNamesToIds = mutableMapOf<String, String>()

            val pagesInteractiveFlowNode = layoutXmlTree["Pages"]["InteractiveFlow"]
            val interactiveFlowIds = if (pagesInteractiveFlowNode is ArrayNode) {
                pagesInteractiveFlowNode.map { it["FlowId"].textValue() }
            } else {
                listOf(pagesInteractiveFlowNode["FlowId"].textValue())
            }

            interactiveFlowIds.forEachIndexed { i, it ->
                val flowData = layoutXmlTree["Flow"].first { flow -> flow["Id"].textValue() == it }
                val flowName = flowData["Name"]
                if (flowName != null) {
                    interactiveFlowNamesToIds[flowName.textValue()] = "Def.InteractiveFlow$i"
                }

                val customProperty = flowData["CustomProperty"]
                if (customProperty != null) {
                    val customPropertyObject = Json.decodeFromString<CustomProperty>(customProperty.textValue())
                    if (customPropertyObject.customName != null) {
                        interactiveFlowNamesToIds[customPropertyObject.customName] = "Def.InteractiveFlow$i"
                    }
                }
            }

            return interactiveFlowNamesToIds
        } catch (e: Exception) {
            logger.warn("Failed to load interactive flow names from base template '${baseTemplatePath}'.", e)
            return emptyMap()
        }
    }

    @Serializable
    data class CustomProperty(
        val customName: String? = null
    )
}