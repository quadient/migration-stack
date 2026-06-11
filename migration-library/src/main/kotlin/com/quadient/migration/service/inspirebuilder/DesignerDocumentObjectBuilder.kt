package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.service.IcmDataCache
import com.quadient.migration.service.ResourcePathProvider
import com.quadient.migration.service.resolveAliases
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.shared.ShapePath
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.millimeters
import com.quadient.migration.shared.toIcmPath
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.FlowArea
import com.quadient.wfdxml.api.layoutnodes.Page
import com.quadient.wfdxml.api.layoutnodes.Pages
import com.quadient.wfdxml.api.layoutnodes.tables.GeneralRowSet
import com.quadient.wfdxml.api.layoutnodes.tables.RowSet
import com.quadient.wfdxml.api.layoutnodes.Image as WfdXmlImage
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
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory.newInstance
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class DesignerDocumentObjectBuilder(
    documentObjectRepository: DocumentObjectRepository,
    textStyleRepository: TextStyleRepository,
    paragraphStyleRepository: ParagraphStyleRepository,
    variableRepository: VariableRepository,
    variableStructureRepository: VariableStructureRepository,
    displayRuleRepository: DisplayRuleRepository,
    imageRepository: ImageRepository,
    attachmentRepository: AttachmentRepository,
    resourcePathProvider: ResourcePathProvider,
    projectConfig: ProjectConfig,
    icmDataCache: IcmDataCache,
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
    resourcePathProvider,
    projectConfig.inspireOutput,
    icmDataCache,
) {
    private val resolvedStyleDefinitionPath: IcmPath? by lazy {
        val path = resourcePathProvider.getStyleDefinitionPath()
        try {
            if (icmDataCache.fileExists(path)) path else null
        } catch (e: Exception) {
            throw RuntimeException("Failed to check for style definition existence", e)
        }
    }

    val defaultPosition = Position(15.millimeters(), 15.millimeters(), 180.millimeters(), 267.millimeters())

    override fun applyImageAlternateText(layout: Layout, image: WfdXmlImage, alternateText: String) {
        image.setAlternateText(alternateText)
    }

    override fun buildDocumentObject(documentObject: DocumentObject): String {
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.name = "DocumentLayout"

        val pageModels = mutableListOf<DocumentObject>()
        val virtualPageContent = mutableListOf<DocumentContent>()

        val variableStructure = initVariableStructure(layout, documentObject.variableStructureRef?.id)
        val languages = collectLanguages(documentObject)

        val languageVariable = variableStructure.languageVariable
        if (languageVariable != null) {
            val languageVariableModel = variableRepository.findOrFail(languageVariable.id)
            val languageVariablePath = variableStructure.structure[languageVariable.id]?.path
                ?.resolve(variableStructure, variableRepository::findOrFail)?.takeIf { it.isNotBlank() }
            if (languageVariablePath.isNullOrBlank()) {
                error("Language variable '${languageVariable.id}' or its path not found in variable structure '${variableStructure.id}'.")
            }
            val variable = getOrCreateVariable(layout.data, languageVariableModel.nameOrId(), languageVariableModel, languageVariablePath)
            layout.data.setLanguageVariable(variable)
        }

        addPdfMetadataToPages(layout, documentObject, variableStructure)

        documentObject.content.paragraphIfEmpty().forEach {
            if (it is DocumentObjectRef) {
                val documentObjectModel = documentObjectRepository.findOrFail(it.id)
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
            if (it.skip.skipped == true && it.skip.placeholder != null) {
                val page = layout.addPage().setName("Page (skipped)").setType(Pages.PageConditionType.SIMPLE)
                val flow = layout.addFlow().setType(Flow.Type.SIMPLE)
                flow.addParagraph().addText().appendText("Skipped page: '${it.nameOrId()}'. Placeholder: ${it.skip.placeholder}")
                page.addFlowArea().setPosX(defaultPosition.x.toMeters()).setPosY(defaultPosition.y.toMeters())
                    .setWidth(defaultPosition.width.toMeters()).setHeight(defaultPosition.height.toMeters())
                    .setFlow(flow)
            } else if (it.skip.skipped != true) {
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

        val root = (layout.root ?: layout.addRoot()).setAllowRuntimeModifications(true)
        if (resolvedStyleDefinitionPath != null) {
            root.setExternalStylesLayout(resolvedStyleDefinitionPath.toString())
        }

        buildTextStyles(layout, textStyleRepository.listAll().filter { it.targetId == null })
        buildParagraphStyles(layout, paragraphStyleRepository.listAll().filter { it.targetId == null })

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
            enrichLayoutWithSourceBaseTemplate(documentObjectXml, projectConfig.sourceBaseTemplatePath.toIcmPath())
        }
    }

    override fun shouldIncludeInternalDependency(documentObject: DocumentObject): Boolean {
        return documentObject.internal == true || documentObject.type == DocumentObjectType.Page
    }

    override fun wrapSuccessFlowInConditionFlow(
        layout: Layout, variableStructure: VariableStructure, rule: DisplayRule, successFlow: Flow,
    ): Flow {
        return layout.addFlow().setType(Flow.Type.SELECT_BY_INLINE_CONDITION).addLineForSelectByInlineCondition(
            rule.toScript(
                layout,
                variableStructure,
                variableRepository::findOrFail,
                displayRuleRepository::findOrFail,
                resourcePathProvider::getDisplayRulePath,
                InspireOutput.Designer,
                projectConfig.interactiveTenant,
            ),
            successFlow
        )
    }

    override fun buildConditionRow(
        layout: Layout,
        variableStructure: VariableStructure,
        rule: DisplayRule,
        innerRowSet: GeneralRowSet?,
    ): GeneralRowSet {
        val successRow = innerRowSet ?: layout.addRowSet().setType(RowSet.Type.SINGLE_ROW)
        return layout.addRowSet().setType(RowSet.Type.SELECT_BY_INLINE_CONDITION)
            .addLineForSelectByInlineCondition(
                rule.toScript(
                    layout,
                    variableStructure,
                    variableRepository::findOrFail,
                    displayRuleRepository::findOrFail,
                    resourcePathProvider::getDisplayRulePath,
                    InspireOutput.Designer,
                    projectConfig.interactiveTenant,
                ),
                successRow
            )
    }

    override fun buildDocumentObjectRef(
        documentModel: DocumentObject,
        layout: Layout,
        variableStructure: VariableStructure,
        documentObjectRef: DocumentObjectRef,
        languages: List<String>
    ): Flow? {
        val flow = getFlowByName(layout, documentModel.nameOrId())
            ?: if (documentModel.type == DocumentObjectType.Snippet) {
                buildDocumentContentAsSingleFlow(
                    layout,
                    variableStructure,
                    documentModel.content,
                    documentModel.nameOrId(),
                    documentModel.displayRuleRef?.let { DisplayRuleRef(it.id) },
                    languages,
                    isInline = true
                )
            } else if (documentModel.internal == true) {
                buildDocumentContentAsSingleFlow(
                    layout,
                    variableStructure,
                    documentModel.content,
                    documentModel.nameOrId(),
                    documentModel.displayRuleRef?.let { DisplayRuleRef(it.id) },
                    languages
                )
            } else {
                layout.addFlow().setName(documentModel.nameOrId()).setType(Flow.Type.DIRECT_EXTERNAL)
                    .setLocation(resourcePathProvider.getDocumentObjectPath(documentModel).toString())
            }

        if (documentObjectRef.displayRuleRef != null) {
            val displayRule = displayRuleRepository.findOrFail(documentObjectRef.displayRuleRef.id)

            return wrapSuccessFlowInConditionFlow(layout, variableStructure, displayRule, flow)
        }

        return flow
    }

    private fun buildPage(
        layout: Layout,
        variableStructure: VariableStructure,
        name: String,
        content: List<DocumentContent>,
        mainObject: DocumentObject,
        options: PageOptions? = null,
        languages: List<String>,
    ) {
        val page = layout.addPage().setName(name).setType(Pages.PageConditionType.SIMPLE)
        options?.height?.let { page.setHeight(it.toMeters()) }
        options?.width?.let { page.setWidth(it.toMeters()) }

        val pageContentModels = mutableListOf<PageContent>()
        val virtualAreaContent = mutableListOf<DocumentContent>()

        content.forEach {
            when (it) {
                is Area -> pageContentModels.add(PageContent.AreaContent(it))
                is Shape -> pageContentModels.add(PageContent.PathObjectContent(it))
                is Barcode -> pageContentModels.add(PageContent.BarCodeContent(it))
                else -> virtualAreaContent.add(it)
            }
        }

        for (model in pageContentModels) {
            when (model) {
                is PageContent.AreaContent -> {
                    buildArea(layout, variableStructure, page, model.content, mainObject, languages)
                }
                is PageContent.PathObjectContent -> {
                    buildPathObject(layout, page, model.content)
                }
                is PageContent.BarCodeContent -> {
                    model.content.buildContent(variableRepository, page.barcodeFactory, layout, variableStructure, false)
                }
            }
        }

        if (virtualAreaContent.isNotEmpty()) {
            buildArea(
                layout,
                variableStructure,
                page,
                Area(virtualAreaContent, defaultPosition, null),
                mainObject,
                languages
            )
        }
    }

    private fun buildPathObject(layout: Layout, page: Page, model: Shape) {
        val pathObject = page.addPathObject()
            .setPosX(model.position.x.toMeters())
            .setPosY(model.position.y.toMeters())
            .setWidth(model.position.width.toMeters())
            .setHeight(model.position.height.toMeters())

        if (model.name != null) {
            pathObject.setName(model.name)
        }

        for (path in model.paths) {
            when (path) {
                is ShapePath.MoveTo -> pathObject.addMoveTo(path.x.toMeters(), path.y.toMeters())
                is ShapePath.LineTo -> pathObject.addLineTo(path.x.toMeters(), path.y.toMeters())
                is ShapePath.BezierTo -> pathObject.addBezierTo(
                    path.x2.toMeters(), path.y2.toMeters(),
                    path.x1.toMeters(), path.y1.toMeters(),
                    path.x0.toMeters(), path.y0.toMeters(),
                )
                is ShapePath.ConicTo -> pathObject.addConicTo(
                    path.x1.toMeters(), path.y1.toMeters(),
                    path.x0.toMeters(), path.y0.toMeters(),
                )
            }
        }

        pathObject.setLineWidth(model.lineWidth.toMeters())

        if (model.fill != null) {
            pathObject.setFillStyle(model.fill.resolve(layout))
        }

        if (model.lineFill != null) {
            pathObject.setLineFillStyle(model.lineFill.resolve(layout))
        }
    }

    private fun buildArea(
        layout: Layout,
        variableStructure: VariableStructure,
        page: Page,
        areaModel: Area,
        mainObject: DocumentObject,
        languages: List<String>
    ) {
        val position = areaModel.position ?: defaultPosition

        val content = areaModel.content.resolveAliases(imageRepository, attachmentRepository)
        if (content.size == 1 && content.first() is ImageRef) {
            val imageRef = content.first() as ImageRef
            val imageModel = imageRepository.findOrFail(imageRef.id)

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
            val areaFlow = buildDocumentContentAsSingleFlow(
                layout,
                variableStructure,
                areaModel.content,
                displayRuleRef = mainObject.displayRuleRef,
                languages = languages
            )

            val sectionAreaFlow = if (areaFlow.isSectionFlow) areaFlow else layout.addFlow().setType(Flow.Type.SIMPLE)
                .setSectionFlow(true).also {
                    it.addParagraph().addText().appendFlow(areaFlow)
                }

            page.addFlowArea().setPosX(position.x.toMeters()).setPosY(position.y.toMeters())
                .setWidth(position.width.toMeters()).setHeight(position.height.toMeters()).setFlow(sectionAreaFlow)
                .setFlowToNextPage(areaModel.flowToNextPage)
        }
    }

    private fun enrichLayoutWithSourceBaseTemplate(documentObjectXml: String, sourceBaseTemplatePath: IcmPath): String {
        val sourceBaseTemplateXml = icmDataCache.wfd2Xml(sourceBaseTemplatePath)

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

    private sealed interface PageContent {
        data class AreaContent(val content: Area): PageContent
        data class PathObjectContent(val content: Shape): PageContent
        data class BarCodeContent(val content: Barcode): PageContent
    }
}
