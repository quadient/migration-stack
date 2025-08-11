package com.quadient.migration.tools.model

import com.quadient.migration.data.AreaModel
import com.quadient.migration.data.DisplayRuleModel
import com.quadient.migration.data.DisplayRuleModelRef
import com.quadient.migration.data.DocumentContentModel
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.ImageModel
import com.quadient.migration.data.ParagraphModel
import com.quadient.migration.data.ParagraphModel.TextModel
import com.quadient.migration.data.ParagraphStyleDefOrRefModel
import com.quadient.migration.data.ParagraphStyleDefinitionModel
import com.quadient.migration.data.ParagraphStyleModel
import com.quadient.migration.data.ParagraphStyleModelRef
import com.quadient.migration.data.StringModel
import com.quadient.migration.data.TableModel
import com.quadient.migration.data.TabsModel
import com.quadient.migration.data.TextContentModel
import com.quadient.migration.data.TextStyleDefOrRefModel
import com.quadient.migration.data.TextStyleDefinitionModel
import com.quadient.migration.data.TextStyleModel
import com.quadient.migration.data.TextStyleModelRef
import com.quadient.migration.data.VariableModel
import com.quadient.migration.data.VariableModelRef
import com.quadient.migration.data.VariablePath
import com.quadient.migration.data.VariableStructureModel
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository
import com.quadient.migration.persistence.table.DisplayRuleTable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ImageTable
import com.quadient.migration.persistence.table.ParagraphStyleTable
import com.quadient.migration.persistence.table.TextStyleTable
import com.quadient.migration.persistence.table.VariableStructureTable
import com.quadient.migration.persistence.table.VariableTable
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Binary
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.Group
import com.quadient.migration.shared.GroupOp
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Literal
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript
import com.quadient.migration.tools.aProjectConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun aDocObj(
    id: String,
    type: DocumentObjectType = DocumentObjectType.Block,
    content: List<DocumentContentModel> = emptyList(),
    internal: Boolean = false,
    name: String? = id + "name",
    targetFolder: String? = null,
    options: DocumentObjectOptions? = null,
    displayRuleRef: String? = null,
    baseTemplate: String? = null,
): DocumentObjectModel {
    return DocumentObjectModel(
        id = id,
        name = name,
        type = type,
        content = content,
        internal = internal,
        targetFolder = targetFolder?.let(IcmPath::from),
        originLocations = emptyList(),
        customFields = emptyMap<String, String>(),
        created = Clock.System.now(),
        lastUpdated = Clock.System.now(),
        displayRuleRef = displayRuleRef?.let { DisplayRuleModelRef(it) },
        baseTemplate = baseTemplate,
        options = options,
    )
}

fun aBlock(
    id: String = "block",
    content: List<DocumentContentModel> = emptyList(),
    name: String? = null,
    internal: Boolean = false,
    targetFolder: String? = null,
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    created: Instant = Clock.System.now(),
    lastUpdated: Instant = Clock.System.now(),
    type: DocumentObjectType = DocumentObjectType.Block,
    displayRuleRef: DisplayRuleModelRef? = null,
    baseTemplate: String? = null,
): DocumentObjectModel {
    return DocumentObjectModel(
        id = id,
        name = name,
        type = type,
        content = content,
        internal = internal,
        targetFolder = targetFolder?.let(IcmPath::from),
        originLocations = originLocations,
        customFields = customFields,
        created = created,
        lastUpdated = lastUpdated,
        displayRuleRef = displayRuleRef,
        baseTemplate = baseTemplate,
        options = null,
    )
}

fun anArea(
    content: List<DocumentContentModel>,
    position: Position? = null,
    interactiveFlowName: String? = null,
): AreaModel {
    return AreaModel(content, position, interactiveFlowName)
}

fun aTemplate(
    id: String,
    content: List<DocumentContentModel>,
    baseTemplate: String? = null,
): DocumentObjectModel {
    return DocumentObjectModel(
        id = id,
        name = null,
        type = DocumentObjectType.Template,
        content = content,
        internal = false,
        targetFolder = null,
        originLocations = emptyList(),
        customFields = emptyMap(),
        created = Clock.System.now(),
        lastUpdated = Clock.System.now(),
        baseTemplate = baseTemplate,
        options = null,
    )
}

fun aVariable(
    id: String,
    name: String = "variable$id",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    dataType: DataType = DataType.String,
    defaultValue: String? = null
) = VariableModel(
    id = id,
    name = name,
    originLocations = originLocations,
    customFields = customFields,
    dataType = dataType,
    defaultValue = defaultValue,
    lastUpdated = Clock.System.now(),
    created = Clock.System.now(),
)

fun aParaStyle(
    id: String,
    name: String? = "style$id",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    definition: ParagraphStyleDefOrRefModel = aParaDef()
): ParagraphStyleModel {
    return ParagraphStyleModel(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = customFields,
        definition = definition,
        lastUpdated = Clock.System.now(),
        created = Clock.System.now(),
    )
}

fun aParaDef(
    leftIndent: Size? = null,
    rightIndent: Size? = null,
    defaultTabSize: Size? = null,
    spaceBefore: Size? = null,
    spaceAfter: Size? = null,
    alignment: Alignment = Alignment.Left,
    firstLineIndent: Size? = null,
    lineSpacing: LineSpacing = LineSpacing.Additional(null),
    keepWithNextParagraph: Boolean? = null,
    tabs: TabsModel? = null
): ParagraphStyleDefinitionModel {
    return ParagraphStyleDefinitionModel(
        leftIndent = leftIndent,
        rightIndent = rightIndent,
        defaultTabSize = defaultTabSize,
        spaceBefore = spaceBefore,
        spaceAfter = spaceAfter,
        alignment = alignment,
        firstLineIndent = firstLineIndent,
        lineSpacing = lineSpacing,
        keepWithNextParagraph = keepWithNextParagraph,
        tabs = tabs
    )
}

fun aTextStyle(
    id: String,
    name: String? = "style$id",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    definition: TextStyleDefOrRefModel = aTextDef(),
): TextStyleModel {
    return TextStyleModel(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = customFields,
        definition = definition,
        lastUpdated = Clock.System.now(),
        created = Clock.System.now(),
    )
}

fun aTextDef(
    fontFamily: String? = null,
    foregroundColor: Color? = null,
    size: Size? = null,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    strikethrough: Boolean = false,
    superOrSubscript: SuperOrSubscript = SuperOrSubscript.None,
    interspacing: Size? = null,
): TextStyleDefinitionModel {
    return TextStyleDefinitionModel(
        fontFamily = fontFamily,
        foregroundColor = foregroundColor,
        size = size,
        bold = bold,
        italic = italic,
        underline = underline,
        strikethrough = strikethrough,
        superOrSubscript = superOrSubscript,
        interspacing = interspacing
    )
}

fun aVariableStructureModel(
    id: String = "variableStructure",
    name: String? = "variableStructureName",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    structure: Map<VariableModelRef, VariablePath> = emptyMap(),
): VariableStructureModel {
    return VariableStructureModel(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = customFields,
        lastUpdated = Clock.System.now(),
        created = Clock.System.now(),
        structure = structure
    )
}

fun aDisplayRule(
    id: String,
    name: String? = "Rule$id",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    definition: DisplayRuleDefinition? = null
): DisplayRuleModel {
    return DisplayRuleModel(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = customFields,
        lastUpdated = Clock.System.now(),
        created = Clock.System.now(),
        definition = definition
    )
}

fun aDisplayRule(
    left: Literal,
    operator: BinOp,
    right: Literal,
    groupOp: GroupOp = GroupOp.And,
    negation: Boolean = false,
    id: String = "R_1",
): DisplayRuleModel {
    return aDisplayRule(
        id, definition = DisplayRuleDefinition(
            Group(
                listOf(Binary(left, operator, right)), groupOp, negation
            )
        )
    )
}

fun aParagraph(
    content: List<TextModel> = listOf<TextModel>(),
    styleRef: ParagraphStyleModelRef? = null,
    displayRuleRef: DisplayRuleModelRef? = null
): ParagraphModel = ParagraphModel(content, styleRef, displayRuleRef)

fun aParagraph(
    content: TextModel, styleRef: String? = null, displayRuleRef: DisplayRuleModelRef? = null
): ParagraphModel = ParagraphModel(listOf(content), styleRef?.let { ParagraphStyleModelRef(it) }, displayRuleRef)

fun aText(string: String): TextModel = TextModel(listOf(StringModel(string)), null, null)

fun aText(
    content: List<TextContentModel> = listOf<TextContentModel>(),
    styleRef: TextStyleModelRef? = null,
    displayRuleRef: DisplayRuleModelRef? = null
): TextModel = TextModel(content, styleRef, displayRuleRef)

fun aText(
    content: TextContentModel, styleRef: String? = null, displayRuleRef: DisplayRuleModelRef? = null
): TextModel = TextModel(listOf(content), styleRef?.let { TextStyleModelRef(it) }, displayRuleRef)

fun aRow(
    cells: List<TableModel.CellModel>, displayRuleRef: String? = null
): TableModel.RowModel {
    return TableModel.RowModel(cells, displayRuleRef?.let { DisplayRuleModelRef(it) })
}

fun aCell(content: DocumentContentModel, mergeLeft: Boolean = false, mergeUp: Boolean = false): TableModel.CellModel {
    return TableModel.CellModel(listOf(content), mergeLeft, mergeUp)
}

fun aImage(
    id: String,
    name: String = "Image_$id",
    sourcePath: String? = "$name.jpg",
    imageType: ImageType = ImageType.Jpeg,
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    options: ImageOptions? = null,
    targetFolder: String? = null,
): ImageModel {
    return ImageModel(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = customFields,
        created = Clock.System.now(),
        sourcePath = sourcePath,
        imageType = imageType,
        options = options,
        targetFolder = targetFolder?.let(IcmPath::from)
    )
}

fun aDocumentObjectRef(id: String, displayRuleId: String? = null) =
    DocumentObjectModelRef(id, displayRuleId?.let { DisplayRuleModelRef(it) })

fun aDocumentObjectInternalRepository() = DocumentObjectInternalRepository(DocumentObjectTable, aProjectConfig().name)
fun aVariableInternalRepository() = VariableInternalRepository(VariableTable, aProjectConfig().name)
fun aVariableStructureInternalRepository() =
    VariableStructureInternalRepository(VariableStructureTable, aProjectConfig().name)

fun aParaStyleInternalRepository() = ParagraphStyleInternalRepository(ParagraphStyleTable, aProjectConfig().name)
fun aTextStyleInternalRepository() = TextStyleInternalRepository(TextStyleTable, aProjectConfig().name)
fun aDisplayRuleInternalRepository() = DisplayRuleInternalRepository(DisplayRuleTable, aProjectConfig().name)
fun aImageInternalRepository() = ImageInternalRepository(ImageTable, aProjectConfig().name)