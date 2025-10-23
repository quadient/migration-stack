package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.quadient.migration.api.IcmFileMetadata
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
import com.quadient.migration.shared.ImageType
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
    ipsService: IpsService,
) : InspireDocumentObjectBuilder(
    documentObjectRepository,
    textStyleRepository,
    paragraphStyleRepository,
    variableRepository,
    variableStructureRepository,
    displayRuleRepository,
    imageRepository,
    projectConfig,
    ipsService,
) {
    private val lenientJson = Json { ignoreUnknownKeys = true }
    private val mainFlowId = "Def.MainFlow"

    private val xmlMapper by lazy { XmlMapper().registerKotlinModule() }
    private val baseTemplatesInteractiveFlowNamesToIds = mutableMapOf<String, Map<String, String>>()
    private val baseTemplateMetadata = mutableMapOf<String, IcmFileMetadata>()

    override fun getDocumentObjectPath(nameOrId: String, type: DocumentObjectType, targetFolder: IcmPath?): String {
        val fileName = "$nameOrId.jld"

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName).toString()
        }

        val tenant = projectConfig.interactiveTenant
        val documentObjectType = type.toInteractiveFolder()

        return IcmPath.root().join("Interactive").join(tenant).join(documentObjectType)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(fileName).toString()
    }

    override fun getDocumentObjectPath(documentObject: DocumentObjectModel) =
        getDocumentObjectPath(documentObject.nameOrId(), documentObject.type, documentObject.targetFolder)

    override fun getImagePath(
        id: String,
        imageType: ImageType,
        name: String?,
        targetFolder: IcmPath?,
        sourcePath: String?
    ): String {
        val fileName = "${name ?: id}${imageExtension(imageType, name, sourcePath)}"

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName).toString()
        }

        val imageConfigPath = projectConfig.paths.images

        return IcmPath.root()
            .join("Interactive")
            .join(projectConfig.interactiveTenant)
            .join(imageConfigPath.orDefault("Resources/Images"))
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder))
            .join(fileName)
            .toString()
    }

    override fun getImagePath(image: ImageModel) =
        getImagePath(image.id, image.imageType, image.name, image.targetFolder, image.sourcePath)

    override fun getStyleDefinitionPath(): String {
        val styleDefConfigPath = projectConfig.styleDefinitionPath

        if (styleDefConfigPath != null && !styleDefConfigPath.isAbsolute()) {
            throw IllegalArgumentException("The configured style definition path '${styleDefConfigPath}' is not absolute.")
        } else if (styleDefConfigPath != null) {
            return styleDefConfigPath.toString()
        }

        return IcmPath.root()
            .join("Interactive")
            .join(projectConfig.interactiveTenant)
            .join("CompanyStyles")
            .join(resolveTargetDir(projectConfig.defaultTargetFolder))
            .join("${projectConfig.name}Styles.wfd")
            .toString()
    }

    override fun getFontRootFolder(): String {
        val fontConfigPath = projectConfig.paths.fonts

        return IcmPath.root().join("Interactive").join(projectConfig.interactiveTenant)
            .join(fontConfigPath.orDefault("Resources/Fonts")).toString()
    }

    override fun buildDocumentObject(documentObject: DocumentObjectModel, styleDefinitionPath: String?): String {
        logger.debug("Starting to build document object '${documentObject.nameOrId()}'.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()

        val baseTemplatePath = getBaseTemplateFullPath(projectConfig, documentObject.baseTemplate)
        val defaultLanguage = this.getDefaultLanguage(baseTemplatePath.toString())

        val variableStructure = initVariableStructure(layout, documentObject)

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
                buildDocumentContentAsFlows(layout, variableStructure, it.value, documentObject.nameOrId(), defaultLanguage)

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
                    val customPropertyObject = lenientJson.decodeFromString<CustomProperty>(customProperty.textValue())
                    if (customPropertyObject.customName != null) {
                        interactiveFlowNamesToIds[customPropertyObject.customName] = "Def.InteractiveFlow$i"
                    }
                }
            }

            baseTemplatesInteractiveFlowNamesToIds[baseTemplatePath] = interactiveFlowNamesToIds
            return interactiveFlowNamesToIds
        } catch (e: Exception) {
            logger.warn("Failed to load interactive flow names from base template '${baseTemplatePath}'.", e)
            return emptyMap()
        }
    }

    private fun getDefaultLanguage(baseTemplatePath: String): String {
        val result = this.baseTemplateMetadata.getOrPut(baseTemplatePath) {
            try {
                ipsService.readMetadata(baseTemplatePath)
            } catch (e: Exception) {
                logger.error("Failed to load metadata from base template '${baseTemplatePath}'.", e)
                throw e
            }
        }.system["language"]?.first()

        return requireNotNull(result) {
            "Failed to determine default language from base template '$baseTemplatePath'. Metadata: '$result'"
        }
    }

    @Serializable
    data class CustomProperty(
        val customName: String? = null
    )
}