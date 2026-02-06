package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.File
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.service.getBaseTemplateFullPath
import com.quadient.migration.service.imageExtension
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.resolveTargetDir
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.orDefault
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.module.Layout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class InteractiveDocumentObjectBuilder(
    documentObjectRepository: DocumentObjectRepository,
    textStyleRepository: TextStyleRepository,
    paragraphStyleRepository: ParagraphStyleRepository,
    variableRepository: Repository<com.quadient.migration.api.dto.migrationmodel.Variable>,
    variableStructureRepository: Repository<com.quadient.migration.api.dto.migrationmodel.VariableStructure>,
    displayRuleRepository: Repository<com.quadient.migration.api.dto.migrationmodel.DisplayRule>,
    imageRepository: Repository<Image>,
    fileRepository: Repository<File>,
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
    fileRepository,
    projectConfig,
    ipsService,
) {
    private val lenientJson = Json { ignoreUnknownKeys = true }
    private val mainFlowId = "Def.MainFlow"

    private val xmlMapper by lazy { XmlMapper().registerKotlinModule() }
    private val baseTemplatesInteractiveFlowNamesToIds = mutableMapOf<String, Map<String, String>>()
    private val baseTemplateExistenceCache = mutableMapOf<IcmPath, Boolean>()

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

    override fun getDocumentObjectPath(documentObject: DocumentObject) =
        getDocumentObjectPath(documentObject.nameOrId(), documentObject.type, documentObject.targetFolder?.let { IcmPath.from(it) })

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

    override fun getImagePath(image: Image) =
        getImagePath(image.id, image.imageType ?: ImageType.Unknown, image.name, image.targetFolder?.let { IcmPath.from(it) }, image.sourcePath)

    override fun getFilePath(
        id: String, name: String?, targetFolder: IcmPath?, sourcePath: String?, fileType: FileType
    ): String {
        val baseFileName = name ?: id
        val fileName = appendExtensionIfMissing(baseFileName, sourcePath)

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName).toString()
        }

        val fileConfigPath = when (fileType) {
            FileType.Document -> projectConfig.paths.documents.orDefault("Documents")
            FileType.Attachment -> projectConfig.paths.attachments.orDefault("Attachments")
        }

        return IcmPath.root().join("Interactive").join(projectConfig.interactiveTenant).join(fileConfigPath)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(fileName).toString()
    }

    override fun getFilePath(file: File): String =
        getFilePath(file.id, file.name, file.targetFolder?.let { IcmPath.from(it) }, file.sourcePath, file.fileType)

    override fun getStyleDefinitionPath(extension: String): String {
        val styleDefConfigPath = projectConfig.styleDefinitionPath

        if (styleDefConfigPath != null && !styleDefConfigPath.isAbsolute()) {
            throw IllegalArgumentException("The configured style definition path '${styleDefConfigPath}' is not absolute.")
        } else if (styleDefConfigPath != null && !styleDefConfigPath.path.endsWith(".jld")) {
            throw IllegalArgumentException("Style definition path '${styleDefConfigPath}' must end with '.jld'.")
        } else if (styleDefConfigPath != null) {
            return styleDefConfigPath.toString().replace(".jld", ".$extension")
        }

        return IcmPath.root()
            .join("Interactive")
            .join(projectConfig.interactiveTenant)
            .join("CompanyStyles")
            .join(resolveTargetDir(projectConfig.defaultTargetFolder))
            .join("${projectConfig.name}Styles.$extension")
            .toString()
    }

    override fun getFontRootFolder(): String {
        val fontConfigPath = projectConfig.paths.fonts

        return IcmPath.root().join("Interactive").join(projectConfig.interactiveTenant)
            .join(fontConfigPath.orDefault("Resources/Fonts")).toString()
    }

    override fun applyImageAlternateText(layout: Layout, image: Image, alternateText: String) {
        // This method receives the DTO Image but needs to set alternate text on the WfdXml image
        // The actual image setting is done in the InspireDocumentObjectBuilder when building
        // For Interactive, we just create the variable, the image reference is handled elsewhere
    }

    override fun buildDocumentObject(documentObject: DocumentObject, styleDefinitionPath: String?): String {
        logger.debug("Starting to build document object '${documentObject.nameOrId()}'.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        if (documentObject.subject != null) {
            val root = layout.addRoot()
            root.setSubject(documentObject.subject)
        }

        val baseTemplatePath = getBaseTemplateFullPath(projectConfig, documentObject.baseTemplate)

        val baseTemplateExists = baseTemplateExistenceCache.getOrPut(baseTemplatePath) {
            ipsService.fileExists(baseTemplatePath.toString())
        }
        if (!baseTemplateExists) {
            error("Unable to deploy document object ${documentObject.id}. Base template '$baseTemplatePath' does not exist.")
        }

        val languages = collectLanguages(documentObject)
        val variableStructure = initVariableStructure(layout, documentObject)

        val interactiveFlowsWithContent = mutableMapOf<String, MutableList<DocumentContent>>()
        if (documentObject.type == DocumentObjectType.Page) {
            documentObject.content.paragraphIfEmpty().forEach {
                if (it is Area && !it.interactiveFlowName.isNullOrBlank()) {
                    val flowName = it.interactiveFlowName!!
                    val interactiveFlowId = if (flowName.startsWith("Def.")) {
                        flowName
                    } else {
                        getInteractiveFlowIdByName(flowName, baseTemplatePath.toString())
                    }

                    if (interactiveFlowId.isNullOrBlank()) {
                        val errorMessage =
                            "Failed to find interactive flow '$flowName' in base template '$baseTemplatePath'."
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
            interactiveFlowsWithContent[mainFlowId] = documentObject.content.toMutableList()
        }

        interactiveFlowsWithContent.forEach {
            val interactiveFlowText =
                layout.addFlow().setId(it.key).setType(Flow.Type.SIMPLE).setSectionFlow(true).addParagraph().addText()

            val contentFlows =
                buildDocumentContentAsFlows(layout, variableStructure, it.value, documentObject.nameOrId(), languages)

            if (documentObject.displayRuleRef == null) {
                contentFlows.forEach { contentFlow -> interactiveFlowText.appendFlow(contentFlow) }
            } else {
                interactiveFlowText.appendFlow(
                    contentFlows.toSingleFlow(
                        layout, variableStructure, documentObject.nameOrId(), documentObject.displayRuleRef?.let { DisplayRuleRef(it.id) }
                    )
                )
            }
        }

        logger.debug("Successfully built document object '${documentObject.nameOrId()}'")
        return builder.buildLayoutDelta()
    }

    override fun shouldIncludeInternalDependency(documentObject: DocumentObject): Boolean {
        return documentObject.internal == true
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

    @Serializable
    data class CustomProperty(
        val customName: String? = null
    )
}