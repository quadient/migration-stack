package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.data.DisplayRuleModelRef
import com.quadient.migration.data.DocumentContentModel
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.FirstMatchModel
import com.quadient.migration.data.AreaModel
import com.quadient.migration.data.ImageModel
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphModel
import com.quadient.migration.data.ParagraphModel.TextModel
import com.quadient.migration.data.ParagraphStyleDefinitionModel
import com.quadient.migration.data.ParagraphStyleModel
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.data.StringModel
import com.quadient.migration.data.TableModel
import com.quadient.migration.data.TextStyleDefinitionModel
import com.quadient.migration.data.TextStyleModel
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.data.VariableModel
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.data.VariableStructureModel
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Binary
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.Group
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.SuperOrSubscript
import com.quadient.migration.shared.TabType
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Color
import com.quadient.wfdxml.api.layoutnodes.FillStyle
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Font
import com.quadient.wfdxml.api.layoutnodes.Image
import com.quadient.wfdxml.api.layoutnodes.LocationType
import com.quadient.wfdxml.api.layoutnodes.ParagraphStyle
import com.quadient.wfdxml.api.layoutnodes.ParagraphStyle.LineSpacingType.*
import com.quadient.wfdxml.api.layoutnodes.TabulatorType
import com.quadient.wfdxml.api.layoutnodes.data.Data
import com.quadient.wfdxml.api.layoutnodes.data.DataType
import com.quadient.wfdxml.api.layoutnodes.data.Variable
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind
import com.quadient.wfdxml.api.layoutnodes.flow.Text
import com.quadient.wfdxml.api.layoutnodes.tables.GeneralRowSet
import com.quadient.wfdxml.api.layoutnodes.tables.RowSet
import com.quadient.wfdxml.api.layoutnodes.tables.Table
import com.quadient.wfdxml.api.module.Layout
import com.quadient.wfdxml.internal.data.WorkFlowTreeDefinition
import com.quadient.wfdxml.internal.layoutnodes.data.DataImpl
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import com.quadient.migration.shared.DataType as DataTypeModel

abstract class InspireDocumentObjectBuilder(
    protected val documentObjectRepository: DocumentObjectInternalRepository,
    protected val textStyleRepository: TextStyleInternalRepository,
    protected val paragraphStyleRepository: ParagraphStyleInternalRepository,
    protected val variableRepository: VariableInternalRepository,
    protected val variableStructureRepository: VariableStructureInternalRepository,
    protected val displayRuleRepository: DisplayRuleInternalRepository,
    protected val imageRepository: ImageInternalRepository,
    protected val projectConfig: ProjectConfig,
) {
    protected val logger = LoggerFactory.getLogger(this::class.java)!!

    abstract fun getDocumentObjectPath(nameOrId: String, type: DocumentObjectType, targetFolder: IcmPath?): String

    abstract fun getDocumentObjectPath(documentObject: DocumentObjectModel): String

    abstract fun getImagePath(
        id: String, imageType: ImageType, name: String?, targetFolder: IcmPath?, sourcePath: String?
    ): String

    abstract fun getImagePath(image: ImageModel): String

    abstract fun getStyleDefinitionPath(): String

    abstract fun buildDocumentObject(documentObject: DocumentObjectModel): String

    protected open fun wrapSuccessFlowInConditionFlow(
        layout: Layout, variableStructure: VariableStructureModel, ruleDef: DisplayRuleDefinition, successFlow: Flow
    ): Flow {
        return layout.addFlow().setType(Flow.Type.SELECT_BY_CONDITION).addLineForSelectByCondition(
            layout.data.addVariable().setKind(VariableKind.CALCULATED).setDataType(DataType.BOOL)
                .setScript(ruleDef.toScript(layout, variableStructure)), successFlow
        )
    }

    protected open fun buildSuccessRowWrappedInConditionRow(
        layout: Layout,
        variableStructure: VariableStructureModel,
        ruleDef: DisplayRuleDefinition,
        multipleRowSet: GeneralRowSet
    ): GeneralRowSet {
        val successRow = layout.addRowSet().setType(RowSet.Type.SINGLE_ROW)

        multipleRowSet.addRowSet(
            layout.addRowSet().setType(RowSet.Type.SELECT_BY_CONDITION).addLineForSelectByCondition(
                layout.data.addVariable().setKind(VariableKind.CALCULATED).setDataType(DataType.BOOL)
                    .setScript(ruleDef.toScript(layout, variableStructure)), successRow
            )
        )

        return successRow
    }

    fun buildStyles(textStyles: List<TextStyleModel>, paragraphStyles: List<ParagraphStyleModel>): String {
        logger.debug("Starting to build style definition.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()

        buildTextStyles(layout, textStyles)
        buildParagraphStyles(layout, paragraphStyles)

        logger.debug("Successfully built style definition.")
        return builder.build()
    }

    protected fun buildDocumentContentAsFlows(
        layout: Layout,
        variableStructure: VariableStructureModel,
        content: List<DocumentContentModel>,
        flowName: String? = null
    ): List<Flow> {
        val mutableContent = content.toMutableList()

        var idx = 0
        val flowModels = mutableListOf<FlowModel>()
        while (idx < mutableContent.size) {
            val contentPart = mutableContent[idx]
            when (contentPart) {
                is TableModel, is ParagraphModel, is ImageModelRef -> {
                    val flowParts = gatherFlowParts(mutableContent, idx)
                    idx += flowParts.size - 1
                    flowModels.add(FlowModel.Composite(flowParts))
                }

                is DocumentObjectModelRef -> flowModels.add(FlowModel.DocumentObject(contentPart))
                is AreaModel -> mutableContent.addAll(idx + 1, contentPart.content)
                is FirstMatchModel -> flowModels.add(FlowModel.FirstMatch(contentPart))
            }
            idx++
        }

        val flowCount = flowModels.size
        var flowSuffix = 1
        return flowModels.mapNotNull {
            when (it) {
                is FlowModel.DocumentObject -> buildDocumentObjectRef(layout, variableStructure, it.ref)
                is FlowModel.Composite -> {
                    if (flowName == null) {
                        buildCompositeFlow(layout, variableStructure, it.parts)
                    } else {
                        val name = if (flowCount == 1) flowName else "$flowName $flowSuffix"
                        flowSuffix++
                        buildCompositeFlow(layout, variableStructure, it.parts, name)
                    }
                }

                is FlowModel.FirstMatch -> {
                    if (flowName == null) {
                        buildFirstMatch(layout, variableStructure, it.model, false)
                    } else {
                        val name = if (flowCount == 1) flowName else "$flowName $flowSuffix"
                        flowSuffix++
                        buildFirstMatch(layout, variableStructure, it.model, false, name)
                    }
                }
            }
        }
    }

    sealed interface FlowModel {
        data class Composite(val parts: List<DocumentContentModel>) : FlowModel
        data class DocumentObject(val ref: DocumentObjectModelRef) : FlowModel
        data class FirstMatch(val model: FirstMatchModel) : FlowModel
    }

    protected fun List<Flow>.toSingleFlow(
        layout: Layout,
        variableStructure: VariableStructureModel,
        flowName: String? = null,
        displayRuleRef: DisplayRuleModelRef? = null,
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
            val displayRule = displayRuleRepository.findModelOrFail(displayRuleRef.id)
            if (displayRule.definition == null) {
                error("Display rule '${displayRuleRef.id}' definition is null.")
            }

            wrapSuccessFlowInConditionFlow(layout, variableStructure, displayRule.definition, singleFlow)
        }
    }

    protected fun buildDocumentContentAsSingleFlow(
        layout: Layout,
        variableStructure: VariableStructureModel,
        content: List<DocumentContentModel>,
        flowName: String? = null,
        displayRuleRef: DisplayRuleModelRef? = null,
    ): Flow {
        return buildDocumentContentAsFlows(layout, variableStructure, content, flowName).toSingleFlow(
            layout, variableStructure, flowName, displayRuleRef
        )
    }

    protected fun initVariableStructure(layout: Layout, documentObject: DocumentObjectModel): VariableStructureModel {
        val documentObjectVariableStructure =
            documentObject.variableStructureRef?.let { variableStructureRepository.findModelOrFail(it.id) }

        val variableStructureModel =
            documentObjectVariableStructure ?: variableStructureRepository.listAllModel().maxByOrNull { it.lastUpdated }
            ?: VariableStructureModel(
                id = "defaultVariableStructure",
                lastUpdated = Clock.System.now(),
                created = Clock.System.now(),
                structure = mutableMapOf(),
                customFields = CustomFieldMap()
            )

        val normalizedVariablePaths =
            variableStructureModel.structure.map { (_, variablePath) -> removeDataFromVariablePath(variablePath.value) }
                .filter { it.isNotBlank() }

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

    protected fun buildTextStyles(layout: Layout, textStyleModels: List<TextStyleModel>) {
        val usedFonts = mutableMapOf<String, Font>()
        val usedColorFillStyles = mutableMapOf<String, Pair<Color, FillStyle>>()

        textStyleModels.forEach { styleModel ->
            val definition = styleModel.resolve()

            val textStyle = layout.addTextStyle().setName(styleModel.nameOrId())

            if (definition.fontFamily != null) {
                val usedFont = usedFonts[definition.fontFamily]
                if (usedFont != null || definition.fontFamily == "Arial") {
                    textStyle.setFont(usedFonts[definition.fontFamily])
                } else {
                    val newFont = layout.addFont().setFont(definition.fontFamily)
                    usedFonts.put(definition.fontFamily, newFont)
                    textStyle.setFont(newFont)
                }
            }

            if (definition.foregroundColor != null) {
                val colorId = definition.foregroundColor.toString()
                val usedColorFillStyle = usedColorFillStyles[colorId]
                if (usedColorFillStyle != null) {
                    textStyle.setFillStyle(usedColorFillStyle.second)
                } else {
                    val newColor = layout.addColor().setRGB(
                        definition.foregroundColor.red(),
                        definition.foregroundColor.green(),
                        definition.foregroundColor.blue()
                    )
                    val newFillStyle = layout.addFillStyle().setColor(newColor)
                    usedColorFillStyles.put(colorId, Pair(newColor, newFillStyle))
                    textStyle.setFillStyle(newFillStyle)
                }
            }

            definition.size?.let { textStyle.setFontSize(definition.size.toPoints()) }
            textStyle.setBold(definition.bold)
            textStyle.seItalic(definition.italic)
            textStyle.setUnderline(definition.underline)
            textStyle.setStrikeThrough(definition.strikethrough)
            when (definition.superOrSubscript) {
                SuperOrSubscript.Subscript -> textStyle.setSubScript(true).setSuperScript(false)
                SuperOrSubscript.Superscript -> textStyle.setSubScript(false).setSuperScript(true)
                SuperOrSubscript.None -> textStyle.setSubScript(false).setSuperScript(false)
            }
            definition.interspacing?.let { textStyle.setInterSpacing(it.toMeters()) }
        }
    }

    protected fun buildParagraphStyles(layout: Layout, paragraphStyleModels: List<ParagraphStyleModel>) {
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
        }
    }

    protected fun getImagePlaceholder(imageModel: ImageModel): String? {
        if (imageModel.imageType == ImageType.Unknown) {
            logger.debug("Image '${imageModel.nameOrId()}' has unknown type. Rendering placeholder text instead.")
            return "Unknown image: ${imageModel.nameOrId()}"
        }

        if (imageModel.sourcePath.isNullOrBlank()) {
            logger.debug("Image '${imageModel.nameOrId()}' has missing source path. Rendering placeholder text instead.")
            return "Image without source path: ${imageModel.nameOrId()}"
        }

        return null
    }

    protected fun buildImage(layout: Layout, imageModel: ImageModel): Image {
        val image = layout.addImage().setName(imageModel.nameOrId())
            .setImageLocation(getImagePath(imageModel), LocationType.ICM)

        if (imageModel.options != null) {
            imageModel.options.resizeWidth?.let { image.setResizeWidth(it.toMeters()) }
            imageModel.options.resizeHeight?.let { image.setResizeHeight(it.toMeters()) }
        }

        return image
    }

    private fun buildCompositeFlow(
        layout: Layout,
        variableStructure: VariableStructureModel,
        documentContentModelParts: List<DocumentContentModel>,
        flowName: String? = null
    ): Flow {
        val flow = layout.addFlow().setType(Flow.Type.SIMPLE)
        flowName?.let { flow.setName(it) }

        documentContentModelParts.forEach {
            when (it) {
                is ParagraphModel -> buildParagraph(layout, variableStructure, flow, it)
                is TableModel -> flow.addParagraph().addText().appendTable(buildTable(layout, variableStructure, it))
                is ImageModelRef -> buildAndAppendImage(layout, flow.addParagraph().addText(), it)
                else -> error("Content part type ${it::class.simpleName} is not allowed in composite flow.")
            }
        }

        return flow
    }

    private fun buildDocumentObjectRef(
        layout: Layout, variableStructure: VariableStructureModel, documentObjectRef: DocumentObjectModelRef
    ): Flow? {
        val documentModel = documentObjectRepository.findModelOrFail(documentObjectRef.id)

        if (documentModel.type == DocumentObjectType.Unsupported) {
            logger.debug("Skipping document content part ${documentObjectRef.id} due to unsupported type.")
            return null
        }

        val flow = getFlowByName(layout, documentModel.nameOrId()) ?: if (documentModel.internal) {
            buildDocumentContentAsSingleFlow(
                layout, variableStructure, documentModel.content, documentModel.nameOrId(), documentModel.displayRuleRef
            )
        } else {
            layout.addFlow().setName(documentModel.nameOrId()).setType(Flow.Type.DIRECT_EXTERNAL)
                .setLocation(getDocumentObjectPath(documentModel))
        }

        if (documentObjectRef.displayRuleRef != null) {
            val displayRule = displayRuleRepository.findModelOrFail(documentObjectRef.displayRuleRef.id)
            if (displayRule.definition == null) {
                error("Display rule '${documentObjectRef.displayRuleRef.id}' definition is null.")
            }

            return wrapSuccessFlowInConditionFlow(layout, variableStructure, displayRule.definition, flow)
        }

        return flow
    }

    private fun gatherFlowParts(content: List<DocumentContentModel>, startIndex: Int): List<DocumentContentModel> {
        val flowParts = mutableListOf<DocumentContentModel>()

        var index = startIndex

        do {
            val contentPart = content[index]
            if (contentPart is TableModel || contentPart is ParagraphModel || contentPart is ImageModelRef) {
                flowParts.add(contentPart)
                index++
            } else {
                break
            }
        } while (index < content.size)

        return flowParts
    }

    private fun buildParagraph(
        layout: Layout, variableStructure: VariableStructureModel, flow: Flow, paragraphModel: ParagraphModel
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
            val text = if (textModel.displayRuleRef == null) {
                paragraph.addText()
            } else {
                buildSuccessFlowWrappedInInlineConditionFlow(
                    layout, variableStructure, textModel.displayRuleRef.id, paragraph.addText()
                ).addParagraph().also {
                    if (paragraphStyle != null) it.setExistingParagraphStyle("ParagraphStyles.${paragraphStyle.nameOrId()}")
                }.addText()
            }

            findTextStyle(textModel)?.also { text.setExistingTextStyle("TextStyles.${it.nameOrId()}") }

            textModel.content.forEach {
                when (it) {
                    is StringModel -> text.appendText(it.value)
                    is VariableModelRef -> text.appendVariable(it, layout, variableStructure)
                    is TableModel -> text.appendTable(buildTable(layout, variableStructure, it))
                    is DocumentObjectModelRef -> buildDocumentObjectRef(layout, variableStructure, it)?.also { flow ->
                        text.appendFlow(flow)
                    }

                    is ImageModelRef -> buildAndAppendImage(layout, text, it)
                    is FirstMatchModel -> text.appendFlow(buildFirstMatch(layout, variableStructure, it, true))
                }
            }
        }
    }

    private fun buildAndAppendImage(layout: Layout, text: Text, ref: ImageModelRef) {
        val imageModel = imageRepository.findModelOrFail(ref.id)

        val imagePlaceholder = getImagePlaceholder(imageModel)
        if (imagePlaceholder != null) {
            text.appendText(imagePlaceholder)
            return
        }

        text.appendImage(buildImage(layout, imageModel))
    }

    private fun Text.appendVariable(
        ref: VariableModelRef, layout: Layout, variableStructure: VariableStructureModel
    ): Text {
        val variableModel = variableRepository.findModelOrFail(ref.id)

        val variablePath = variableStructure.structure[ref]?.value
        if (variablePath.isNullOrBlank()) {
            this.appendText("""$${variableModel.nameOrId()}$""")
        } else {
            this.appendVariable(getOrCreateVariable(layout.data, variableModel, variablePath))
        }

        return this
    }

    private fun findParagraphStyle(paragraphModel: ParagraphModel): ParagraphStyleModel? {
        if (paragraphModel.styleRef == null) return null

        val paraStyleModel = paragraphStyleRepository.firstWithDefinitionModel(paragraphModel.styleRef.id)
        if (paraStyleModel == null) {
            error("Paragraph style definition for ${paragraphModel.styleRef.id} not found.")
        }

        return paraStyleModel
    }

    private fun findTextStyle(textModel: TextModel): TextStyleModel? {
        if (textModel.styleRef == null) return null

        val textStyleModel = textStyleRepository.firstWithDefinitionModel(textModel.styleRef.id)
        if (textStyleModel == null) {
            error("Text style definition for ${textModel.styleRef.id} not found.")
        }

        return textStyleModel
    }

    private fun buildSuccessFlowWrappedInInlineConditionFlow(
        layout: Layout, variableStructure: VariableStructureModel, displayRuleId: String, text: Text
    ): Flow {
        val displayRule = displayRuleRepository.findModelOrFail(displayRuleId)
        if (displayRule.definition == null) {
            error("Display rule '$displayRuleId' definition is null.")
        }

        val successFlow = layout.addFlow().setType(Flow.Type.SIMPLE)

        text.appendFlow(
            layout.addFlow().setType(Flow.Type.SELECT_BY_INLINE_CONDITION).addLineForSelectByInlineCondition(
                displayRule.definition.toScript(layout, variableStructure), successFlow
            )
        )

        return successFlow
    }

    private fun buildTable(layout: Layout, variableStructure: VariableStructureModel, model: TableModel): Table {
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
                val displayRule = displayRuleRepository.findModelOrFail(rowModel.displayRuleRef.id)
                if (displayRule.definition == null) {
                    error("Display rule '${rowModel.displayRuleRef.id}' definition is null.")
                }

                buildSuccessRowWrappedInConditionRow(
                    layout, variableStructure, displayRule.definition, rowset
                )
            }

            rowModel.cells.forEach { cellModel ->
                val cellFlow = buildDocumentContentAsSingleFlow(layout, variableStructure, cellModel.content)
                row.addCell(
                    layout.addCell().setSpanLeft(cellModel.mergeLeft).setSpanUp(cellModel.mergeUp)
                        .setFlowToNextPage(true).setFlow(cellFlow)
                )
            }
        }

        return table
    }

    private fun buildFirstMatch(
        layout: Layout,
        variableStructure: VariableStructureModel,
        model: FirstMatchModel,
        isInline: Boolean,
        flowName: String? = null
    ): Flow {
        val firstMatchFlow = layout.addFlow().setType(Flow.Type.SELECT_BY_INLINE_CONDITION)
        flowName?.let { firstMatchFlow.setName(it) }

        model.cases.forEachIndexed { i, case ->
            val displayRule = displayRuleRepository.findModelOrFail(case.displayRuleRef.id)
            if (displayRule.definition == null) {
                error("Display rule '${case.displayRuleRef.id}' definition is null.")
            }

            val caseName = case.name ?: "Case ${i + 1}"

            val contentFlow = buildDocumentContentAsSingleFlow(layout, variableStructure, case.content)

            val caseFlow =
                if (isInline) contentFlow else layout.addFlow().setSectionFlow(true).setType(Flow.Type.SIMPLE)
                    .setWebEditingType(Flow.WebEditingType.SECTION).setDisplayName(caseName)
                    .also { it.addParagraph().addText().appendFlow(contentFlow) }

            firstMatchFlow.addLineForSelectByInlineCondition(
                displayRule.definition.toScript(layout, variableStructure), caseFlow
            )
        }

        if (model.default.isNotEmpty()) {
            val contentFlow = buildDocumentContentAsSingleFlow(layout, variableStructure, model.default)

            val caseFlow =
                if (isInline) contentFlow else layout.addFlow().setSectionFlow(true).setType(Flow.Type.SIMPLE)
                    .setWebEditingType(Flow.WebEditingType.SECTION).setDisplayName("Else Case")
                    .also { it.addParagraph().addText().appendFlow(contentFlow) }

            firstMatchFlow.setDefaultFlow(caseFlow)
        }

        return firstMatchFlow
    }

    protected fun DisplayRuleDefinition.toScript(layout: Layout, variableStructure: VariableStructureModel): String {
        return "return ${this.group.toScript(layout, variableStructure)};"
    }

    private fun Group.toScript(layout: Layout, variableStructure: VariableStructureModel): String {
        val expressions = """(${
            items.joinToString(
                separator = " ${operator.toInlineCondition()} ", transform = {
                    when (it) {
                        is Binary -> it.toScript(layout, variableStructure)
                        is Group -> it.toScript(layout, variableStructure)
                    }
                })
        })"""
        return if (negation) {
            "not $expressions"
        } else {
            expressions
        }
    }

    private fun Binary.toScript(layout: Layout, variableStructure: VariableStructureModel): String {
        val leftScriptResult = left.toScript(layout, variableStructure)
        val rightScriptResult = right.toScript(layout, variableStructure)

        val binary = "$leftScriptResult${operator.toInlineCondition()}$rightScriptResult"
        return if (leftScriptResult is ScriptResult.Success && rightScriptResult is ScriptResult.Success) {
            binary
        } else {
            "String('${binary.replace("'", "")}')${BinOp.Equals.toInlineCondition()}String('unmapped')"
        }
    }

    private fun Literal.toScript(layout: Layout, variableStructure: VariableStructureModel): ScriptResult {
        return when (dataType) {
            LiteralDataType.Variable -> variableToScript(value, layout, variableStructure)
            LiteralDataType.String -> ScriptResult.Success("String('$value')")
            LiteralDataType.Number -> ScriptResult.Success(value)
            LiteralDataType.Boolean -> ScriptResult.Success(value.lowercase().toBooleanStrict().toString())
        }
    }

    private fun variableToScript(
        id: String, layout: Layout, variableStructure: VariableStructureModel
    ): ScriptResult {
        val variable = variableRepository.findModel(id)
        if (variable == null) {
            error("Variable $id not found")
        }

        val variablePath = variableStructure.structure[VariableModelRef(id)]?.value
        return if (variablePath == null) {
            ScriptResult.Failure(variable.nameOrId())
        } else {
            getOrCreateVariable(layout.data, variable, variablePath)

            ScriptResult.Success((variablePath.split(".") + variable.nameOrId()).joinToString(".") { pathPart ->
                when (pathPart.lowercase()) {
                    "value" -> "Current"
                    "data" -> "DATA"
                    else -> if (pathPart.first().isDigit()) "_$pathPart" else pathPart
                }
            })
        }
    }

    private fun getOrCreateVariable(data: Data, variableModel: VariableModel, variablePath: String): Variable {
        val variable = getVariable(data as DataImpl, variableModel.nameOrId(), variablePath)
        return variable ?: data.addVariable().setName(variableModel.nameOrId()).setKind(VariableKind.DISCONNECTED)
            .setDataType(getDataType(variableModel.dataType)).setExistingParentId(variablePath)
            .setValueIfAvailable(variableModel)
    }

    private fun Variable.setValueIfAvailable(variableModel: VariableModel): Variable {
        if (!variableModel.defaultValue.isNullOrBlank()) {
            when (variableModel.dataType) {
                DataTypeModel.String, DataTypeModel.DateTime -> this.setValue(variableModel.defaultValue)
                DataTypeModel.Integer -> this.setValue(variableModel.defaultValue.toInt())
                DataTypeModel.Integer64 -> this.setValue(variableModel.defaultValue.toLong())
                DataTypeModel.Double, DataTypeModel.Currency -> this.setValue(variableModel.defaultValue.toDouble())
                DataTypeModel.Boolean -> this.setValue(
                    variableModel.defaultValue.lowercase().toBooleanStrict()
                )
            }
        }

        return this
    }

    private fun TextStyleModel.resolve(): TextStyleDefinitionModel {
        return when (this.definition) {
            is TextStyleDefinitionModel -> this.definition
            is TextStyleModelRef -> {
                textStyleRepository.findModel(this.definition.id)?.resolve() ?: error("Invalid text style reference")
            }
        }
    }

    private fun ParagraphStyleModel.resolve(): ParagraphStyleDefinitionModel {
        return when (this.definition) {
            is ParagraphStyleDefinitionModel -> this.definition
            is ParagraphStyleModelRef -> paragraphStyleRepository.findModelOrFail(this.definition.id).resolve()
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