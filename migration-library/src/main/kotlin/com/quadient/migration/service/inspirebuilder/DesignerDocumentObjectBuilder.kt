package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.data.AreaModel
import com.quadient.migration.data.DocumentContentModel
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.FileModel
import com.quadient.migration.data.ImageModel
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphStyleDefinitionModel
import com.quadient.migration.data.TextStyleDefinitionModel
import com.quadient.migration.data.VariableStructureModel
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.FileInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository
import com.quadient.migration.service.imageExtension
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.resolveTargetDir
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.millimeters
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.FlowArea
import com.quadient.wfdxml.api.layoutnodes.Image
import com.quadient.wfdxml.api.layoutnodes.Page
import com.quadient.wfdxml.api.layoutnodes.Pages
import com.quadient.wfdxml.api.layoutnodes.tables.GeneralRowSet
import com.quadient.wfdxml.api.layoutnodes.tables.RowSet
import com.quadient.wfdxml.api.module.Layout
import com.quadient.wfdxml.internal.layoutnodes.FlowAreaImpl
import com.quadient.wfdxml.internal.layoutnodes.PageImpl
import com.quadient.wfdxml.internal.layoutnodes.PagesImpl
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory.newInstance
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class DesignerDocumentObjectBuilder(
    documentObjectRepository: DocumentObjectInternalRepository,
    textStyleRepository: TextStyleInternalRepository,
    paragraphStyleRepository: ParagraphStyleInternalRepository,
    variableRepository: VariableInternalRepository,
    variableStructureRepository: VariableStructureInternalRepository,
    displayRuleRepository: DisplayRuleInternalRepository,
    imageRepository: ImageInternalRepository,
    fileRepository: FileInternalRepository,
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
    private val sourceBaseTemplateCache = ConcurrentHashMap<String, String>()

    val defaultPosition = Position(15.millimeters(), 15.millimeters(), 180.millimeters(), 267.millimeters())

    override fun getDocumentObjectPath(nameOrId: String, type: DocumentObjectType, targetFolder: IcmPath?): String {
        val fileName = "$nameOrId.wfd"

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName).toString()
        }

        return IcmPath.root().join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(fileName)
            .toString()
    }

    override fun getDocumentObjectPath(documentObject: DocumentObjectModel) =
        getDocumentObjectPath(documentObject.nameOrId(), documentObject.type, documentObject.targetFolder)

    override fun getImagePath(
        id: String, imageType: ImageType, name: String?, targetFolder: IcmPath?, sourcePath: String?
    ): String {
        val fileName = "${name ?: id}${imageExtension(imageType, name, sourcePath)}"

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName).toString()
        }

        val imageConfigPath = projectConfig.paths.images

        return IcmPath.root().join(imageConfigPath)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(fileName).toString()
    }

    override fun getImagePath(image: ImageModel) =
        getImagePath(image.id, image.imageType, image.name, image.targetFolder, image.sourcePath)

    override fun getFilePath(
        id: String, name: String?, targetFolder: IcmPath?, sourcePath: String?, fileType: FileType
    ): String {
        val fileName = name ?: id

        if (targetFolder?.isAbsolute() == true) {
            return targetFolder.join(fileName).toString()
        }

        val fileConfigPath = when (fileType) {
            FileType.Document -> projectConfig.paths.documents
            FileType.Attachment -> projectConfig.paths.attachments
        }

        return IcmPath.root().join(fileConfigPath)
            .join(resolveTargetDir(projectConfig.defaultTargetFolder, targetFolder)).join(fileName).toString()
    }

    override fun getFilePath(file: FileModel): String =
        getFilePath(file.id, file.name, file.targetFolder, file.sourcePath, file.fileType)

    override fun getStyleDefinitionPath(extension: String): String {
        val styleDefinitionPath = projectConfig.styleDefinitionPath

        if (styleDefinitionPath != null && !styleDefinitionPath.isAbsolute()) {
            throw IllegalArgumentException("The configured style definition path '${styleDefinitionPath}' is not absolute.")
        } else if (styleDefinitionPath != null) {
            return styleDefinitionPath.toString()
        }

        return IcmPath.root().join(resolveTargetDir(projectConfig.defaultTargetFolder))
            .join("${projectConfig.name}Styles.$extension").toString()
    }

    override fun getFontRootFolder(): String {
        val fontConfigPath = projectConfig.paths.fonts

        return IcmPath.root().join(fontConfigPath).toString()
    }

    override fun applyImageAlternateText(layout: Layout, image: Image, alternateText: String) {
        image.setAlternateText(alternateText)
    }

    override fun buildDocumentObject(documentObject: DocumentObjectModel, styleDefinitionPath: String?): String {
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.name = "DocumentLayout"

        val pageModels = mutableListOf<DocumentObjectModel>()
        val virtualPageContent = mutableListOf<DocumentContentModel>()

        if (fontDataCache.isEmpty()) {
            val fontDataString = ipsService.gatherFontData(getFontRootFolder())
            fontDataCache.putAll(fontDataStringToMap(fontDataString))
        }
        val variableStructure = initVariableStructure(layout, documentObject)
        val languages = collectLanguages(documentObject)

        val languageVariable = variableStructure.languageVariable
        if (languageVariable != null) {
            val languageVariableModel = variableRepository.findModelOrFail(languageVariable.id)
            val languageVariablePathData = variableStructure.structure[languageVariable]
            if (languageVariablePathData == null || languageVariablePathData.path.isBlank()) {
                error("Language variable '${languageVariable.id}' or its path not found in variable structure '${variableStructure.id}'.")
            }
            val variable = getOrCreateVariable(layout.data, languageVariableModel.nameOrId(), languageVariableModel, languageVariablePathData.path)
            layout.data.setLanguageVariable(variable)
        }

        documentObject.content.paragraphIfEmpty().forEach {
            if (it is DocumentObjectModelRef) {
                val documentObjectModel = documentObjectRepository.findModelOrFail(it.id)
                if (documentObjectModel.type == DocumentObjectType.Page) {
                    pageModels.add(documentObjectModel)
                } else {
                    virtualPageContent.add(it)
                }
            } else {
                virtualPageContent.add(it)
            }
        }

        pageModels.forEach {
            if (it.skip.skipped && it.skip.placeholder != null) {
                val page = layout.addPage().setName("Page (skipped)").setType(Pages.PageConditionType.SIMPLE)
                val flow = layout.addFlow().setType(Flow.Type.SIMPLE)
                flow.addParagraph().addText().appendText("Skipped page: '${it.nameOrId()}'. Placeholder: ${it.skip.placeholder}")
                page.addFlowArea().setPosX(defaultPosition.x.toMeters()).setPosY(defaultPosition.y.toMeters())
                    .setWidth(defaultPosition.width.toMeters()).setHeight(defaultPosition.height.toMeters())
                    .setFlow(flow)
            } else if (!it.skip.skipped) {
                buildPage(
                    layout,
                    variableStructure,
                    it.nameOrId(),
                    it.content,
                    documentObject,
                    it.options as? PageOptions,
                    languages,
                )
            }
        }

        if (virtualPageContent.isNotEmpty() || pageModels.isEmpty()) {
            buildPage(
                layout, variableStructure, "Virtual Page", virtualPageContent, documentObject, null, languages
            )
        }

        val root = layout.addRoot().setAllowRuntimeModifications(true)
        if (styleDefinitionPath != null) {
            root.setExternalStylesLayout(styleDefinitionPath)
        }

        buildTextStyles(
            layout, textStyleRepository.listAllModel().filter { it.definition is TextStyleDefinitionModel })
        buildParagraphStyles(
            layout, paragraphStyleRepository.listAllModel().filter { it.definition is ParagraphStyleDefinitionModel })

        val firstPageWithFlowArea =
            (layout.pages as PagesImpl).children.find { page -> (page as PageImpl).children.any { it is FlowArea } } as? PageImpl
        if (firstPageWithFlowArea != null) {
            val flowAreaFlow = (firstPageWithFlowArea.children.first { it is FlowArea } as FlowAreaImpl).flow
            layout.pages.setMainFlow(flowAreaFlow)
        }

        val documentObjectXml = builder.build()
        return if (projectConfig.sourceBaseTemplatePath.isNullOrBlank()) {
            documentObjectXml
        } else {
            enrichLayoutWithSourceBaseTemplate(documentObjectXml, projectConfig.sourceBaseTemplatePath)
        }
    }

    override fun shouldIncludeInternalDependency(documentObject: DocumentObjectModel): Boolean {
        return documentObject.internal || documentObject.type == DocumentObjectType.Page
    }

    override fun wrapSuccessFlowInConditionFlow(
        layout: Layout, variableStructure: VariableStructureModel, ruleDef: DisplayRuleDefinition, successFlow: Flow,
    ): Flow {
        return layout.addFlow().setType(Flow.Type.SELECT_BY_INLINE_CONDITION).addLineForSelectByInlineCondition(
            ruleDef.toScript(layout, variableStructure, variableRepository::findModelOrFail), successFlow
        )
    }

    override fun buildSuccessRowWrappedInConditionRow(
        layout: Layout,
        variableStructure: VariableStructureModel,
        ruleDef: DisplayRuleDefinition,
        multipleRowSet: GeneralRowSet,
    ): GeneralRowSet {
        val successRow = layout.addRowSet().setType(RowSet.Type.SINGLE_ROW)

        multipleRowSet.addRowSet(
            layout.addRowSet().setType(RowSet.Type.SELECT_BY_INLINE_CONDITION).addLineForSelectByInlineCondition(
                ruleDef.toScript(layout, variableStructure, variableRepository::findModelOrFail), successRow
            )
        )

        return successRow
    }

    private fun buildPage(
        layout: Layout,
        variableStructure: VariableStructureModel,
        name: String,
        content: List<DocumentContentModel>,
        mainObject: DocumentObjectModel,
        options: PageOptions? = null,
        languages: List<String>,
    ) {
        val page = layout.addPage().setName(name).setType(Pages.PageConditionType.SIMPLE)
        options?.height?.let { page.setHeight(it.toMeters()) }
        options?.width?.let { page.setWidth(it.toMeters()) }

        val areaModels = mutableListOf<AreaModel>()
        val virtualAreaContent = mutableListOf<DocumentContentModel>()

        content.forEach {
            if (it is AreaModel) {
                areaModels.add(it)
            } else {
                virtualAreaContent.add(it)
            }
        }

        areaModels.forEach { buildArea(layout, variableStructure, page, it, mainObject, languages) }

        if (virtualAreaContent.isNotEmpty()) {
            buildArea(
                layout,
                variableStructure,
                page,
                AreaModel(virtualAreaContent, defaultPosition, null),
                mainObject,
                languages
            )
        }
    }

    private fun buildArea(
        layout: Layout,
        variableStructure: VariableStructureModel,
        page: Page,
        areaModel: AreaModel,
        mainObject: DocumentObjectModel,
        languages: List<String>
    ) {
        val position = areaModel.position ?: defaultPosition

        val content = areaModel.content
        if (content.size == 1 && content.first() is ImageModelRef) {
            val imageRef = content.first() as ImageModelRef
            val imageModel = imageRepository.findModelOrFail(imageRef.id)

            when (val imagePlaceholder = getImagePlaceholder(imageModel)) {
                is ImagePlaceholderResult.Skip -> return
                is ImagePlaceholderResult.RenderAsNormal -> {
                    page.addImageArea().setPosX(position.x.toMeters()).setPosY(position.y.toMeters())
                        .setWidth(position.width.toMeters()).setHeight(position.height.toMeters())
                        .setImage(getOrBuildImage(layout, imageModel, imageModel.alternateText))
                }
                is ImagePlaceholderResult.Placeholder -> {
                    val flow = layout.addFlow()
                    flow.addParagraph().addText().appendText(imagePlaceholder.value)
                    page.addFlowArea().setPosX(position.x.toMeters()).setPosY(position.y.toMeters())
                        .setWidth(position.width.toMeters()).setHeight(position.height.toMeters()).setFlow(flow)
                }
            }
        } else {
            page.addFlowArea().setPosX(position.x.toMeters()).setPosY(position.y.toMeters())
                .setWidth(position.width.toMeters()).setHeight(position.height.toMeters()).setFlow(
                    buildDocumentContentAsSingleFlow(
                        layout,
                        variableStructure,
                        areaModel.content,
                        displayRuleRef = mainObject.displayRuleRef,
                        languages = languages
                    )
                )
        }
    }

    fun enrichLayoutWithSourceBaseTemplate(documentObjectXml: String, sourceBaseTemplatePath: String): String {
        val sourceBaseTemplateXml = sourceBaseTemplateCache.computeIfAbsent(sourceBaseTemplatePath) {
            ipsService.wfd2xml(it)
        }

        val sourceBaseTemplateDoc = sourceBaseTemplateXml.toXmlDocument()
        val documentObjectDoc = documentObjectXml.toXmlDocument()

        val sourceBaseLayoutNode = sourceBaseTemplateDoc.getElementsByTagName("Layout").item(0) as? Element
            ?: error("Source base template '$sourceBaseTemplatePath' does not contain a Layout element.")
        val sourceBaseInnerLayoutNode = sourceBaseLayoutNode.firstElementChildByTag("Layout")
            ?: error("Source base template '$sourceBaseTemplatePath' does not contain an inner Layout element.")

        val documentObjectInnerLayoutNode =
            documentObjectDoc.getElementsByTagName("Layout").item(0)?.firstElementChildByTag("Layout")
                ?.let { sourceBaseTemplateDoc.importNode(it, true) }
                ?: error("Document object does not contain an inner Layout element.")

        sourceBaseLayoutNode.replaceChild(documentObjectInnerLayoutNode, sourceBaseInnerLayoutNode)
        return sourceBaseTemplateDoc.toXmlString()
    }

    private fun String.toXmlDocument(): Document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(this)))

    private fun Document.toXmlString(): String {
        val result = StringWriter()
        val transformer = newInstance().newTransformer()
        transformer.transform(DOMSource(this), StreamResult(result))
        return result.toString().replace(Regex("<Value>([\\s\\S]*?)</Value>")) { matchResult ->
            val value = matchResult.groupValues[1]
            val encoded = value.replace("\n", "&#xa;").replace("\r", "")
            "<Value>$encoded</Value>"
        }
    }

    private fun Node.firstElementChildByTag(tag: String): Element? =
        (0 until childNodes.length).asSequence().map { childNodes.item(it) }.filterIsInstance<Element>()
            .firstOrNull { it.tagName == tag }
}