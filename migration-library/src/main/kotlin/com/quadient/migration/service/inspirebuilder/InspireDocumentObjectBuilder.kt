package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.ColumnLayout
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
import com.quadient.migration.api.dto.migrationmodel.RepeatedContent
import com.quadient.migration.api.dto.migrationmodel.ResourceRef
import com.quadient.migration.api.dto.migrationmodel.SelectByLanguage
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.TableRow
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.FlowModel.*
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.ScriptResult
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.ScriptResult.*
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.service.resolveAlias
import com.quadient.migration.service.resolveAliases
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Binary
import com.quadient.migration.shared.ColumnApplyTo
import com.quadient.migration.shared.ColumnBalancingType
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.AttachmentType
import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.CellAlignment
import com.quadient.migration.shared.CellHeight
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
import com.quadient.migration.shared.TableAlignment
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Font
import com.quadient.wfdxml.api.layoutnodes.Image as WfdXmlImage
import com.quadient.wfdxml.api.layoutnodes.LocationType
import com.quadient.wfdxml.api.layoutnodes.Pages
import com.quadient.wfdxml.api.layoutnodes.ParagraphStyle as WfdXmlParagraphStyle
import com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.LineSpacingType.*
import com.quadient.wfdxml.api.layoutnodes.Section as WfdXmlSection
import com.quadient.wfdxml.api.layoutnodes.SheetNameType
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
import com.quadient.wfdxml.api.layoutnodes.TextStyle as WfdXmlTextStyle
import com.quadient.wfdxml.api.layoutnodes.TextStyleInheritFlag
import com.quadient.wfdxml.api.layoutnodes.TextStyleType
import com.quadient.wfdxml.api.layoutnodes.tables.BorderStyle
import com.quadient.wfdxml.api.layoutnodes.tables.Cell
import com.quadient.wfdxml.api.layoutnodes.flow.Paragraph as WfdXmlParagraph
import com.quadient.wfdxml.api.module.Layout
import com.quadient.wfdxml.internal.data.WorkFlowTreeDefinition
import com.quadient.wfdxml.internal.layoutnodes.TextStyleImpl
import com.quadient.wfdxml.internal.layoutnodes.data.DataImpl
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE
import kotlin.time.Clock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ifEmpty
import com.quadient.migration.shared.DataType as DataTypeModel
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.Shape
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.shared.VariablePath
import com.quadient.migration.shared.LiteralPath
import com.quadient.migration.shared.VariableRefPath
import com.quadient.migration.service.resolveTarget
import com.quadient.migration.shared.Size
import com.quadient.wfdxml.api.layoutnodes.Flow.WebEditingType.SECTION

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
    protected val output: InspireOutput,
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

    abstract fun getDisplayRulePath(rule: DisplayRule): IcmPath

    abstract fun getAttachmentPath(attachment: Attachment): String

    abstract fun getStyleDefinitionPath(): String

    abstract fun getFontRootFolder(): String

    abstract fun buildDocumentObject(documentObject: DocumentObject): String

    abstract fun shouldIncludeInternalDependency(documentObject: DocumentObject): Boolean

    protected fun collectLanguages(documentObject: DocumentObject): List<String> {
        val languages = mutableSetOf<String>()

        fun Table.allRows(): List<Table.Row> =
            (rows + header + firstHeader + footer + lastFooter).flatMap { row ->
                when (row) {
                    is Table.Row -> listOf(row)
                    is Table.RepeatedRow -> row.rows
                }
            }

        fun collectLanguagesFromContent(content: List<DocumentContent>) {
            for (item in content) {
                when (item) {
                    is SelectByLanguage -> item.cases.forEach { languages.add(it.language) }
                    is Area -> collectLanguagesFromContent(item.content)
                    is RepeatedContent -> collectLanguagesFromContent(item.content)
                    is ColumnLayout -> {}
                    is FirstMatch -> {
                        item.cases.forEach { case -> collectLanguagesFromContent(case.content) }
                        collectLanguagesFromContent(item.default)
                    }

                    is Table -> item.allRows()
                        .forEach { row -> row.cells.forEach { cell -> collectLanguagesFromContent(cell.content) } }

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

                                is Table -> textContent.allRows()
                                    .forEach { row -> row.cells.forEach { cell -> collectLanguagesFromContent(cell.content) } }

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
        layout: Layout, variableStructure: VariableStructure, rule: DisplayRule, successFlow: Flow
    ): Flow {
        return layout.addFlow().setType(Flow.Type.SELECT_BY_CONDITION).addLineForSelectByCondition(
            layout.data.addVariable().setKind(VariableKind.CALCULATED).setDataType(DataType.BOOL)
                .setScript(rule.toScript(
                    layout,
                    variableStructure,
                    variableRepository::findOrFail,
                    displayRuleRepository::findOrFail,
                    ::getDisplayRulePath,
                    output,
                    projectConfig.interactiveTenant
                )),
            successFlow
        )
    }

    protected open fun buildConditionRow(
        layout: Layout,
        variableStructure: VariableStructure,
        rule: DisplayRule,
        innerRowSet: GeneralRowSet? = null,
    ): GeneralRowSet {
        val successRow = innerRowSet ?: layout.addRowSet().setType(RowSet.Type.SINGLE_ROW)
        return layout.addRowSet().setType(RowSet.Type.SELECT_BY_CONDITION)
            .addLineForSelectByCondition(
                layout.data.addVariable().setKind(VariableKind.CALCULATED).setDataType(DataType.BOOL)
                    .setScript(rule.toScript(
                        layout,
                        variableStructure,
                        variableRepository::findOrFail,
                        displayRuleRepository::findOrFail,
                        ::getDisplayRulePath,
                        output,
                        projectConfig.interactiveTenant
                    )),
                successRow
            )
    }

    protected fun wrapRowSetInConditionIfNeeded(
        layout: Layout,
        variableStructure: VariableStructure,
        displayRuleRef: DisplayRuleRef?,
        rowSet: GeneralRowSet,
    ): GeneralRowSet {
        if (displayRuleRef == null) return rowSet
        val displayRule = displayRuleRepository.findOrFail(displayRuleRef.id)
        return buildConditionRow(layout, variableStructure, displayRule, rowSet)
    }

    fun buildStyleLayoutDelta(textStyles: List<TextStyle>, paragraphStyles: List<ParagraphStyle>): String {
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
        textStyles: List<TextStyle>,
        paragraphStyles: List<ParagraphStyle>,
    ): String {
        logger.debug("Starting to build style definition.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()

        layout.setName("DocumentLayout")
        val flow = layout.addFlow().setName("StyleDefinitionMainFlow").setSectionFlow(true).setWebEditingType(SECTION)
        layout.addPage().setName("Page 1").setType(Pages.PageConditionType.SIMPLE).addFlowArea()
            .setName("StyleDefinitionFlowArea").setPosX(Size.ofMillimeters(3.62).toMeters())
            .setPosY(Size.ofMillimeters(3.62).toMeters()).setWidth(Size.ofMillimeters(203.55).toMeters())
            .setHeight(Size.ofMillimeters(287.23).toMeters()).setFlow(flow).setFlowToNextPage(true)

        layout.pages.setMainFlow(flow)
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
        val mutableContent = content.resolveAliases(imageRepository, attachmentRepository).toMutableList()

        var idx = 0
        val flowModels = mutableListOf<FlowModel>()
        while (idx < mutableContent.size) {
            when (val contentPart = mutableContent[idx]) {
                is Table, is Paragraph, is ImageRef, is VariableStringContent, is ColumnLayout -> {
                    val flowParts = gatherFlowParts(mutableContent, idx)
                    idx += flowParts.size - 1
                    flowModels.add(Composite(flowParts))
                }

                is DocumentObjectRef -> flowModels.add(DocumentObjectRefFlow(contentPart))
                is AttachmentRef -> flowModels.add(AttachmentFlow(contentPart))
                is Area -> mutableContent.addAll(
                    idx + 1, contentPart.content.resolveAliases(imageRepository, attachmentRepository)
                )

                is Shape -> {}

                is RepeatedContent -> flowModels.add(RepeatedContentFlow(contentPart))
                is FirstMatch -> flowModels.add(FirstMatchFlow(contentPart))
                is SelectByLanguage -> flowModels.add(SelectByLanguageFlow(contentPart))
            }
            idx++
        }

        val flowCount = flowModels.size
        var flowSuffix = 1

        fun nextName(): String? {
            if (flowName == null) return null
            return if (flowCount == 1) flowName else "$flowName ${flowSuffix++}"
        }

        return flowModels.mapNotNull {
            when (it) {
                is DocumentObjectRefFlow -> buildDocumentObjectRefOrPlaceholder(
                    layout, variableStructure, it.ref, languages
                )

                is AttachmentFlow -> buildAttachmentRef(layout, it.ref)
                is Composite -> buildCompositeFlow(layout, variableStructure, it.parts, nextName(), languages)
                is FirstMatchFlow -> buildFirstMatch(layout, variableStructure, it.model, false, nextName(), languages)

                is SelectByLanguageFlow -> buildSelectByLanguage(
                    layout, variableStructure, it.model, nextName(), languages
                )

                is RepeatedContentFlow -> buildRepeatedContent(
                    it.model, layout, variableStructure, nextName(), languages
                )
            }
        }
    }

    sealed interface FlowModel {
        data class Composite(val parts: List<DocumentContent>) : FlowModel
        data class DocumentObjectRefFlow(val ref: DocumentObjectRef) : FlowModel
        data class AttachmentFlow(val ref: AttachmentRef) : FlowModel
        data class FirstMatchFlow(val model: FirstMatch) : FlowModel
        data class SelectByLanguageFlow(val model: SelectByLanguage) : FlowModel
        data class RepeatedContentFlow(val model: RepeatedContent) : FlowModel
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

            wrapSuccessFlowInConditionFlow(layout, variableStructure, displayRule, singleFlow)
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

    protected fun initVariableStructure(layout: Layout, variableStructureId: String?): VariableStructure {
        val variableStructureId = variableStructureId ?: projectConfig.defaultVariableStructure

        val variableStructureModel =
            variableStructureId?.let { variableStructureRepository.findOrFail(it) } ?: VariableStructure(
                id = "defaultVariableStructure",
                lastUpdated = Clock.System.now(),
                created = Clock.System.now(),
                structure = mutableMapOf(),
                customFields = CustomFieldMap(),
                languageVariable = null,
            )

        val normalizedVariablePaths = variableStructureModel.structure.map { (variableId, variablePathData) ->
            val literalPath = variablePathData.path.resolve(variableStructureModel, variableRepository::findOrFail)
                ?: error("Variable '$variableId' referenced as array path has no resolvable literal path in structure")
            removeDataFromVariablePath(literalPath)
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

    fun buildTextStyles(layout: Layout, textStyleModels: List<TextStyle>) {
        val arialFont = getFontByName(layout, "Arial")
        require(arialFont != null) { "Layout must contain Arial font." }
        arialFont.setName("Arial").setFontName("Arial")
        upsertSubFont(arialFont, isBold = false, isItalic = false)

        textStyleModels.forEach { styleModel ->
            val definition = styleModel.resolve().definition
            val textStyle = layout.addTextStyle().setName(styleModel.nameOrId())
            applyTextStyleProperties(layout, textStyle, definition)
        }
    }

    private fun applyTextStyleProperties(layout: Layout, textStyle: WfdXmlTextStyle, definition: TextStyleDefinition) {
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

    fun buildParagraphStyles(layout: Layout, paragraphStyleModels: List<ParagraphStyle>) {
        paragraphStyleModels.forEach { styleModel ->
            val definition = styleModel.resolve().definition

            val paragraphStyle = layout.addParagraphStyle().setName(styleModel.nameOrId())

            definition.leftIndent?.let { paragraphStyle.setLeftIndent(it.toMeters()) }
            definition.rightIndent?.let { paragraphStyle.setRightIndent(it.toMeters()) }
            definition.defaultTabSize?.let { paragraphStyle.setDefaultTabSize(it.toMeters()) }
            definition.spaceBefore?.let { paragraphStyle.setSpaceBefore(it.toMeters()) }
            definition.spaceAfter?.let { paragraphStyle.setSpaceAfter(it.toMeters()) }
            val alignType = when (definition.alignment) {
                Alignment.Left -> WfdXmlParagraphStyle.AlignType.LEFT
                Alignment.Right -> WfdXmlParagraphStyle.AlignType.RIGHT
                Alignment.Center -> WfdXmlParagraphStyle.AlignType.CENTER
                Alignment.JustifyLeft -> WfdXmlParagraphStyle.AlignType.JUSTIFY_lEFT
                Alignment.JustifyRight -> WfdXmlParagraphStyle.AlignType.JUSTIFY_RIGHT
                Alignment.JustifyCenter -> WfdXmlParagraphStyle.AlignType.JUSTIFY_CENTER
                Alignment.JustifyBlock -> WfdXmlParagraphStyle.AlignType.JUSTIFY_BLOCK
                Alignment.JustifyWithMargins -> WfdXmlParagraphStyle.AlignType.JUSTIFY_WITH_MARGIN
                Alignment.JustifyBlockUniform -> WfdXmlParagraphStyle.AlignType.JUSTIFY_BLOCK_UNIFORM
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

        var columnLayout: ColumnLayout? = null

        var i = 0
        while (i < documentContentModelParts.size) {
            when (val model = documentContentModelParts[i]) {
                is ColumnLayout -> columnLayout = model
                is VariableStringContent -> {
                    val paragraph = ParagraphBuilder()
                    var y = i
                    while (y < documentContentModelParts.size) {
                        val part = documentContentModelParts[y]
                        if (part !is VariableStringContent) {
                            break
                        } else {
                            y++
                            when (part) {
                                is StringValue -> paragraph.string(part.value)
                                is VariableRef -> paragraph.variableRef(part.id)
                            }
                        }
                    }

                    i += (y - i)
                    buildParagraph(layout, variableStructure, flow, paragraph.build(), languages, columnLayout)
                    columnLayout = null
                }
                is Paragraph -> {
                    buildParagraph(layout, variableStructure, flow, model, languages, columnLayout)
                    columnLayout = null
                }
                is Table -> {
                    val text = flow.addParagraph().addText()
                    columnLayout?.let { text.appendSection(buildWfdXmlSection(it, layout)) }
                    columnLayout = null
                    text.appendTable(buildTable(layout, variableStructure, model, languages))
                }
                is ImageRef -> {
                    val text = flow.addParagraph().addText()
                    columnLayout?.let { text.appendSection(buildWfdXmlSection(it, layout)) }
                    columnLayout = null
                    buildAndAppendImage(layout, text, model)
                }
                else -> error("Content part type ${model::class.simpleName} is not allowed in composite flow.")
            }

            i++
        }

        return flow
    }

    abstract fun buildDocumentObjectRef(
        documentModel: DocumentObject,
        layout: Layout,
        variableStructure: VariableStructure,
        documentObjectRef: DocumentObjectRef,
        languages: List<String>,
    ): Flow?

    private fun buildDocumentObjectRefOrPlaceholder(
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

        return buildDocumentObjectRef(documentModel, layout, variableStructure, documentObjectRef, languages)
    }

    private fun gatherFlowParts(content: List<DocumentContent>, startIndex: Int): List<DocumentContent> {
        val flowParts = mutableListOf<DocumentContent>()

        var index = startIndex

        do {
            val contentPart = content[index]
            if (contentPart is Table || contentPart is Paragraph || contentPart is ImageRef || contentPart is VariableStringContent || contentPart is ColumnLayout) {
                flowParts.add(contentPart)
                index++
            } else {
                break
            }
        } while (index < content.size)

        return flowParts
    }

    protected open fun resolveParagraphStyleName(name: String): String = name
    protected open fun resolveTextStyleName(name: String): String = name
    protected open fun resolveTableStyleName(name: String): String = name

    private fun buildParagraph(
        layout: Layout,
        variableStructure: VariableStructure,
        flow: Flow,
        paragraphModel: Paragraph,
        languages: List<String>,
        parentColumnLayout: ColumnLayout? = null
    ) {
        val paragraph = if (paragraphModel.displayRuleRef == null) {
            flow.addParagraph()
        } else {
            buildSuccessFlowWrappedInInlineConditionFlow(
                layout, variableStructure, paragraphModel.displayRuleRef.id, flow.addParagraph().addText()
            ).addParagraph()
        }

        val paragraphStyle = paragraphModel.styleRef?.let { paragraphStyleRepository.findOrFail(it.id).resolve() }
        paragraphStyle?.also { paragraph.setExistingParagraphStyle("ParagraphStyles.${resolveParagraphStyleName(it.nameOrId())}") }

        val columnLayout = paragraphModel.content.firstNotNullOfOrNull { textModel ->
            textModel.content.filterIsInstance<ColumnLayout>().firstOrNull()
        } ?: parentColumnLayout

        paragraphModel.content.forEachIndexed { index, textModel ->
            val baseText = if (textModel.displayRuleRef == null) {
                paragraph.addText()
            } else {
                buildSuccessFlowWrappedInInlineConditionFlow(
                    layout, variableStructure, textModel.displayRuleRef.id, paragraph.addText()
                ).addParagraph().also {
                    if (paragraphStyle != null) it.setExistingParagraphStyle("ParagraphStyles.${resolveParagraphStyleName(paragraphStyle.nameOrId())}")
                }.addText()
            }

            val baseTextStyleModel = textModel.styleRef?.let { textStyleRepository.findOrFail(it.id).resolve() }
            baseTextStyleModel?.also { baseText.setExistingTextStyle("TextStyles.${resolveTextStyleName(it.nameOrId())}") }

            var currentText = baseText

            if (index == 0 && columnLayout != null) {
                currentText.appendSection(buildWfdXmlSection(columnLayout, layout))
            }

            textModel.content.forEach { textContent ->
                when (textContent) {
                    is ColumnLayout -> {}
                    is ResourceRef -> {
                        when (val resolved = resolveAlias(textContent, imageRepository, attachmentRepository)) {
                            is ImageRef -> buildAndAppendImage(layout, currentText, resolved)
                            is AttachmentRef -> buildAttachmentRef(layout, resolved)?.also { flow ->
                                currentText.appendFlow(flow)
                            }
                        }
                    }
                    
                    is StringValue -> currentText.appendText(textContent.value)
                    is VariableRef -> currentText.appendVariable(textContent, layout, variableStructure)
                    is Table -> currentText.appendTable(buildTable(layout, variableStructure, textContent, languages))
                    is DocumentObjectRef -> buildDocumentObjectRefOrPlaceholder(
                        layout, variableStructure, textContent, languages
                    )?.also { flow ->
                        currentText.appendFlow(flow)
                    }

                    is Hyperlink -> currentText = buildAndAppendHyperlink(layout, paragraph, baseTextStyleModel, textContent)
                    is FirstMatch -> currentText.appendFlow(
                        buildFirstMatch(layout, variableStructure, textContent, true, null, languages)
                    )
                }
            }
        }
    }

    private fun createHyperlinkTextStyle(
        layout: Layout, baseTextStyleModel: TextStyle?, hyperlinkModel: Hyperlink
    ): WfdXmlTextStyle {
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
            val definition = baseTextStyleModel.resolve().definition
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
        layout: Layout, paragraph: WfdXmlParagraph, baseTextStyleModel: TextStyle?, hyperlinkModel: Hyperlink
    ): WfdXmlText {
        val hyperlinkText = paragraph.addText()
        val hyperlinkStyle = createHyperlinkTextStyle(layout, baseTextStyleModel, hyperlinkModel)
        hyperlinkText.setTextStyle(hyperlinkStyle)
        hyperlinkText.appendText(hyperlinkModel.displayText ?: hyperlinkModel.url)

        val newText = paragraph.addText()
        baseTextStyleModel?.also { newText.setExistingTextStyle("TextStyles.${resolveTextStyleName(it.nameOrId())}") }
        return newText
    }

    private fun WfdXmlText.appendVariable(
        ref: VariableRef, layout: Layout, variableStructure: VariableStructure
    ): WfdXmlText {
        val variableModel = variableRepository.findOrFail(ref.id)

        val variablePathData = variableStructure.structure[ref.id]
        val resolvedPath = variablePathData?.path?.resolve(variableStructure, variableRepository::findOrFail)?.takeIf { it.isNotBlank() }
        if (resolvedPath.isNullOrBlank()) {
            this.appendText("""$${variablePathData?.name ?: variableModel.nameOrId()}$""")
        } else {
            val variableName = variablePathData.name ?: variableModel.nameOrId()
            this.appendVariable(getOrCreateVariable(layout.data, variableName, variableModel, resolvedPath))
        }

        return this
    }

    private fun buildSuccessFlowWrappedInInlineConditionFlow(
        layout: Layout, variableStructure: VariableStructure, displayRuleId: String, text: WfdXmlText
    ): Flow {
        val displayRule = displayRuleRepository.findOrFail(displayRuleId)

        val successFlow = layout.addFlow().setType(Flow.Type.SIMPLE)
        text.appendFlow(
            layout.addFlow().setType(Flow.Type.SELECT_BY_INLINE_CONDITION).addLineForSelectByInlineCondition(
                displayRule.toScript(
                    layout,
                    variableStructure,
                    variableRepository::findOrFail,
                    displayRuleRepository::findOrFail,
                    ::getDisplayRulePath,
                    output,
                    projectConfig.interactiveTenant
                ),
                successFlow
            )
        )

        return successFlow
    }

    private fun buildTableBorderStyle(border: BorderOptions?, layout: Layout, setStyle: (BorderStyle) -> Unit) {
        if (border == null) {
            return
        }

        val borderStyle = layout.addBorderStyle()
        setStyle(borderStyle)

        borderStyle.setMargins(
            border.paddingTop.toMeters(),
            border.paddingRight.toMeters(),
            border.paddingBottom.toMeters(),
            border.paddingLeft.toMeters()
        )

        if (border.fill != null) {
            borderStyle.setFill(
                layout.addFillStyle().setColor(
                    layout.addColor().setRGB(
                        border.fill.red(), border.fill.green(), border.fill.blue()
                    )
                )
            )
        }

        val toSelect = listOfNotNull(
            border.leftLine?.let { BorderStyle.LinesAndCorners.LEFT_LINE },
            border.rightLine?.let { BorderStyle.LinesAndCorners.RIGHT_LINE },
            border.topLine?.let { BorderStyle.LinesAndCorners.TOP_LINE },
            border.bottomLine?.let { BorderStyle.LinesAndCorners.BOTTOM_LINE },
        ).toTypedArray()

        if (toSelect.isEmpty()) {
            return
        }

        val borderLines = borderStyle.select(*toSelect)

        if (border.leftLine != null) {
            val fillStyle = layout.addFillStyle().setColor(
                layout.addColor().setRGB(
                    border.leftLine.color.red(),
                    border.leftLine.color.green(),
                    border.leftLine.color.blue()
                )
            )
            borderLines.setLineWidth(BorderStyle.LinesAndCorners.LEFT_LINE, border.leftLine.width.toMeters())
            borderLines.setLineFillStyle(BorderStyle.LinesAndCorners.LEFT_LINE, fillStyle)
        }

        if (border.rightLine != null) {
            val fillStyle = layout.addFillStyle().setColor(
                layout.addColor().setRGB(
                    border.rightLine.color.red(),
                    border.rightLine.color.green(),
                    border.rightLine.color.blue()
                )
            )
            borderLines.setLineWidth(BorderStyle.LinesAndCorners.RIGHT_LINE, border.rightLine.width.toMeters())
            borderLines.setLineFillStyle(BorderStyle.LinesAndCorners.RIGHT_LINE, fillStyle)
        }

        if (border.topLine != null) {
            val fillStyle = layout.addFillStyle().setColor(
                layout.addColor().setRGB(
                    border.topLine.color.red(),
                    border.topLine.color.green(),
                    border.topLine.color.blue()
                )
            )
            borderLines.setLineWidth(BorderStyle.LinesAndCorners.TOP_LINE, border.topLine.width.toMeters())
            borderLines.setLineFillStyle(BorderStyle.LinesAndCorners.TOP_LINE, fillStyle)
        }

        if (border.bottomLine != null) {
            val fillStyle = layout.addFillStyle().setColor(
                layout.addColor().setRGB(
                    border.bottomLine.color.red(),
                    border.bottomLine.color.green(),
                    border.bottomLine.color.blue()
                )
            )
            borderLines.setLineWidth(BorderStyle.LinesAndCorners.BOTTOM_LINE, border.bottomLine.width.toMeters())
            borderLines.setLineFillStyle(BorderStyle.LinesAndCorners.BOTTOM_LINE, fillStyle)
        }
    }

    private fun List<TableRow>.buildRowSetGroup(
        layout: Layout, variableStructure: VariableStructure, languages: List<String>
    ): GeneralRowSet? {
        fun TableRow.toRowSet(): GeneralRowSet? = when (this) {
            is Table.Row -> buildSingleRowSet(this, layout, variableStructure, languages)
            is Table.RepeatedRow -> buildRepeatedRowSet(this, layout, variableStructure, languages)
        }

        if (isEmpty()) return null
        if (size == 1) return first().toRowSet()
        return layout.addRowSet().setType(RowSet.Type.MULTIPLE_ROWS).also { multipleRowSet ->
            forEach { tableRow -> tableRow.toRowSet()?.let { multipleRowSet.addRowSet(it) } }
        }
    }

    private fun buildSingleRowSet(
        rowModel: Table.Row, layout: Layout, variableStructure: VariableStructure, languages: List<String>
    ): GeneralRowSet {
        val rowSet = layout.addRowSet().setType(RowSet.Type.SINGLE_ROW)

        rowModel.cells.forEach { cellModel ->
            val cellContentFlow = buildDocumentContentAsSingleFlow(
                layout, variableStructure, cellModel.content, null, null, languages
            )
            val cellFlow =
                if (cellContentFlow.type === Flow.Type.SELECT_BY_INLINE_CONDITION || cellContentFlow.type === Flow.Type.SELECT_BY_CONDITION) {
                layout.addFlow().setType(Flow.Type.SIMPLE).setSectionFlow(true)
                    .setWebEditingType(SECTION)
                    .also { it.addParagraph().addText().appendFlow(cellContentFlow) }
            } else cellContentFlow

            val cell = layout.addCell().setSpanLeft(cellModel.mergeLeft).setSpanUp(cellModel.mergeUp)
                .setFlowToNextPage(true).setFlow(cellFlow)

            when (cellModel.height) {
                is CellHeight.Custom -> {
                    cell.setType(Cell.CellType.CUSTOM)
                        .setMinHeight(cellModel.height.minHeight.toMeters())
                        .setMaxHeight(cellModel.height.maxHeight.toMeters())
                }
                is CellHeight.Fixed -> {
                    cell.setType(Cell.CellType.FIXED_HEIGHT)
                        .setFixedHeight(cellModel.height.size.toMeters())
                }
                null -> {}
            }

            when (cellModel.alignment) {
                CellAlignment.Top -> cell.setAlignment(Cell.CellVerticalAlignment.TOP)
                CellAlignment.Center -> cell.setAlignment(Cell.CellVerticalAlignment.CENTER)
                CellAlignment.Bottom -> cell.setAlignment(Cell.CellVerticalAlignment.BOTTOM)
                null -> {}
            }

            buildTableBorderStyle(cellModel.border, layout, cell::setBorderStyle)

            rowSet.addCell(cell)
        }

        return wrapRowSetInConditionIfNeeded(layout, variableStructure, rowModel.displayRuleRef, rowSet)
    }

    private fun buildRepeatedRowSet(
        repeatedRow: Table.RepeatedRow,
        layout: Layout,
        variableStructure: VariableStructure,
        languages: List<String>
    ): GeneralRowSet? {
        val (varName, varPath) = resolveVariableNameAndPath(repeatedRow.variable, variableStructure)
            ?: return buildUnmappedRepeatedRowFallback(repeatedRow, layout, variableStructure, languages)
        val arrayVariable = getVariable(layout.data as DataImpl, varName, varPath)
            ?: return buildUnmappedRepeatedRowFallback(repeatedRow, layout, variableStructure, languages)
        require(arrayVariable.nodeOptionality == NodeOptionality.ARRAY) {
            "Variable '$varName' at '$varPath' used in repeated row is not an Array variable"
        }

        val repeatedRowSet = layout.addRowSet().setType(RowSet.Type.REPEATED)
        if (repeatedRow.rows.size > 1) {
            val multipleRowSet = layout.addRowSet().setType(RowSet.Type.MULTIPLE_ROWS)
            repeatedRowSet.addRowSet(multipleRowSet)
            repeatedRow.rows.forEach { multipleRowSet.addRowSet(buildSingleRowSet(it, layout, variableStructure, languages)) }
        } else {
            repeatedRow.rows.forEach { repeatedRowSet.addRowSet(buildSingleRowSet(it, layout, variableStructure, languages)) }
        }
        repeatedRowSet.setVariable(arrayVariable)

        val layoutRoot = layout.root ?: layout.addRoot()
        layoutRoot.addLockedWebNode(repeatedRowSet)

        return wrapRowSetInConditionIfNeeded(layout, variableStructure, repeatedRow.displayRuleRef, repeatedRowSet)
    }

    private fun buildUnmappedRepeatedRowFallback(
        repeatedRow: Table.RepeatedRow, layout: Layout, variableStructure: VariableStructure, languages: List<String>
    ): GeneralRowSet? {
        val varName = getVariableNameFromPath(repeatedRow.variable, variableStructure)
        val warning = Paragraph("<repeated by unmapped \$$varName\$>")

        val rows = repeatedRow.rows
        val rowsWithWarning = rows.firstOrNull()?.let { firstRow ->
            firstRow.cells.firstOrNull()?.let { firstCell ->
                val cellWithWarning = firstCell.copy(content = listOf(warning) + firstCell.content)
                listOf(firstRow.copy(cells = listOf(cellWithWarning) + firstRow.cells.drop(1))) + rows.drop(1)
            }
        } ?: rows

        return rowsWithWarning.buildRowSetGroup(layout, variableStructure, languages)?.let {
            wrapRowSetInConditionIfNeeded(layout, variableStructure, repeatedRow.displayRuleRef, it)
        }
    }

    private fun getVariableNameFromPath(
        variablePath: VariablePath, variableStructure: VariableStructure
    ): String = when (variablePath) {
        is LiteralPath -> variablePath.path
        is VariableRefPath -> variableStructure.structure[variablePath.variableId]?.name
            ?: variableRepository.find(variablePath.variableId)?.nameOrId()
            ?: variablePath.variableId
    }

    private fun buildRepeatedContent(
        model: RepeatedContent,
        layout: Layout,
        variableStructure: VariableStructure,
        flowName: String?,
        languages: List<String>,
    ): Flow {
        val (varName, varPath) = resolveVariableNameAndPath(model.variablePath, variableStructure)
            ?: return buildUnmappedRepeatedContentFallback(model, layout, variableStructure, languages)
        val arrayVariable = getVariable(layout.data as DataImpl, varName, varPath)
            ?: return buildUnmappedRepeatedContentFallback(model, layout, variableStructure, languages)
        require(arrayVariable.nodeOptionality == NodeOptionality.ARRAY) {
            "Variable '$varName' at '$varPath' used in repeated content is not an Array variable"
        }

        val innerFlows = buildDocumentContentAsFlows(layout, variableStructure, model.content, languages = languages)
        return if (innerFlows.size == 1 && innerFlows[0].type == Flow.Type.SIMPLE) {
            val repeatedFlow = innerFlows[0].setType(Flow.Type.REPEATED).setVariable(arrayVariable)
            flowName?.let { repeatedFlow.setName(it) }
            repeatedFlow
        } else {
            val repeatedFlow =
                layout.addFlow().setType(Flow.Type.REPEATED).setVariable(arrayVariable).setSectionFlow(true)
                    .setWebEditingType(SECTION)
            flowName?.let { repeatedFlow.setName(it) }
            val repeatedFlowText = repeatedFlow.addParagraph().addText()
            innerFlows.forEach { repeatedFlowText.appendFlow(it) }
            repeatedFlow
        }
    }

    private fun buildUnmappedRepeatedContentFallback(
        model: RepeatedContent,
        layout: Layout,
        variableStructure: VariableStructure,
        languages: List<String>,
    ): Flow {
        val varName = getVariableNameFromPath(model.variablePath, variableStructure)
        val warning = Paragraph("<repeated by unmapped $$varName$>")
        return buildDocumentContentAsSingleFlow(
            layout, variableStructure, listOf(warning) + model.content, languages = languages
        )
    }

    private fun buildWfdXmlSection(model: ColumnLayout, layout: Layout): WfdXmlSection {
        val section = layout.addSection().setNumberOfColumns(model.numberOfColumns)

        model.gutterWidth?.let { section.setGutterWidth(it.toMeters()) }

        model.balancingType?.let {
            section.setBalancingType(
                when (it) {
                    ColumnBalancingType.FirstColumn -> WfdXmlSection.BalancingType.FIRST_COLUMN
                    ColumnBalancingType.Balanced -> WfdXmlSection.BalancingType.BALANCED
                    ColumnBalancingType.Unbalanced -> WfdXmlSection.BalancingType.UNBALANCED
                }
            )
        }

        model.applyTo?.let {
            section.setApplyTo(
                when (it) {
                    ColumnApplyTo.WholeTemplate -> WfdXmlSection.ApplyTo.WHOLE_TEMPLATE
                    ColumnApplyTo.ThisBlockOnly -> WfdXmlSection.ApplyTo.THIS_BLOCK_ONLY
                }
            )
        }

        return section
    }

    private fun resolveVariableNameAndPath(
        variablePath: VariablePath, variableStructure: VariableStructure
    ): Pair<String, String>? {
        val fullPath = when (variablePath) {
            is LiteralPath -> variablePath.path
            is VariableRefPath -> {
                val parentVarPathData = variableStructure.structure[variablePath.variableId] ?: return null
                val parentVarName =
                    parentVarPathData.name ?: variableRepository.findOrFail(variablePath.variableId).nameOrId()
                val parentVarPath =
                    parentVarPathData.path.resolve(variableStructure, variableRepository::findOrFail) ?: return null
                "$parentVarPath.$parentVarName"
            }
        }
        val normalized = removeDataFromVariablePath(removeValueFromVariablePath(fullPath))
        if (normalized.isBlank()) return null
        val parts = normalized.split(".")
        return parts.last() to (if (parts.size > 1) "Data.${parts.dropLast(1).joinToString(".")}" else "Data")
    }

    fun buildTable(
        layout: Layout, variableStructure: VariableStructure, model: Table, languages: List<String>
    ): WfdXmlTable {
        val table = layout.addTable().setDisplayAsImage(false)

        if (model.tableStyleName != null) {
            if (output == InspireOutput.Designer && getTableStyleByName(layout, model.tableStyleName) == null) {
                layout.addTableStyle().setName(model.tableStyleName)
            }
            table.setExistingTableStyle("Others.${resolveTableStyleName(model.tableStyleName)}")
        }

        if (model.columnWidths.isNotEmpty()) {
            model.columnWidths.forEach { table.addColumn(it.minWidth.toMeters(), it.percentWidth) }
        } else {
            val numberOfColumns = when (val firstRow = model.rows.firstOrNull()) {
                is Table.Row -> firstRow.cells.size
                is Table.RepeatedRow -> firstRow.rows.firstOrNull()?.cells?.size ?: 0
                null -> 0
            }
            repeat(numberOfColumns) { table.addColumn() }
        }

        if (model.minWidth != null) {
            table.setMinWidth(model.minWidth.toMeters())
        }
        if (model.maxWidth != null) {
            table.setMaxWidth(model.maxWidth.toMeters())
        }
        if (model.percentWidth != null) {
            table.setPercentWidth(model.percentWidth)
        }

        table.setAlignment(
            when (model.alignment) {
                TableAlignment.Left -> WfdXmlTable.TableAlignment.LEFT
                TableAlignment.Center -> WfdXmlTable.TableAlignment.CENTER
                TableAlignment.Right -> WfdXmlTable.TableAlignment.RIGHT
                TableAlignment.Inherit -> WfdXmlTable.TableAlignment.INHERIT
            }
        )

        buildTableBorderStyle(model.border, layout, table::setBorderStyle)

        if (model.header.isNotEmpty() || model.firstHeader.isNotEmpty() || model.footer.isNotEmpty() || model.lastFooter.isNotEmpty()) {
            val headerFooterRowSet = layout.addRowSetHeaderFooter()
            table.setRowSet(headerFooterRowSet)

            model.header.buildRowSetGroup(layout, variableStructure, languages)?.let { headerFooterRowSet.setHeader(it) }
            model.firstHeader.buildRowSetGroup(layout, variableStructure, languages)?.let { headerFooterRowSet.setFirstHeader(it) }
            model.footer.buildRowSetGroup(layout, variableStructure, languages)?.let { headerFooterRowSet.setFooter(it) }
            model.lastFooter.buildRowSetGroup(layout, variableStructure, languages)?.let { headerFooterRowSet.setLastFooter(it) }

            model.rows.buildRowSetGroup(layout, variableStructure, languages)?.let { headerFooterRowSet.setBody(it) }
        } else {
            model.rows.buildRowSetGroup(layout, variableStructure, languages)?.let { table.setRowSet(it) }
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
                    .setWebEditingType(SECTION).setDisplayName(caseName)
                    .also { it.addParagraph().addText().appendFlow(contentFlow) }

            firstMatchFlow.addLineForSelectByInlineCondition(
                displayRule.toScript(
                    layout,
                    variableStructure,
                    variableRepository::findOrFail,
                    displayRuleRepository::findOrFail,
                    ::getDisplayRulePath,
                    output,
                    projectConfig.interactiveTenant
                ),
                caseFlow
            )
        }

        if (model.default.isNotEmpty()) {
            val contentFlow =
                buildDocumentContentAsSingleFlow(layout, variableStructure, model.default, null, null, languages)

            val caseFlow =
                if (isInline) contentFlow else layout.addFlow().setSectionFlow(true).setType(Flow.Type.SIMPLE)
                    .setWebEditingType(SECTION).setDisplayName("Else Case")
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
            val contentFlow: Flow = caseFlows[language] ?: layout.addFlow().setType(Flow.Type.SIMPLE).setDisplayName("Case $language")
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
                Paragraph(
                    listOf(Text(listOf(), null, null)),
                    null,
                    null
                )
            )
        }
    }

    private fun TextStyle.resolve(): TextStyle {
        val targetId = this.targetId
        return if (targetId == null) {
            this
        } else {
            textStyleRepository.findOrFail(targetId.id).resolve()
        }
    }

    private fun ParagraphStyle.resolve(): ParagraphStyle {
        val targetId = this.targetId
        return if (targetId == null) {
            this
        } else {
            paragraphStyleRepository.findOrFail(targetId.id).resolve()
        }
    }

    protected fun addPdfMetadataToPages(
        layout: Layout, documentObject: DocumentObject, variableStructure: VariableStructure
    ) {
        val pdfMetadata = documentObject.pdfMetadata ?: return

        val metadataMap = mapOf(
            SheetNameType.PDF_TITLE to Pair("TaggingTitle", pdfMetadata.title),
            SheetNameType.PDF_AUTHOR to Pair("TaggingAuthor", pdfMetadata.author),
            SheetNameType.PDF_SUBJECT to Pair("TaggingSubject", pdfMetadata.subject),
            SheetNameType.PDF_KEYWORDS to Pair("TaggingKeywords", pdfMetadata.keywords),
            SheetNameType.PDF_PRODUCER to Pair("TaggingProduce", pdfMetadata.producer)
        )

        metadataMap.forEach { (type, data) ->
            val (variableName, value) = data
            if (!value.isNullOrEmpty()) {
                val variable =
                    layout.data.addVariable().setName(variableName).setKind(VariableKind.CALCULATED).setScript(
                        variableStringContentToScript(
                            value, layout, variableStructure, variableRepository::findOrFail
                        )
                    )
                layout.pages.addSheetName(type, variable)
            }
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

fun DisplayRule.toScript(
    layout: Layout,
    variableStructure: VariableStructure,
    findVar: (String) -> Variable,
    findRule: (String) -> DisplayRule,
    getDisplayRulePath: (DisplayRule) -> IcmPath,
    output: InspireOutput,
    interactiveTenant: String,
): String {
    val rule = this.resolveTarget(findRule)
    val def = rule.definition ?: error("Display rule '${rule.id}' definition is null.")

    return if (rule.internal || rule.definition?.containsFunction() == true || output == InspireOutput.Designer) {
        def.toScript(layout, variableStructure, findVar)
    } else {
        for (ref in rule.collectRefs()) {
            when (ref) {
                is VariableRef -> {
                    val variableModel = findVar(ref.id)
                    val variablePathData = variableStructure.structure[ref.id]
                    val resolvedPath = variablePathData?.path?.resolve(variableStructure, findVar)
                    if (!resolvedPath.isNullOrBlank()) {
                        val variableName = variablePathData.name ?: variableModel.nameOrId()

                        getOrCreateVariable(
                            layout.data, variableName, variableModel, resolvedPath
                        )
                    }
                }

                else -> {}
            }
        }

        val path = getDisplayRulePath(rule).toMapInteractive(interactiveTenant)
        "return (do.evalFile(Bool, String(${toScriptStringLiteral(path)}))==true);"
    }
}

fun DisplayRuleDefinition.toScript(
    layout: Layout?, variableStructure: VariableStructure, findVar: (String) -> Variable
): String {
    return "return ${this.group.toScript(layout, variableStructure, findVar)};"
}

fun Group.toScript(
    layout: Layout?, variableStructure: VariableStructure, findVar: (String) -> Variable
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
    layout: Layout?, variableStructure: VariableStructure, findVar: (String) -> Variable
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
    layout: Layout?, variableStructure: VariableStructure, findVar: (String) -> Variable
): ScriptResult {
    return when (this) {
        is Literal -> this.toScript(layout, variableStructure, findVar)
        is Function -> this.toScript(layout, variableStructure, findVar)
    }
}

fun Function.toScript(
    layout: Layout?, variableStructure: VariableStructure, findVar: (String) -> Variable
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
    layout: Layout?, variableStructure: VariableStructure, findVar: (String) -> Variable
): ScriptResult {
    return when (dataType) {
        LiteralDataType.Variable -> variableToScript(value, layout, variableStructure, findVar)
        LiteralDataType.String -> Success("String(${toScriptStringLiteral(value)})")
        LiteralDataType.Number -> Success(value)
        LiteralDataType.Boolean -> Success(value.lowercase().toBooleanStrict().toString())
    }
}

internal fun VariablePath.resolve(variableStructure: VariableStructure, findVariable: (String) -> Variable): String? {
    return when (this) {
        is LiteralPath -> this.path
        is VariableRefPath -> {
            val parentVarPathData = variableStructure.structure[this.variableId] ?: return null
            val parentVarPath =
                parentVarPathData.path.resolve(variableStructure, findVariable)?.takeIf { it.isNotBlank() }
                    ?: return null

            val parentVar = findVariable(this.variableId)
            val parentVarName = parentVarPathData.name ?: parentVar.nameOrId()

            when (parentVar.dataType) {
                DataTypeModel.Array -> "$parentVarPath.$parentVarName.Value"
                DataTypeModel.SubTree -> "$parentVarPath.$parentVarName"
                else -> error("Variable '${this.variableId}' of type ${parentVar.dataType} is used in path. Only Array and Subtree can be referenced.")
            }
        }
    }
}

private fun variableStringContentToScript(
    variableStringContent: List<VariableStringContent>,
    layout: Layout,
    variableStructure: VariableStructure,
    findVar: (String) -> Variable
): String {
    val scriptParts = variableStringContent.map {
        when (it) {
            is StringValue -> toScriptStringLiteral(it.value)
            is VariableRef -> {
                when (val variableScript = variableToScript(it.id, layout, variableStructure, findVar)) {
                    is Success -> "$variableScript.toString()"
                    is Failure -> toScriptStringLiteral("$${variableScript.variableName}$")
                }
            }
        }
    }

    return "return ${scriptParts.joinToString(" + ")};"
}

fun variableToScript(
    id: String, layout: Layout?, variableStructure: VariableStructure, findVar: (String) -> Variable
): ScriptResult {
    val variableModel = findVar(id)
    val variablePathData = variableStructure.structure[id]
    val resolvedPath = variablePathData?.path?.resolve(variableStructure, findVar)?.takeIf { it.isNotBlank() }
    return if (resolvedPath.isNullOrBlank()) {
        Failure(variablePathData?.name ?: variableModel.nameOrId())
    } else {
        val variableName = variablePathData.name ?: variableModel.nameOrId()

        if (layout != null) {
            getOrCreateVariable(layout.data, variableName, variableModel, resolvedPath)
        }

        Success((resolvedPath.split(".") + variableName).joinToString(".") { pathPart ->
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
            DataTypeModel.Boolean -> this.setValue(defaultVal.lowercase().toBooleanStrict())
            DataTypeModel.Array, DataTypeModel.SubTree -> {}
        }
    }

    return this
}
