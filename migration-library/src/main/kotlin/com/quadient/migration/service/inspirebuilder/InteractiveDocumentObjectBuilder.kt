package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Attachment
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
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.orDefault
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Image as WfdXmlImage
import com.quadient.wfdxml.api.layoutnodes.data.DataType
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind
import com.quadient.wfdxml.api.module.Layout
import kotlinx.serialization.SerialName
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
    attachmentRepository: Repository<Attachment>,
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
    attachmentRepository,
    projectConfig,
    ipsService,
) {
    private val lenientJson = Json { ignoreUnknownKeys = true }
    private val mainFlowId = "Def.MainFlow"

    private val xmlMapper by lazy { XmlMapper().registerKotlinModule() }
    private val baseTemplateCache = mutableMapOf<IcmPath, BaseTemplateData?>()
    private var currentBaseTemplateData: BaseTemplateData? = null
    private val styleDefinitionData: StyleDefinitionData? by lazy {
        val path = getStyleDefinitionPath("jld")
        if (!ipsService.fileExists(path)) {
            logger.warn("Style definition '$path' does not exist. Style display name resolution will be skipped.")
            return@lazy null
        }
        try {
            parseStyleDefinitionData(ipsService.wfd2xml(path))
        } catch (e: Exception) {
            logger.warn("Failed to load style definition data from '$path'.", e)
            null
        }
    }

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

    override fun getAttachmentPath(
        id: String, name: String?, targetFolder: IcmPath?, sourcePath: String?, attachmentType: AttachmentType
    ): String {
        val baseAttachmentName = name ?: id
        val attachmentName = appendExtensionIfMissing(baseAttachmentName, sourcePath)

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(attachmentName).toString()
        }

        val fileConfigPath = when (attachmentType) {
            AttachmentType.Attachment -> projectConfig.paths.attachments.orDefault("Attachments")
            AttachmentType.Document -> projectConfig.paths.documents.orDefault("Documents")
        }

        return IcmPath.root().join("Interactive").join(projectConfig.interactiveTenant).join(fileConfigPath)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(attachmentName).toString()
    }

    override fun getAttachmentPath(attachment: Attachment): String =
        getAttachmentPath(attachment.id, attachment.name, attachment.targetFolder?.let { IcmPath.from(it) }, attachment.sourcePath, attachment.attachmentType)

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

    override fun applyImageAlternateText(layout: Layout, image: WfdXmlImage, alternateText: String) {
        val variable = layout.data.addVariable()
            .setName("Alternate text variable for ${image.name}")
            .setKind(VariableKind.CALCULATED)
            .setDataType(DataType.STRING)
            .setScript("return ${toScriptStringLiteral(alternateText)};")
            .addCustomProperty("ValueWrapperVariable", true)
        val layoutRoot = layout.root ?: layout.addRoot()
        layoutRoot.addLockedWebNode(variable)

        image.setAlternateTextVariable(variable)
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
        currentBaseTemplateData = getOrLoadBaseTemplateData(baseTemplatePath)
            ?: error("Unable to deploy document object ${documentObject.id}. Base template '$baseTemplatePath' does not exist.")

        val languages = collectLanguages(documentObject)
        val variableStructure = initVariableStructure(layout, documentObject)

        addPdfMetadataToPages(layout, documentObject, variableStructure)

        val interactiveFlowsWithContent = mutableMapOf<String, MutableList<DocumentContent>>()
        if (documentObject.type == DocumentObjectType.Page) {
            documentObject.content.paragraphIfEmpty().forEach {
                if (it is Area && !it.interactiveFlowName.isNullOrBlank()) {
                    val flowName = it.interactiveFlowName!!
                    val interactiveFlowId = if (flowName.startsWith("Def.")) {
                        flowName
                    } else {
                        currentBaseTemplateData!!.interactiveFlowNamesToIds[flowName]
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

    override fun resolveParagraphStyleName(name: String): String =
        styleDefinitionData?.paragraphStyleDisplayNamesToNames?.get(name) ?: name

    override fun resolveTextStyleName(name: String): String =
        styleDefinitionData?.textStyleDisplayNamesToNames?.get(name) ?: name

    private fun getOrLoadBaseTemplateData(path: IcmPath): BaseTemplateData? {
        if (baseTemplateCache.containsKey(path)) return baseTemplateCache[path]

        if (!ipsService.fileExists(path.toString())) {
            baseTemplateCache[path] = null
            return null
        }

        return try {
            val xml = ipsService.wfd2xml(path.toString())
            parseBaseTemplateData(xml).also { baseTemplateCache[path] = it }
        } catch (e: Exception) {
            logger.warn("Failed to load base template data from '$path'.", e)
            baseTemplateCache[path] = null
            null
        }
    }

    private fun parseBaseTemplateData(xml: String): BaseTemplateData {
        val layoutXmlTree = xmlMapper.readTree(xml.trimIndent())["Layout"]["Layout"]
        return BaseTemplateData(parseInteractiveFlowNamesToIds(layoutXmlTree))
    }

    private fun parseStyleDefinitionData(xml: String): StyleDefinitionData {
        val layoutXmlTree = xmlMapper.readTree(xml.trimIndent())["Layout"]["Layout"]
        return StyleDefinitionData(
            parseStyleDisplayNamesToNames(layoutXmlTree, "TextStyle"),
            parseStyleDisplayNamesToNames(layoutXmlTree, "ParagraphStyle"),
        )
    }

    private fun parseInteractiveFlowNamesToIds(layoutXmlTree: JsonNode): Map<String, String> {
        val pagesInteractiveFlowNode = layoutXmlTree["Pages"]?.get("InteractiveFlow") ?: return emptyMap()
        val flowNodes = layoutXmlTree["Flow"] ?: return emptyMap()

        val interactiveFlowIds = if (pagesInteractiveFlowNode is ArrayNode) {
            pagesInteractiveFlowNode.map { it["FlowId"].textValue() }
        } else {
            listOf(pagesInteractiveFlowNode["FlowId"].textValue())
        }

        val result = mutableMapOf<String, String>()
        interactiveFlowIds.forEachIndexed { i, id ->
            val flowData = flowNodes.first { flow -> flow["Id"].textValue() == id }
            flowData["Name"]?.textValue()?.let { result[it] = "Def.InteractiveFlow$i" }

            flowData["CustomProperty"]?.textValue()?.let { raw ->
                lenientJson.decodeFromString<FlowCustomProperty>(raw).customName?.let {
                    result[it] = "Def.InteractiveFlow$i"
                }
            }
        }
        return result
    }

    private fun parseStyleDisplayNamesToNames(layoutXmlTree: JsonNode, nodeTag: String): Map<String, String> {
        val styleNode = layoutXmlTree[nodeTag] ?: return emptyMap()
        val styleNodeList = if (styleNode is ArrayNode) styleNode.toList() else listOf(styleNode)
        val result = mutableMapOf<String, String>()
        styleNodeList.forEach { node ->
            val name = node["Name"]?.textValue() ?: return@forEach
            val raw = node["CustomProperty"]?.textValue() ?: return@forEach
            val displayName = lenientJson.decodeFromString<StyleCustomProperty>(raw).displayName ?: return@forEach
            result.putIfAbsent(displayName, name)
        }
        return result
    }

    private data class BaseTemplateData(
        val interactiveFlowNamesToIds: Map<String, String>,
    )

    private data class StyleDefinitionData(
        val textStyleDisplayNamesToNames: Map<String, String>,
        val paragraphStyleDisplayNamesToNames: Map<String, String>,
    )

    @Serializable
    private data class FlowCustomProperty(
        val customName: String? = null,
    )

    @Serializable
    private data class StyleCustomProperty(
        @SerialName("DisplayName") val displayName: String? = null,
    )
}
