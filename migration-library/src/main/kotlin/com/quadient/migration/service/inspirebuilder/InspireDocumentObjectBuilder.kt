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
import com.quadient.migration.data.SelectByLanguageModel
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
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.FlowModel.*
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.ScriptResult
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.ScriptResult.*
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Binary
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.Function
import com.quadient.migration.shared.Group
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.LiteralDataType
import com.quadient.migration.shared.LiteralOrFunctionCall
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
import com.quadient.wfdxml.api.layoutnodes.font.SubFont
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ifEmpty
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
    protected val ipsService: IpsService,
) {
    protected val logger = LoggerFactory.getLogger(this::class.java)!!

    protected val fontDataCache = ConcurrentHashMap<FontKey, String>()

    abstract fun getDocumentObjectPath(nameOrId: String, type: DocumentObjectType, targetFolder: IcmPath?): String

    abstract fun getDocumentObjectPath(documentObject: DocumentObjectModel): String

    abstract fun getImagePath(
        id: String, imageType: ImageType, name: String?, targetFolder: IcmPath?, sourcePath: String?
    ): String

    abstract fun getImagePath(image: ImageModel): String

    abstract fun getStyleDefinitionPath(): String

    abstract fun getFontRootFolder(): String

    abstract fun buildDocumentObject(documentObject: DocumentObjectModel, styleDefinitionPath: String?): String

    abstract fun shouldIncludeInternalDependency(documentObject: DocumentObjectModel): Boolean

    protected fun collectLanguages(documentObject: DocumentObjectModel): List<String> {
        val languages = mutableSetOf<String>()

        fun collectLanguagesFromContent(content: List<DocumentContentModel>) {
            for (item in content) {
                when (item) {
                    is SelectByLanguageModel -> item.cases.forEach { languages.add(it.language) }
                    is AreaModel -> collectLanguagesFromContent(item.content)
                    is FirstMatchModel -> {
                        item.cases.forEach { case -> collectLanguagesFromContent(case.content) }
                        collectLanguagesFromContent(item.default)
                    }

                    is TableModel -> item.rows.forEach { row ->
                        row.cells.forEach { cell -> collectLanguagesFromContent(cell.content) }
                    }

                    is DocumentObjectModelRef -> {
                        val documentObject = documentObjectRepository.findModelOrFail(item.id)
                        if (shouldIncludeInternalDependency(documentObject)) {
                            collectLanguagesFromContent(documentObject.content)
                        }
                    }

                    is ParagraphModel -> item.content.forEach { textModel ->
                        textModel.content.forEach { textContent ->
                            when (textContent) {
                                is FirstMatchModel -> {
                                    textContent.cases.forEach { case -> collectLanguagesFromContent(case.content) }
                                    collectLanguagesFromContent(textContent.default)
                                }

                                is TableModel -> textContent.rows.forEach { row ->
                                    row.cells.forEach { cell -> collectLanguagesFromContent(cell.content) }
                                }

                                is DocumentObjectModelRef -> {
                                    val documentObject = documentObjectRepository.findModelOrFail(textContent.id)
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
        layout: Layout, variableStructure: VariableStructureModel, ruleDef: DisplayRuleDefinition, successFlow: Flow
    ): Flow {
        return layout.addFlow().setType(Flow.Type.SELECT_BY_CONDITION).addLineForSelectByCondition(
            layout.data.addVariable().setKind(VariableKind.CALCULATED).setDataType(DataType.BOOL)
                .setScript(ruleDef.toScript(layout, variableStructure, variableRepository::findModelOrFail)),
            successFlow
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
                    .setScript(ruleDef.toScript(layout, variableStructure, variableRepository::findModelOrFail)),
                successRow
            )
        )

        return successRow
    }

    fun buildStyles(textStyles: List<TextStyleModel>, paragraphStyles: List<ParagraphStyleModel>): String {
        logger.debug("Starting to build style definition.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()

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
        variableStructure: VariableStructureModel,
        content: List<DocumentContentModel>,
        flowName: String? = null,
        languages: List<String>,
    ): List<Flow> {
        val mutableContent = content.toMutableList()

        var idx = 0
        val flowModels = mutableListOf<FlowModel>()
        while (idx < mutableContent.size) {
            when (val contentPart = mutableContent[idx]) {
                is TableModel, is ParagraphModel, is ImageModelRef -> {
                    val flowParts = gatherFlowParts(mutableContent, idx)
                    idx += flowParts.size - 1
                    flowModels.add(Composite(flowParts))
                }

                is DocumentObjectModelRef -> flowModels.add(DocumentObject(contentPart))
                is AreaModel -> mutableContent.addAll(idx + 1, contentPart.content)
                is FirstMatchModel -> flowModels.add(FirstMatch(contentPart))
                is SelectByLanguageModel -> flowModels.add(SelectByLanguage(contentPart))
            }
            idx++
        }

        val flowCount = flowModels.size
        var flowSuffix = 1
        return flowModels.mapNotNull {
            when (it) {
                is DocumentObject -> buildDocumentObjectRef(layout, variableStructure, it.ref, languages)
                is Composite -> {
                    if (flowName == null) {
                        buildCompositeFlow(layout, variableStructure, it.parts, null, languages)
                    } else {
                        val name = if (flowCount == 1) flowName else "$flowName $flowSuffix"
                        flowSuffix++
                        buildCompositeFlow(layout, variableStructure, it.parts, name, languages)
                    }
                }

                is FirstMatch -> {
                    if (flowName == null) {
                        buildFirstMatch(layout, variableStructure, it.model, false, null, languages)
                    } else {
                        val name = if (flowCount == 1) flowName else "$flowName $flowSuffix"
                        flowSuffix++
                        buildFirstMatch(layout, variableStructure, it.model, false, name, languages)
                    }
                }

                is SelectByLanguage -> {
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
        data class Composite(val parts: List<DocumentContentModel>) : FlowModel
        data class DocumentObject(val ref: DocumentObjectModelRef) : FlowModel
        data class FirstMatch(val model: FirstMatchModel) : FlowModel
        data class SelectByLanguage(val model: SelectByLanguageModel) : FlowModel
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
        languages: List<String>,
    ): Flow {
        return buildDocumentContentAsFlows(layout, variableStructure, content, flowName, languages).toSingleFlow(
            layout, variableStructure, flowName, displayRuleRef
        )
    }

    protected fun initVariableStructure(layout: Layout, documentObject: DocumentObjectModel): VariableStructureModel {
        val variableStructureId = documentObject.variableStructureRef?.id ?: projectConfig.defaultVariableStructure

        val variableStructureModel =
            variableStructureId?.let { variableStructureRepository.findModelOrFail(it) } ?: VariableStructureModel(
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

    protected fun buildTextStyles(layout: Layout, textStyleModels: List<TextStyleModel>) {
        val arialFont = getFontByName(layout, "Arial")
        require(arialFont != null) { "Layout must contain Arial font." }
        arialFont.setName("Arial").setFontName("Arial")

        val usedFonts = mutableMapOf("Arial" to arialFont)
        val usedColorFillStyles = mutableMapOf<String, Pair<Color, FillStyle>>()

        textStyleModels.forEach { styleModel ->
            val definition = styleModel.resolve()

            val textStyle = layout.addTextStyle().setName(styleModel.nameOrId())

            if (!definition.fontFamily.isNullOrBlank()) {
                val usedFont = usedFonts[definition.fontFamily]
                val font = if (usedFont != null) usedFont else {
                    val newFont = layout.addFont().setName(definition.fontFamily).setFontName(definition.fontFamily)
                    usedFonts[definition.fontFamily] = newFont
                    newFont
                }
                textStyle.setFont(font)

                val subFont = upsertSubFont(font, definition.bold, definition.italic)
                if (subFont != null) {
                    textStyle.setSubFont(subFont)
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
                    usedColorFillStyles[colorId] = Pair(newColor, newFillStyle)
                    textStyle.setFillStyle(newFillStyle)
                }
            }

            definition.size?.let { textStyle.setFontSizeInMeters(definition.size.toMeters()) }
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

    sealed interface ImagePlaceholderResult {
        object RenderAsNormal : ImagePlaceholderResult
        object Skip : ImagePlaceholderResult
        data class Placeholder(val value: String) : ImagePlaceholderResult
    }
    protected fun getImagePlaceholder(imageModel: ImageModel): ImagePlaceholderResult {
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

    protected fun getOrBuildImage(layout: Layout, imageModel: ImageModel): Image {
        val image = getImageByName(layout, imageModel.nameOrId()) ?: layout.addImage().setName(imageModel.nameOrId())
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
        flowName: String? = null,
        languages: List<String>
    ): Flow {
        val flow = layout.addFlow().setType(Flow.Type.SIMPLE)
        flowName?.let { flow.setName(it) }

        documentContentModelParts.forEach {
            when (it) {
                is ParagraphModel -> buildParagraph(layout, variableStructure, flow, it, languages)
                is TableModel -> flow.addParagraph().addText()
                    .appendTable(buildTable(layout, variableStructure, it, languages))

                is ImageModelRef -> buildAndAppendImage(layout, flow.addParagraph().addText(), it)
                else -> error("Content part type ${it::class.simpleName} is not allowed in composite flow.")
            }
        }

        return flow
    }

    private fun buildDocumentObjectRef(
        layout: Layout,
        variableStructure: VariableStructureModel,
        documentObjectRef: DocumentObjectModelRef,
        languages: List<String>,
    ): Flow? {
        val documentModel = documentObjectRepository.findModelOrFail(documentObjectRef.id)

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

        val flow = getFlowByName(layout, documentModel.nameOrId()) ?: if (documentModel.internal) {
            buildDocumentContentAsSingleFlow(
                layout,
                variableStructure,
                documentModel.content,
                documentModel.nameOrId(),
                documentModel.displayRuleRef,
                languages
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
        layout: Layout,
        variableStructure: VariableStructureModel,
        flow: Flow,
        paragraphModel: ParagraphModel,
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
                    is TableModel -> text.appendTable(buildTable(layout, variableStructure, it, languages))
                    is DocumentObjectModelRef -> buildDocumentObjectRef(
                        layout, variableStructure, it, languages
                    )?.also { flow ->
                        text.appendFlow(flow)
                    }

                    is ImageModelRef -> buildAndAppendImage(layout, text, it)
                    is FirstMatchModel -> text.appendFlow(
                        buildFirstMatch(layout, variableStructure, it, true, null, languages)
                    )
                }
            }
        }
    }

    private fun buildAndAppendImage(layout: Layout, text: Text, ref: ImageModelRef) {
        val imageModel = imageRepository.findModelOrFail(ref.id)

        when (val imagePlaceholder = getImagePlaceholder(imageModel)) {
            is ImagePlaceholderResult.Placeholder -> {
                text.appendText(imagePlaceholder.value)
                return
            }
            is ImagePlaceholderResult.RenderAsNormal -> {}
            is ImagePlaceholderResult.Skip -> return
        }

        text.appendImage(getOrBuildImage(layout, imageModel))
    }

    private fun Text.appendVariable(
        ref: VariableModelRef, layout: Layout, variableStructure: VariableStructureModel
    ): Text {
        val variableModel = variableRepository.findModelOrFail(ref.id)

        val variablePathData = variableStructure.structure[ref]
        if (variablePathData == null) {
            this.appendText("""$${variableModel.nameOrId()}$""")
        } else {
            val variableName = variablePathData.name ?: variableModel.nameOrId()
            this.appendVariable(getOrCreateVariable(layout.data, variableName, variableModel, variablePathData.path))
        }

        return this
    }

    private fun findParagraphStyle(paragraphModel: ParagraphModel): ParagraphStyleModel? {
        if (paragraphModel.styleRef == null) return null

        val paraStyleModel = paragraphStyleRepository.firstWithDefinitionModel(paragraphModel.styleRef.id)
            ?: error("Paragraph style definition for ${paragraphModel.styleRef.id} not found.")

        return paraStyleModel
    }

    private fun findTextStyle(textModel: TextModel): TextStyleModel? {
        if (textModel.styleRef == null) return null

        val textStyleModel = textStyleRepository.firstWithDefinitionModel(textModel.styleRef.id)
            ?: error("Text style definition for ${textModel.styleRef.id} not found.")

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
                displayRule.definition.toScript(layout, variableStructure, variableRepository::findModelOrFail),
                successFlow
            )
        )

        return successFlow
    }

    private fun buildTable(
        layout: Layout, variableStructure: VariableStructureModel, model: TableModel, languages: List<String>
    ): Table {
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

        return table
    }

    private fun buildFirstMatch(
        layout: Layout,
        variableStructure: VariableStructureModel,
        model: FirstMatchModel,
        isInline: Boolean,
        flowName: String? = null,
        languages: List<String>,
    ): Flow {
        val firstMatchFlow = layout.addFlow().setType(Flow.Type.SELECT_BY_INLINE_CONDITION)
        flowName?.let { firstMatchFlow.setName(it) }

        model.cases.forEachIndexed { i, case ->
            val displayRule = displayRuleRepository.findModelOrFail(case.displayRuleRef.id)
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

            firstMatchFlow.addLineForSelectByInlineCondition(
                displayRule.definition.toScript(layout, variableStructure, variableRepository::findModelOrFail),
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
        variableStructure: VariableStructureModel,
        model: SelectByLanguageModel,
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
            val contentFlow =
                caseFlows[language] ?: layout.addFlow().setType(Flow.Type.SIMPLE).setDisplayName("Case $language")
            languageFlow.addLineForSelectByInlineCondition(language, contentFlow)

            if (language == defaultLanguage) {
                defaultLanguageFlow = contentFlow
            }
        }

        languageFlow.setDefaultFlow(defaultLanguageFlow ?: layout.addFlow().setType(Flow.Type.SIMPLE))

        return languageFlow
    }

    protected fun List<DocumentContentModel>.paragraphIfEmpty(): List<DocumentContentModel> {
        return this.ifEmpty {
            listOf(
                ParagraphModel(
                    listOf(TextModel(listOf(), null, null)),
                    null,
                    null
                )
            )
        }
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

fun DisplayRuleDefinition.toScript(
    layout: Layout, variableStructure: VariableStructureModel, findVar: (String) -> VariableModel
): String {
    return "return ${this.group.toScript(layout, variableStructure, findVar)};"
}

fun Group.toScript(
    layout: Layout, variableStructure: VariableStructureModel, findVar: (String) -> VariableModel
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
    layout: Layout, variableStructure: VariableStructureModel, findVar: (String) -> VariableModel
): String {
    val leftScriptResult = left.toScript(layout, variableStructure, findVar)
    val rightScriptResult = right.toScript(layout, variableStructure, findVar)

    val binary = operator.toScript(leftScriptResult, rightScriptResult)
    return if (leftScriptResult is ScriptResult.Success && rightScriptResult is ScriptResult.Success) {
        binary
    } else {
        BinOp.Equals.toScript(
            ScriptResult.Success("String('${binary.replace("'", "")}')"), ScriptResult.Success("String('unmapped')")
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
    layout: Layout, variableStructure: VariableStructureModel, findVar: (String) -> VariableModel
): ScriptResult {
    return when (this) {
        is Literal -> this.toScript(layout, variableStructure, findVar)
        is Function -> this.toScript(layout, variableStructure, findVar)
    }
}

fun Function.toScript(
    layout: Layout, variableStructure: VariableStructureModel, findVar: (String) -> VariableModel
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
    layout: Layout, variableStructure: VariableStructureModel, findVar: (String) -> VariableModel
): ScriptResult {
    return when (dataType) {
        LiteralDataType.Variable -> variableToScript(value, layout, variableStructure, findVar)
        LiteralDataType.String -> ScriptResult.Success(
            "String('${
                value.replace("\\", "\\\\").replace("\"", "\\\"")
            }')"
        )

        LiteralDataType.Number -> ScriptResult.Success(value)
        LiteralDataType.Boolean -> ScriptResult.Success(value.lowercase().toBooleanStrict().toString())
    }
}

fun variableToScript(
    id: String, layout: Layout, variableStructure: VariableStructureModel, findVar: (String) -> VariableModel
): ScriptResult {
    val variableModel = findVar(id)
    val variablePathData = variableStructure.structure[VariableModelRef(id)]
    return if (variablePathData == null) {
        ScriptResult.Failure(variableModel.nameOrId())
    } else {
        val variableName = variablePathData.name ?: variableModel.nameOrId()

        getOrCreateVariable(layout.data, variableName, variableModel, variablePathData.path)

        ScriptResult.Success((variablePathData.path.split(".") + variableName).joinToString(".") { pathPart ->
            when (pathPart.lowercase()) {
                "value" -> "Current"
                "data" -> "DATA"
                else -> sanitizeVariablePart(if (pathPart.first().isDigit()) "_$pathPart" else pathPart)
            }
        })
    }
}

fun getOrCreateVariable(
    data: Data, variableName: String, variableModel: VariableModel, variablePath: String
): Variable {
    val variable = getVariable(data as DataImpl, variableName, variablePath)
    return variable ?: data.addVariable().setName(variableName).setKind(VariableKind.DISCONNECTED)
        .setDataType(getDataType(variableModel.dataType)).setExistingParentId(variablePath)
        .setValueIfAvailable(variableModel)
}

fun Variable.setValueIfAvailable(variableModel: VariableModel): Variable {
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