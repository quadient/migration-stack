package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.Hyperlink
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.Paragraph.Text
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.SelectByLanguage
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Table as TableDTO
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.FlowModel.*
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.ScriptResult
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.ScriptResult.*
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Binary
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.Function
import com.quadient.migration.shared.Group
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.LiteralOrFunctionCall
import com.quadient.migration.shared.ParagraphPdfTaggingRule as ParagraphPdfTaggingRuleModel
import com.quadient.migration.shared.SuperOrSubscript
import com.quadient.migration.shared.TabType
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Font
import com.quadient.wfdxml.api.layoutnodes.Image as WfdXmlImage
import com.quadient.wfdxml.api.layoutnodes.LocationType
import com.quadient.wfdxml.api.layoutnodes.Pages
import com.quadient.wfdxml.api.layoutnodes.ParagraphStyle
import com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.LineSpacingType.*
import com.quadient.wfdxml.api.layoutnodes.TabulatorType
import com.quadient.wfdxml.api.layoutnodes.data.Data
import com.quadient.wfdxml.api.layoutnodes.data.DataType
import com.quadient.wfdxml.api.layoutnodes.data.Variable as WfdXmlVariable
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind
import com.quadient.wfdxml.api.layoutnodes.flow.Text as WfdXmlText
import com.quadient.wfdxml.api.layoutnodes.font.SubFont
import com.quadient.wfdxml.api.layoutnodes.tables.GeneralRowSet
import com.quadient.wfdxml.api.layoutnodes.tables.RowSet
import com.quadient.wfdxml.api.layoutnodes.tables.Table as WfdXmlTable
import com.quadient.migration.shared.TablePdfTaggingRule
import com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.ParagraphPdfTaggingRule
import com.quadient.wfdxml.api.layoutnodes.TextStyle
import com.quadient.wfdxml.api.layoutnodes.TextStyleInheritFlag
import com.quadient.wfdxml.api.layoutnodes.TextStyleType
import com.quadient.wfdxml.api.layoutnodes.flow.Paragraph as WfdXmlParagraph
import com.quadient.wfdxml.api.module.Layout
import com.quadient.wfdxml.internal.data.WorkFlowTreeDefinition
import com.quadient.wfdxml.internal.layoutnodes.TextStyleImpl
import com.quadient.wfdxml.internal.layoutnodes.data.DataImpl
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ifEmpty
import com.quadient.migration.shared.DataType as DataTypeModel
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle as ParagraphStyleDTO
import com.quadient.migration.api.dto.migrationmodel.TextStyle as TextStyleDTO

abstract class InspireDocumentObjectBuilder(
    protected val documentObjectRepository: DocumentObjectRepository,
    protected val textStyleRepository: TextStyleRepository,
    protected val paragraphStyleRepository: ParagraphStyleRepository,
    protected val variableRepository: Repository<Variable>,
    protected val variableStructureRepository: Repository<VariableStructure>,
    protected val displayRuleRepository: Repository<DisplayRule>,
    protected val imageRepository: Repository<Image>,
    protected val attachmentRepository: Repository<Attachment>,
    protected val projectConfig: ProjectConfig,
    protected val ipsService: IpsService,
) {
    protected val logger = LoggerFactory.getLogger(this::class.java)!!

    protected val fontDataCache = ConcurrentHashMap<FontKey, String>()

    abstract fun getDocumentObjectPath(nameOrId: String, type: DocumentObjectType, targetFolder: IcmPath?): String

    abstract fun getDocumentObjectPath(documentObject: DocumentObject): String

    abstract fun getImagePath(
        id: String, imageType: ImageType, name: String?, targetFolder: IcmPath?, sourcePath: String?
    ): String

    abstract fun getImagePath(image: Image): String

    abstract fun getAttachmentPath(
        id: String, name: String?, targetFolder: IcmPath?, sourcePath: String?, attachmentType: AttachmentType
    ): String

    abstract fun getAttachmentPath(attachment: Attachment): String

    abstract fun getStyleDefinitionPath(extension: String = "wfd"): String

    abstract fun getFontRootFolder(): String

    abstract fun buildDocumentObject(documentObject: DocumentObject, styleDefinitionPath: String?): String

    abstract fun shouldIncludeInternalDependency(documentObject: DocumentObject): Boolean

    protected fun collectLanguages(documentObject: DocumentObject): List<String> {
        val languages = mutableSetOf<String>()

        fun collectLanguagesFromContent(content: List<DocumentContent>) {
            for (item in content) {
                when (item) {
                    is SelectByLanguage -> item.cases.forEach { languages.add(it.language) }
                    is Area -> collectLanguagesFromContent(item.content)
                    is FirstMatch -> {
                        item.cases.forEach { case -> collectLanguagesFromContent(case.content) }
                        collectLanguagesFromContent(item.default)
                    }

                    is TableDTO -> item.rows.forEach { row ->
                        row.cells.forEach { cell -> collectLanguagesFromContent(cell.content) }
                    }

                    is DocumentObjectRef -> {
                        val documentObject = documentObjectRepository.findOrFail(item.id)
                        if (shouldIncludeInternalDependency(documentObject)) {
                            collectLanguagesFromContent(documentObject.content)
                        }
                    }

                    is Paragraph -> item.content.forEach { textModel ->
                        textModel.content.forEach { textContent ->
                            when (textContent) {
                                is FirstMatch -> {
                                    textContent.cases.forEach { case -> collectLanguagesFromContent(case.content) }
                                    collectLanguagesFromContent(textContent.default)
                                }

                                is TableDTO -> textContent.rows.forEach { row ->
                                    row.cells.forEach { cell -> collectLanguagesFromContent(cell.content) }
                                }

                                is DocumentObjectRef -> {
                                    val documentObject = documentObjectRepository.findOrFail(textContent.id)
                                    if (shouldIncludeInternalDependency(documentObject)) {
                                        collectLanguagesFromContent(documentObject.content)
                                    }
                                }

                                else -> {}
                            }
                        }
                    }

                    else -> {}
                }
            }
        }

        collectLanguagesFromContent(documentObject.content)

        return languages.toList()
    }

    protected open fun wrapSuccessFlowInConditionFlow(
        layout: Layout, variableStructure: VariableStructure, ruleDef: DisplayRuleDefinition, successFlow: Flow
    ): Flow {
        return layout.addFlow().setType(Flow.Type.SELECT_BY_CONDITION).addLineForSelectByCondition(
            layout.data.addVariable().setKind(VariableKind.CALCULATED).setDataType(DataType.BOOL)
                .setScript(ruleDef.toScript(layout, variableStructure, variableRepository::findOrFail)),
            successFlow
        )
    }

    protected open fun buildSuccessRowWrappedInConditionRow(
        layout: Layout,
        variableStructure: VariableStructure,
        ruleDef: DisplayRuleDefinition,
        multipleRowSet: GeneralRowSet
    ): GeneralRowSet {
        val successRow = layout.addRowSet().setType(RowSet.Type.SINGLE_ROW)

        multipleRowSet.addRowSet(
            layout.addRowSet().setType(RowSet.Type.SELECT_BY_CONDITION).addLineForSelectByCondition(
                layout.data.addVariable().setKind(VariableKind.CALCULATED).setDataType(DataType.BOOL)
                    .setScript(ruleDef.toScript(layout, variableStructure, variableRepository::findOrFail)),
                successRow
            )
        )

        return successRow
    }

    fun buildStyleLayoutDelta(textStyles: List<TextStyleDTO>, paragraphStyles: List<ParagraphStyleDTO>): String {
        logger.debug("Starting to build style layout delta.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()

        if (fontDataCache.isEmpty()) {
            val fontDataString = ipsService.gatherFontData(getFontRootFolder())
            fontDataCache.putAll(fontDataStringToMap(fontDataString))
        }

        buildTextStyles(layout, textStyles)
        buildParagraphStyles(layout, paragraphStyles)

        logger.debug("Successfully built style layout delta.")
        return builder.buildStyleLayoutDelta()
    }

    fun buildStyles(
        textStyles: List<TextStyleDTO>,
        paragraphStyles: List<ParagraphStyleDTO>,
    ): String {
        logger.debug("Starting to build style definition.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()

        layout.setName("DocumentLayout")
        val flow = layout.addFlow().setSectionFlow(true).setWebEditingType(Flow.WebEditingType.SECTION)
        layout.pages.setMainFlow(flow)
        layout.addPage().setName("Page 1").setType(Pages.PageConditionType.SIMPLE)
        layout.addRoot().setAllowRuntimeModifications(true)

        if (fontDataCache.isEmpty()) {
            val fontDataString = ipsService.gatherFontData(getFontRootFolder())
            fontDataCache.putAll(fontDataStringToMap(fontDataString))
        }

        buildTextStyles(layout, textStyles)
        buildParagraphStyles(layout, paragraphStyles)

        logger.debug("Successfully built style definition.")
        return builder.build()
    }

    fun buildDocumentContentAsFlows(
        layout: Layout,
        variableStructure: VariableStructure,
        content: List<DocumentContent>,
        flowName: String? = null,
        languages: List<String>,
    ): List<Flow> {
        val mutableContent = content.toMutableList()

        var idx = 0
        val flowModels = mutableListOf<FlowModel>()
        while (idx < mutableContent.size) {
            when (val contentPart = mutableContent[idx]) {
                is TableDTO, is Paragraph, is ImageRef -> {
                    val flowParts = gatherFlowParts(mutableContent, idx)
                    idx += flowParts.size - 1
                    flowModels.add(Composite(flowParts))
                }

                is DocumentObjectRef -> flowModels.add(DocumentObject(contentPart))
                is AttachmentRef -> flowModels.add(Attachment(contentPart))
                is Area -> mutableContent.addAll(idx + 1, contentPart.content)
                is FirstMatch -> flowModels.add(FirstMatch(contentPart))
                is SelectByLanguage -> flowModels.add(SelectByLanguage(contentPart))
            }
            idx++
        }

        val flowCount = flowModels.size
        var flowSuffix = 1
        return flowModels.mapNotNull {
            when (it) {
                is FlowModel.DocumentObject -> buildDocumentObjectRef(layout, variableStructure, it.ref, languages)
                is FlowModel.Attachment -> buildAttachmentRef(layout, it.ref)
                is FlowModel.Composite -> {
                    if (flowName == null) {
                        buildCompositeFlow(layout, variableStructure, it.parts, null, languages)
                    } else {
                        val name = if (flowCount == 1) flowName else "$flowName $flowSuffix"
                        flowSuffix++
                        buildCompositeFlow(layout, variableStructure, it.parts, name, languages)
                    }
                }

                is FlowModel.FirstMatch -> {
                    if (flowName == null) {
                        buildFirstMatch(layout, variableStructure, it.model, false, null, languages)
                    } else {
                        val name = if (flowCount == 1) flowName else "$flowName $flowSuffix"
                        flowSuffix++
                        buildFirstMatch(layout, variableStructure, it.model, false, name, languages)
                    }
                }

                is FlowModel.SelectByLanguage -> {
                    if (flowName == null) {
                        buildSelectByLanguage(layout, variableStructure, it.model, null, languages)
                    } else {
                        val name = if (flowCount == 1) flowName else "$flowName $flowSuffix"
                        flowSuffix++
                        buildSelectByLanguage(layout, variableStructure, it.model, name, languages)
                    }
                }
            }
        }
    }

    sealed interface FlowModel {
        data class Composite(val parts: List<DocumentContent>) : FlowModel
        data class DocumentObject(val ref: DocumentObjectRef) : FlowModel
        data class Attachment(val ref: AttachmentRef) : FlowModel
        data class FirstMatch(val model: com.quadient.migration.api.dto.migrationmodel.FirstMatch) : FlowModel
        data class SelectByLanguage(val model: com.quadient.migration.api.dto.migrationmodel.SelectByLanguage) : FlowModel
    }

    protected fun List<Flow>.toSingleFlow(
        layout: Layout,
        variableStructure: VariableStructure,
        flowName: String? = null,
        displayRuleRef: DisplayRuleRef? = null,
    ): Flow {
        val singleFlow = if (this.size == 1) {
            this[0]
        } else {
            val sectionFlow = layout.addFlow().setType(Flow.Type.SIMPLE).setSectionFlow(true)
            flowName?.let { sectionFlow.setName(it) }

            val sectionFlowText = sectionFlow.addParagraph().addText()

            this.forEach { sectionFlowText.appendFlow(it) }

            sectionFlow
        }

        return if (displayRuleRef == null) {
            singleFlow
        } else {
            val displayRule = displayRuleRepository.findOrFail(displayRuleRef.id)
            val def = displayRule.definition
            if (def == null) {
                error("Display rule '${displayRuleRef.id}' definition is null.")
            }

            wrapSuccessFlowInConditionFlow(layout, variableStructure, def, singleFlow)
        }
    }

    protected fun buildDocumentContentAsSingleFlow(
        layout: Layout,
        variableStructure: VariableStructure,
        content: List<DocumentContent>,
        flowName: String? = null,
        displayRuleRef: DisplayRuleRef? = null,
        languages: List<String>,
    ): Flow {
        return buildDocumentContentAsFlows(layout, variableStructure, content, flowName, languages).toSingleFlow(
            layout, variableStructure, flowName, displayRuleRef
        )
    }

    protected fun initVariableStructure(layout: Layout, documentObject: DocumentObject): VariableStructure {
        val variableStructureId = documentObject.variableStructureRef?.id ?: projectConfig.defaultVariableStructure

        val variableStructureModel =
            variableStructureId?.let { variableStructureRepository.findOrFail(it) } ?: VariableStructure(
                id = "defaultVariableStructure",
                lastUpdated = Clock.System.now(),
                created = Clock.System.now(),
                structure = mutableMapOf(),
                customFields = CustomFieldMap(),
                languageVariable = null,
            )

        val normalizedVariablePaths = variableStructureModel.structure.map { (_, variablePathData) ->
            removeDataFromVariablePath(variablePathData.path)
        }.filter { it.isNotBlank() }

        val variableTree = buildVariableTree(normalizedVariablePaths)

        val workflowTreeDefinition = WorkFlowTreeDefinition("Root", SUB_TREE, NodeOptionality.ARRAY).also {
            buildVariablePathPart(it, variableTree)
        }

        val layoutData = layout.data
        layoutData.importDataDefinition(workflowTreeDefinition)
        if (variableTree.isNotEmpty() && variableTree.values.first() is ArrayVariable) {
            layoutData.setRepeatedBy("Data.${variableTree.keys.first()}")
        }

        return variableStructureModel
    }

    private fun buildVariablePathPart(
        parentNode: WorkFlowTreeDefinition, currentMap: Map<String, VariablePathPart>
    ) {
        currentMap.forEach {
            val variablePathPart = it.value
            val optionality =
                if (variablePathPart is ArrayVariable) NodeOptionality.ARRAY else NodeOptionality.MUST_EXIST

            val node = WorkFlowTreeDefinition(variablePathPart.name, SUB_TREE, optionality)
            parentNode.addSubNode(node)

            if (variablePathPart.children.isNotEmpty()) {
                buildVariablePathPart(node, variablePathPart.children)
            }
        }
    }

    private fun upsertSubFont(font: Font, isBold: Boolean, isItalic: Boolean): SubFont? {
        val subFontName = buildFontName(isBold, isItalic)

        val fontLocation = fontDataCache[FontKey(font.name, subFontName)]
            ?: fontDataCache[FontKey(font.name, buildFontName(bold = false, italic = false))]
            ?: return null

        font.subFonts.removeAll { it.name == subFontName }
        return font.addSubfont().setName(subFontName).setBold(isBold).setItalic(isItalic)
            .setLocation(fontLocation, LocationType.ICM)
    }

    fun buildTextStyles(layout: Layout, textStyleModels: List<TextStyleDTO>) {
        val arialFont = getFontByName(layout, "Arial")
        require(arialFont != null) { "Layout must contain Arial font." }
        arialFont.setName("Arial").setFontName("Arial")
        upsertSubFont(arialFont, isBold = false, isItalic = false)

        textStyleModels.forEach { styleModel ->
            val definition = styleModel.resolve()
            val textStyle = layout.addTextStyle().setName(styleModel.nameOrId())
            applyTextStyleProperties(layout, textStyle, definition)
        }
    }

    private fun applyTextStyleProperties(layout: Layout, textStyle: TextStyle, definition: TextStyleDefinition) {
        val fontFamily = definition.fontFamily ?: "Arial"

        val font = getFontByName(layout, fontFamily) ?: layout.addFont().setName(fontFamily).setFontName(fontFamily)
        textStyle.setFont(font)

        val subFont = upsertSubFont(font, definition.bold, definition.italic)
        if (subFont != null) {
            textStyle.setSubFont(subFont)
        }

        textStyle.setBold(definition.bold)
        textStyle.seItalic(definition.italic)
        textStyle.setUnderline(definition.underline)
        textStyle.setStrikeThrough(definition.strikethrough)

        definition.size?.let { textStyle.setFontSizeInMeters(it.toMeters()) }
        definition.interspacing?.let { textStyle.setInterSpacing(it.toMeters()) }

        when (definition.superOrSubscript) {
            SuperOrSubscript.Subscript -> textStyle.setSubScript(true).setSuperScript(false)
            SuperOrSubscript.Superscript -> textStyle.setSubScript(false).setSuperScript(true)
            SuperOrSubscript.None -> textStyle.setSubScript(false).setSuperScript(false)
        }

        definition.foregroundColor?.let { colorModel ->
            val layoutColor = getColorByRGB(layout, colorModel.red(), colorModel.green(), colorModel.blue())
                ?: layout.addColor().setRGB(colorModel.red(), colorModel.green(), colorModel.blue())
            val fillStyle = getFillStyleByColor(layout, layoutColor)
                ?: layout.addFillStyle().setColor(layoutColor)
            textStyle.setFillStyle(fillStyle)
        }
    }

    fun buildParagraphStyles(layout: Layout, paragraphStyleModels: List<ParagraphStyleDTO>) {
        paragraphStyleModels.forEach { styleModel ->
            val definition = styleModel.resolve()

            val paragraphStyle = layout.addParagraphStyle().setName(styleModel.nameOrId())

            definition.leftIndent?.let { paragraphStyle.setLeftIndent(it.toMeters()) }
            definition.rightIndent?.let { paragraphStyle.setRightIndent(it.toMeters()) }
            definition.defaultTabSize?.let { paragraphStyle.setDefaultTabSize(it.toMeters()) }
            definition.spaceBefore?.let { paragraphStyle.setSpaceBefore(it.toMeters()) }
            definition.spaceAfter?.let { paragraphStyle.setSpaceAfter(it.toMeters()) }
            val alignType = when (definition.alignment) {
                Alignment.Left -> ParagraphStyle.AlignType.LEFT
                Alignment.Right -> ParagraphStyle.AlignType.RIGHT
                Alignment.Center -> ParagraphStyle.AlignType.CENTER
                Alignment.JustifyLeft -> ParagraphStyle.AlignType.JUSTIFY_lEFT
                Alignment.JustifyRight -> ParagraphStyle.AlignType.JUSTIFY_RIGHT
                Alignment.JustifyCenter -> ParagraphStyle.AlignType.JUSTIFY_CENTER
                Alignment.JustifyBlock -> ParagraphStyle.AlignType.JUSTIFY_BLOCK
                Alignment.JustifyWithMargins -> ParagraphStyle.AlignType.JUSTIFY_WITH_MARGIN
                Alignment.JustifyBlockUniform -> ParagraphStyle.AlignType.JUSTIFY_BLOCK_UNIFORM
            }
            paragraphStyle.setAlignType(alignType)

            definition.firstLineIndent?.let { paragraphStyle.setFirstLineLeftIndent(it.toMeters()) }

            val lineSpacing = definition.lineSpacing
            val (lineSpacingType, lineSpacingValue) = when (lineSpacing) {
                is LineSpacing.Additional -> Pair(ADDITIONAL, lineSpacing.size?.toMeters())
                is LineSpacing.AtLeast -> Pair(AT_LEAST, lineSpacing.size?.toMeters())
                is LineSpacing.Exact -> Pair(EXACT, lineSpacing.size?.toMeters())
                is LineSpacing.ExactFromPrevious -> Pair(EXACT_FROM_PREVIOUS, lineSpacing.size?.toMeters())
                is LineSpacing.ExactFromPreviousWithAdjust -> Pair(
                    EXACT_FROM_PREVIOUS_WITH_ADJUST, lineSpacing.size?.toMeters()
                )

                is LineSpacing.ExactFromPreviousWithAdjustLegacy -> Pair(
                    EXACT_FROM_PREVIOUS_WITH_ADJUST_OLD, lineSpacing.size?.toMeters()
                )

                is LineSpacing.MultipleOf -> Pair(MULTIPLE_OF, lineSpacing.value)
            }
            paragraphStyle.setLineSpacingType(lineSpacingType)
            lineSpacingValue?.let { paragraphStyle.setLineSpacingValue(it) }

            definition.tabs?.let { tabsModel ->
                paragraphStyle.setUseOutsideTabs(tabsModel.useOutsideTabs)
                tabsModel.tabs.forEach { tabModel ->
                    val tabType = when (tabModel.type) {
                        TabType.Left -> TabulatorType.LEFT
                        TabType.Right -> TabulatorType.RIGHT
                        TabType.Center -> TabulatorType.CENTER
                        TabType.DecimalWord -> TabulatorType.WORD_DECIMAL
                        TabType.Decimal -> TabulatorType.DECIMAL
                    }

                    paragraphStyle.addTabulator(tabModel.position.toMeters(), tabType)
                }
            }

            definition.pdfTaggingRule?.let { pdfTaggingRule ->
                val rule = when (pdfTaggingRule) {
                    ParagraphPdfTaggingRuleModel.Paragraph -> ParagraphPdfTaggingRule.PARAGRAPH
                    ParagraphPdfTaggingRuleModel.Heading -> ParagraphPdfTaggingRule.HEADING
                    ParagraphPdfTaggingRuleModel.Heading1 -> ParagraphPdfTaggingRule.HEADING_1
                    ParagraphPdfTaggingRuleModel.Heading2 -> ParagraphPdfTaggingRule.HEADING_2
                    ParagraphPdfTaggingRuleModel.Heading3 -> ParagraphPdfTaggingRule.HEADING_3
                    ParagraphPdfTaggingRuleModel.Heading4 -> ParagraphPdfTaggingRule.HEADING_4
                    ParagraphPdfTaggingRuleModel.Heading5 -> ParagraphPdfTaggingRule.HEADING_5
                    ParagraphPdfTaggingRuleModel.Heading6 -> ParagraphPdfTaggingRule.HEADING_6
                }
                paragraphStyle.setPdfTaggingRule(rule)
            }
        }
    }

    sealed interface ImagePlaceholderResult {
        object RenderAsNormal : ImagePlaceholderResult
        object Skip : ImagePlaceholderResult
        data class Placeholder(val value: String) : ImagePlaceholderResult
    }
    protected fun getImagePlaceholder(imageModel: Image): ImagePlaceholderResult {
        if (imageModel.imageType == ImageType.Unknown && !imageModel.skip.skipped) {
            throw IllegalStateException(
                "Image '${imageModel.nameOrId()}' has unknown type and is not set to be skipped."
            )
        }
        if (imageModel.sourcePath.isNullOrBlank() && !imageModel.skip.skipped) {
            throw IllegalStateException(
                "Image '${imageModel.nameOrId()}' has missing source path and is not set to be skipped."
            )
        }

        if (imageModel.skip.skipped && imageModel.skip.placeholder != null) {
            return ImagePlaceholderResult.Placeholder(imageModel.skip.placeholder)
        } else if (imageModel.skip.skipped) {
            return ImagePlaceholderResult.Skip
        }

        return ImagePlaceholderResult.RenderAsNormal
    }

    protected fun getOrBuildImage(layout: Layout, imageModel: Image, alternateText: String? = null): WfdXmlImage {
        val image = getImageByName(layout, imageModel.nameOrId()) ?: layout.addImage().setName(imageModel.nameOrId())
            .setImageLocation(getImagePath(imageModel), LocationType.ICM)

        val options = imageModel.options
        if (options != null) {
            options.resizeWidth?.let { image.setResizeWidth(it.toMeters()) }
            options.resizeHeight?.let { image.setResizeHeight(it.toMeters()) }
        }

        if (!alternateText.isNullOrBlank()) {
            applyImageAlternateText(layout, image, alternateText)
        }

        return image
    }

    protected abstract fun applyImageAlternateText(layout: Layout, image: WfdXmlImage, alternateText: String)

    private fun buildAttachmentRef(
        layout: Layout,
        attachmentRef: AttachmentRef,
    ): Flow? {
        val attachmentModel = attachmentRepository.findOrFail(attachmentRef.id)

        if (attachmentModel.skip.skipped && attachmentModel.skip.placeholder == null) {
            val reason = attachmentModel.skip.reason?.let { "with reason: $it" } ?: "without reason"
            logger.debug("Attachment ${attachmentRef.id} is set to be skipped without placeholder $reason.")
            return null
        } else if (attachmentModel.skip.skipped && attachmentModel.skip.placeholder != null) {
            val reason = attachmentModel.skip.reason?.let { "and reason: $it" } ?: "without reason"
            logger.debug("Attachment ${attachmentRef.id} is set to be skipped with placeholder $reason.")
            val flow = layout.addFlow().setType(Flow.Type.SIMPLE)
            flow.addParagraph().addText().appendText(attachmentModel.skip.placeholder)
            return flow
        }

        if (attachmentModel.sourcePath.isNullOrBlank()) {
            throw IllegalStateException(
                "Attachment '${attachmentModel.nameOrId()}' has missing source path and is not set to be skipped."
            )
        }

        val flow = getFlowByName(layout, attachmentModel.nameOrId()) ?: run {
            layout.addFlow()
                .setName(attachmentModel.nameOrId())
                .setType(Flow.Type.DIRECT_EXTERNAL)
                .setLocation(getAttachmentPath(attachmentModel))
        }

        return flow
    }

    private fun buildCompositeFlow(
        layout: Layout,
        variableStructure: VariableStructure,
        documentContentModelParts: List<DocumentContent>,
        flowName: String? = null,
        languages: List<String>
    ): Flow {
        val flow = layout.addFlow().setType(Flow.Type.SIMPLE)
        flowName?.let { flow.setName(it) }

        documentContentModelParts.forEach {
            when (it) {
                is Paragraph -> buildParagraph(layout, variableStructure, flow, it, languages)
                is TableDTO -> flow.addParagraph().addText()
                    .appendTable(buildTable(layout, variableStructure, it, languages))

                is ImageRef -> buildAndAppendImage(layout, flow.addParagraph().addText(), it)
                else -> error("Content part type ${it::class.simpleName} is not allowed in composite flow.")
            }
        }

        return flow
    }

    private fun buildDocumentObjectRef(
        layout: Layout,
        variableStructure: VariableStructure,
        documentObjectRef: DocumentObjectRef,
        languages: List<String>,
    ): Flow? {
        val documentModel = documentObjectRepository.findOrFail(documentObjectRef.id)

        if (documentModel.skip.skipped && documentModel.skip.placeholder == null) {
            val reason = documentModel.skip.reason?.let { "with reason: $it" } ?: "without reason"
            logger.debug("Document content part ${documentObjectRef.id} is set to be skipped without placeholder $reason.")
            return null
        } else if (documentModel.skip.skipped && documentModel.skip.placeholder != null) {
            val reason = documentModel.skip.reason?.let { "and reason: $it" } ?: "without reason"
            logger.debug("Document content part ${documentObjectRef.id} is set to be skipped with placeholder $reason.")
            val flow = layout.addFlow().setType(Flow.Type.SIMPLE)
            flow.addParagraph().addText().appendText(documentModel.skip.placeholder)
            return flow
        }

        val flow = getFlowByName(layout, documentModel.nameOrId()) ?: if (documentModel.internal == true) {
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
                .setLocation(getDocumentObjectPath(documentModel))
        }

        if (documentObjectRef.displayRuleRef != null) {
            val displayRule = displayRuleRepository.findOrFail(documentObjectRef.displayRuleRef.id)
            val def = displayRule.definition
            if (def == null) {
                error("Display rule '${documentObjectRef.displayRuleRef.id}' definition is null.")
            }

            return wrapSuccessFlowInConditionFlow(layout, variableStructure, def, flow)
        }

        return flow
    }

    private fun gatherFlowParts(content: List<DocumentContent>, startIndex: Int): List<DocumentContent> {
        val flowParts = mutableListOf<DocumentContent>()

        var index = startIndex

        do {
            val contentPart = content[index]
            if (contentPart is TableDTO || contentPart is Paragraph || contentPart is ImageRef) {
                flowParts.add(contentPart)
                index++
            } else {
                break
            }
        } while (index < content.size)

        return flowParts
    }

    private fun buildParagraph(
        layout: Layout,
        variableStructure: VariableStructure,
        flow: Flow,
        paragraphModel: Paragraph,
        languages: List<String>
    ) {
        val paragraph = if (paragraphModel.displayRuleRef == null) {
            flow.addParagraph()
        } else {
            buildSuccessFlowWrappedInInlineConditionFlow(
                layout, variableStructure, paragraphModel.displayRuleRef.id, flow.addParagraph().addText()
            ).addParagraph()
        }

        val paragraphStyle = findParagraphStyle(paragraphModel)?.also {
            paragraph.setExistingParagraphStyle("ParagraphStyles.${it.nameOrId()}")
        }

        paragraphModel.content.forEach { textModel ->
            val baseText = if (textModel.displayRuleRef == null) {
                paragraph.addText()
            } else {
                buildSuccessFlowWrappedInInlineConditionFlow(
                    layout, variableStructure, textModel.displayRuleRef.id, paragraph.addText()
                ).addParagraph().also {
                    if (paragraphStyle != null) it.setExistingParagraphStyle("ParagraphStyles.${paragraphStyle.nameOrId()}")
                }.addText()
            }

            val baseTextStyleModel = findTextStyle(textModel)
            baseTextStyleModel?.also { baseText.setExistingTextStyle("TextStyles.${it.nameOrId()}") }

            var currentText = baseText

            textModel.content.forEach {
                when (it) {
                    is StringValue -> currentText.appendText(it.value)
                    is VariableRef -> currentText.appendVariable(it, layout, variableStructure)
                    is TableDTO -> currentText.appendTable(buildTable(layout, variableStructure, it, languages))
                    is DocumentObjectRef -> buildDocumentObjectRef(
                        layout, variableStructure, it, languages
                    )?.also { flow ->
                        currentText.appendFlow(flow)
                    }

                    is AttachmentRef -> buildAttachmentRef(layout, it)?.also { flow ->
                        currentText.appendFlow(flow)
                    }

                    is ImageRef -> buildAndAppendImage(layout, currentText, it)
                    is Hyperlink -> currentText = buildAndAppendHyperlink(layout, paragraph, baseTextStyleModel, it)
                    is FirstMatch -> currentText.appendFlow(
                        buildFirstMatch(layout, variableStructure, it, true, null, languages)
                    )
                }
            }
        }
    }

    private fun createHyperlinkTextStyle(
        layout: Layout, baseTextStyleModel: TextStyleDTO?, hyperlinkModel: Hyperlink
    ): TextStyle {
        val baseStyleName = baseTextStyleModel?.nameOrId() ?: "text"
        val hyperlinkName = generateUniqueHyperlinkStyleName(layout, baseStyleName)

        val urlVariable = createUrlVariable(layout, hyperlinkName, hyperlinkModel.url)

        val hyperlinkStyle = layout.addTextStyle()
            .setName(hyperlinkName)
            .setUrlTarget(urlVariable)
            .setType(TextStyleType.DELTA)
            .setUrlAlternateText(hyperlinkModel.alternateText)
            .addInheritFlags(
                *TextStyleInheritFlag.entries
                .filter { it != TextStyleInheritFlag.UNDERLINE && it != TextStyleInheritFlag.FILL_STYLE }
                .toTypedArray())
        (hyperlinkStyle as TextStyleImpl).ancestorId = "Def.TextStyleHyperlink"

        if (baseTextStyleModel != null) {
            val definition = baseTextStyleModel.resolve()
            applyTextStyleProperties(layout, hyperlinkStyle, definition)
        }

        return hyperlinkStyle
    }

    private fun generateUniqueHyperlinkStyleName(layout: Layout, baseStyleName: String): String {
        var counter = 1
        var candidateName = "${baseStyleName}_url_${counter}"
        while (getTextStyleByName(layout, candidateName) != null) {
            counter++
            candidateName = "${baseStyleName}_url_${counter}"
        }
        return candidateName
    }

    private fun createUrlVariable(layout: Layout, variableName: String, url: String): WfdXmlVariable {
        return layout.data.addVariable().setName(variableName).setKind(VariableKind.CONSTANT)
            .setDataType(DataType.STRING).setValue(url)
    }

    private fun buildAndAppendImage(layout: Layout, text: WfdXmlText, ref: ImageRef) {
        val imageModel = imageRepository.findOrFail(ref.id)

        when (val imagePlaceholder = getImagePlaceholder(imageModel)) {
            is ImagePlaceholderResult.Placeholder -> {
                text.appendText(imagePlaceholder.value)
                return
            }
            is ImagePlaceholderResult.RenderAsNormal -> {}
            is ImagePlaceholderResult.Skip -> return
        }

        text.appendImage(getOrBuildImage(layout, imageModel, imageModel.alternateText))
    }

    private fun buildAndAppendHyperlink(
        layout: Layout, paragraph: WfdXmlParagraph, baseTextStyleModel: TextStyleDTO?, hyperlinkModel: Hyperlink
    ): WfdXmlText {
        val hyperlinkText = paragraph.addText()
        val hyperlinkStyle = createHyperlinkTextStyle(layout, baseTextStyleModel, hyperlinkModel)
        hyperlinkText.setTextStyle(hyperlinkStyle)
        hyperlinkText.appendText(hyperlinkModel.displayText ?: hyperlinkModel.url)

        val newText = paragraph.addText()
        baseTextStyleModel?.also { newText.setExistingTextStyle("TextStyles.${it.nameOrId()}") }
        return newText
    }

    private fun WfdXmlText.appendVariable(
        ref: VariableRef, layout: Layout, variableStructure: VariableStructure
    ): WfdXmlText {
        val variableModel = variableRepository.findOrFail(ref.id)

        val variablePathData = variableStructure.structure[ref.id]
        if (variablePathData == null || variablePathData.path.isBlank()) {
            this.appendText("""$${variablePathData?.name ?: variableModel.nameOrId()}$""")
        } else {
            val variableName = variablePathData.name ?: variableModel.nameOrId()
            this.appendVariable(getOrCreateVariable(layout.data, variableName, variableModel, variablePathData.path))
        }

        return this
    }

    private fun findParagraphStyle(paragraphModel: Paragraph): ParagraphStyleDTO? {
        if (paragraphModel.styleRef == null) return null

        val paraStyleModel = paragraphStyleRepository.firstWithDefinition(paragraphModel.styleRef.id)
            ?: error("Paragraph style definition for ${paragraphModel.styleRef.id} not found.")

        return paraStyleModel
    }

    private fun findTextStyle(textModel: Text): TextStyleDTO? {
        if (textModel.styleRef == null) return null

        val textStyleModel = textStyleRepository.firstWithDefinition(textModel.styleRef.id)
            ?: error("Text style definition for ${textModel.styleRef.id} not found.")

        return textStyleModel
    }

    private fun buildSuccessFlowWrappedInInlineConditionFlow(
        layout: Layout, variableStructure: VariableStructure, displayRuleId: String, text: WfdXmlText
    ): Flow {
        val displayRule = displayRuleRepository.findOrFail(displayRuleId)
        if (displayRule.definition == null) {
            error("Display rule '$displayRuleId' definition is null.")
        }

        val successFlow = layout.addFlow().setType(Flow.Type.SIMPLE)
        val def = displayRule.definition!!

        text.appendFlow(
            layout.addFlow().setType(Flow.Type.SELECT_BY_INLINE_CONDITION).addLineForSelectByInlineCondition(
                def.toScript(layout, variableStructure, variableRepository::findOrFail),
                successFlow
            )
        )

        return successFlow
    }

    private fun buildTable(
        layout: Layout, variableStructure: VariableStructure, model: TableDTO, languages: List<String>
    ): WfdXmlTable {
        val table = layout.addTable().setDisplayAsImage(false)

        if (model.columnWidths.isNotEmpty()) {
            model.columnWidths.forEach { table.addColumn(it.minWidth.toMeters(), it.percentWidth) }
        } else {
            val numberOfColumns = model.rows.firstOrNull()?.cells?.size ?: 0
            repeat(numberOfColumns) { table.addColumn() }
        }

        val rowset = layout.addRowSet().setType(RowSet.Type.MULTIPLE_ROWS)
        table.setRowSet(rowset)

        model.rows.forEach { rowModel ->
            val row = if (rowModel.displayRuleRef == null) {
                layout.addRowSet().setType(RowSet.Type.SINGLE_ROW).also { rowset.addRowSet(it) }
            } else {
                val displayRule = displayRuleRepository.findOrFail(rowModel.displayRuleRef.id)
                val def = displayRule.definition
                if (def == null) {
                    error("Display rule '${rowModel.displayRuleRef.id}' definition is null.")
                }

                buildSuccessRowWrappedInConditionRow(
                    layout, variableStructure, def, rowset
                )
            }

            rowModel.cells.forEach { cellModel ->
                val cellContentFlow = buildDocumentContentAsSingleFlow(
                    layout, variableStructure, cellModel.content, null, null, languages
                )
                val cellFlow = if (cellContentFlow.type === Flow.Type.SELECT_BY_INLINE_CONDITION) {
                    layout.addFlow().setType(Flow.Type.SIMPLE).setSectionFlow(true)
                        .setWebEditingType(Flow.WebEditingType.SECTION)
                        .also { it.addParagraph().addText().appendFlow(cellContentFlow) }
                } else cellContentFlow

                row.addCell(
                    layout.addCell().setSpanLeft(cellModel.mergeLeft).setSpanUp(cellModel.mergeUp)
                        .setFlowToNextPage(true).setFlow(cellFlow)
                )
            }
        }

        when (model.pdfTaggingRule) {
            TablePdfTaggingRule.None -> table.setTablePdfTaggingRule(WfdXmlTable.TablePdfTaggingRule.NONE)
            TablePdfTaggingRule.Default -> table.setTablePdfTaggingRule(WfdXmlTable.TablePdfTaggingRule.DEFAULT)
            TablePdfTaggingRule.Table -> table.setTablePdfTaggingRule(WfdXmlTable.TablePdfTaggingRule.TABLE)
            TablePdfTaggingRule.Artifact -> table.setTablePdfTaggingRule(WfdXmlTable.TablePdfTaggingRule.ARTIFACT)
        }
        table.setTablePdfAlternateText(model.pdfAlternateText)

        return table
    }

    private fun buildFirstMatch(
        layout: Layout,
        variableStructure: VariableStructure,
        model: FirstMatch,
        isInline: Boolean,
        flowName: String? = null,
        languages: List<String>,
    ): Flow {
        val firstMatchFlow = layout.addFlow().setType(Flow.Type.SELECT_BY_INLINE_CONDITION)
        flowName?.let { firstMatchFlow.setName(it) }

        model.cases.forEachIndexed { i, case ->
            val displayRule = displayRuleRepository.findOrFail(case.displayRuleRef.id)
            if (displayRule.definition == null) {
                error("Display rule '${case.displayRuleRef.id}' definition is null.")
            }

            val caseName = case.name ?: "Case ${i + 1}"

            val contentFlow =
                buildDocumentContentAsSingleFlow(layout, variableStructure, case.content, null, null, languages)

            val caseFlow =
                if (isInline) contentFlow else layout.addFlow().setSectionFlow(true).setType(Flow.Type.SIMPLE)
                    .setWebEditingType(Flow.WebEditingType.SECTION).setDisplayName(caseName)
                    .also { it.addParagraph().addText().appendFlow(contentFlow) }

            val def = displayRule.definition!!
            firstMatchFlow.addLineForSelectByInlineCondition(
                def.toScript(layout, variableStructure, variableRepository::findOrFail),
                caseFlow
            )
        }

        if (model.default.isNotEmpty()) {
            val contentFlow =
                buildDocumentContentAsSingleFlow(layout, variableStructure, model.default, null, null, languages)

            val caseFlow =
                if (isInline) contentFlow else layout.addFlow().setSectionFlow(true).setType(Flow.Type.SIMPLE)
                    .setWebEditingType(Flow.WebEditingType.SECTION).setDisplayName("Else Case")
                    .also { it.addParagraph().addText().appendFlow(contentFlow) }

            firstMatchFlow.setDefaultFlow(caseFlow)
        }

        return firstMatchFlow
    }

    private fun buildSelectByLanguage(
        layout: Layout,
        variableStructure: VariableStructure,
        model: SelectByLanguage,
        flowName: String?,
        languages: List<String>,
    ): Flow {
        val languageFlow = layout.addFlow().setType(Flow.Type.SELECT_BY_LANGUAGE)
        flowName?.let { languageFlow.setName(it) }

        val defaultLanguage = projectConfig.defaultLanguage

        val caseFlows = model.cases.associate {
            val caseName = "Case ${it.language}"
            val contentFlow = buildDocumentContentAsSingleFlow(
                layout, variableStructure, it.content, null, null, languages
            ).setDisplayName(caseName)
            it.language to contentFlow
        }

        var defaultLanguageFlow: Flow? = null
        for (language in languages) {
            val contentFlow: Flow = (caseFlows[language] as Flow?) ?: layout.addFlow().setType(Flow.Type.SIMPLE).setDisplayName("Case $language")
            languageFlow.addLineForSelectByInlineCondition(language, contentFlow)

            if (language == defaultLanguage) {
                defaultLanguageFlow = contentFlow
            }
        }

        languageFlow.setDefaultFlow(defaultLanguageFlow ?: layout.addFlow().setType(Flow.Type.SIMPLE))

        return languageFlow
    }


    protected fun List<DocumentContent>.paragraphIfEmpty(): List<DocumentContent> {
        return this.ifEmpty {
            listOf(
                com.quadient.migration.api.dto.migrationmodel.Paragraph(
                    listOf(com.quadient.migration.api.dto.migrationmodel.Paragraph.Text(listOf(), null, null)),
                    null,
                    null
                )
            )
        }
    }

    private fun TextStyleDTO.resolve(): TextStyleDefinition {
        val def = this.definition
        return when (def) {
            is TextStyleDefinition -> def
            is TextStyleRef -> {
                textStyleRepository.find(def.id)?.resolve() ?: error("Invalid text style reference")
            }
            else -> error("Invalid text style definition type")
        }
    }

    private fun ParagraphStyleDTO.resolve(): ParagraphStyleDefinition {
        val def = this.definition
        return when (def) {
            is ParagraphStyleDefinition -> def
            is ParagraphStyleRef -> paragraphStyleRepository.findOrFail(def.id).resolve()
            else -> error("Invalid paragraph style definition type")
        }
    }

    sealed interface ScriptResult {
        data class Success(val variableScript: String) : ScriptResult {
            override fun toString() = variableScript
        }

        data class Failure(val variableName: String) : ScriptResult {
            override fun toString() = variableName
        }
    }
}

fun DisplayRuleDefinition.toScript(
    layout: Layout, variableStructure: VariableStructure, findVar: (String) -> Variable
): String {
    return "return ${this.group.toScript(layout, variableStructure, findVar)};"
}

fun Group.toScript(
    layout: Layout, variableStructure: VariableStructure, findVar: (String) -> Variable
): String {
    val expressions = """(${
        items.joinToString(
            separator = " ${operator.toInlineCondition()} ", transform = {
                when (it) {
                    is Binary -> it.toScript(layout, variableStructure, findVar)
                    is Group -> it.toScript(layout, variableStructure, findVar)
                }
            })
    })"""
    return if (negation) {
        "not $expressions"
    } else {
        expressions
    }
}

fun Binary.toScript(
    layout: Layout, variableStructure: VariableStructure, findVar: (String) -> Variable
): String {
    val leftScriptResult = left.toScript(layout, variableStructure, findVar)
    val rightScriptResult = right.toScript(layout, variableStructure, findVar)

    val binary = operator.toScript(leftScriptResult, rightScriptResult)
    return if (leftScriptResult is Success && rightScriptResult is Success) {
        binary
    } else {
        BinOp.Equals.toScript(
            Success("String('${binary.replace("'", "")}')"), Success("String('unmapped')")
        )
    }
}

fun BinOp.toScript(left: ScriptResult, right: ScriptResult): String {
    return when (this) {
        BinOp.Equals -> "$left==$right"
        BinOp.EqualsCaseInsensitive -> "$left.equalCaseInsensitive($right)"
        BinOp.NotEquals -> "$left!=$right"
        BinOp.NotEqualsCaseInsensitive -> "(not $left.equalCaseInsensitive($right))"
        BinOp.GreaterThan -> "$left>$right"
        BinOp.GreaterOrEqualThan -> "$left>=$right"
        BinOp.LessThan -> "$left<$right"
        BinOp.LessOrEqualThen -> "$left<=$right"
    }
}

fun LiteralOrFunctionCall.toScript(
    layout: Layout, variableStructure: VariableStructure, findVar: (String) -> Variable
): ScriptResult {
    return when (this) {
        is Literal -> this.toScript(layout, variableStructure, findVar)
        is Function -> this.toScript(layout, variableStructure, findVar)
    }
}

fun Function.toScript(
    layout: Layout, variableStructure: VariableStructure, findVar: (String) -> Variable
): ScriptResult {
    when (this) {
        is Function.UpperCase -> {
            val arg = args[0]
            return Success("(${arg.toScript(layout, variableStructure, findVar)}).toUpperCase()")
        }

        is Function.LowerCase -> {
            val arg = args[0]
            return Success("(${arg.toScript(layout, variableStructure, findVar)}).toLowerCase()")
        }
    }
}

fun Literal.toScript(
    layout: Layout, variableStructure: VariableStructure, findVar: (String) -> Variable
): ScriptResult {
    return when (dataType) {
        LiteralDataType.Variable -> variableToScript(value, layout, variableStructure, findVar)
        LiteralDataType.String -> Success(
            "String('${
                value.replace("\\", "\\\\").replace("\"", "\\\"")
            }')"
        )

        LiteralDataType.Number -> Success(value)
        LiteralDataType.Boolean -> Success(value.lowercase().toBooleanStrict().toString())
    }
}

fun variableToScript(
    id: String, layout: Layout, variableStructure: VariableStructure, findVar: (String) -> Variable
): ScriptResult {
    val variableModel = findVar(id)
    val variablePathData = variableStructure.structure[id]
    return if (variablePathData == null || variablePathData.path.isBlank()) {
        Failure(variablePathData?.name ?: variableModel.nameOrId())
    } else {
        val variableName = variablePathData.name ?: variableModel.nameOrId()

        getOrCreateVariable(layout.data, variableName, variableModel, variablePathData.path)

        Success((variablePathData.path.split(".") + variableName).joinToString(".") { pathPart ->
            when (pathPart.lowercase()) {
                "value" -> "Current"
                "data" -> "DATA"
                else -> sanitizeVariablePart(if (pathPart.first().isDigit()) "_$pathPart" else pathPart)
            }
        })
    }
}

fun getOrCreateVariable(
    data: Data, variableName: String, variableModel: Variable, variablePath: String
): WfdXmlVariable {
    val variable = getVariable(data as DataImpl, variableName, variablePath)
    return variable ?: data.addVariable().setName(variableName).setKind(VariableKind.DISCONNECTED)
        .setDataType(getDataType(variableModel.dataType)).setExistingParentId(variablePath)
        .setValueIfAvailable(variableModel)
}

fun WfdXmlVariable.setValueIfAvailable(variableModel: Variable): WfdXmlVariable {
    val defaultVal = variableModel.defaultValue
    if (!defaultVal.isNullOrBlank()) {
        when (variableModel.dataType) {
            DataTypeModel.String, DataTypeModel.DateTime -> this.setValue(defaultVal)
            DataTypeModel.Integer -> this.setValue(defaultVal.toInt())
            DataTypeModel.Integer64 -> this.setValue(defaultVal.toLong())
            DataTypeModel.Double, DataTypeModel.Currency -> this.setValue(defaultVal.toDouble())
            DataTypeModel.Boolean -> this.setValue(
                defaultVal.lowercase().toBooleanStrict()
            )
        }
    }

    return this
}