package com.quadient.migration.tools.model

import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.File
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.Paragraph.Text
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefOrRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.SelectByLanguage
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.Tabs
import com.quadient.migration.api.dto.migrationmodel.TextContent
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefOrRef
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.BinOp
import com.quadient.migration.shared.Binary
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.FileType
import com.quadient.migration.shared.Group
import com.quadient.migration.shared.GroupOp
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.LiteralOrFunctionCall
import com.quadient.migration.shared.MetadataPrimitive
import com.quadient.migration.shared.ParagraphPdfTaggingRule
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SkipOptions
import com.quadient.migration.shared.SuperOrSubscript
import com.quadient.migration.shared.VariablePathData
import kotlinx.datetime.Instant
import kotlin.collections.emptyMap

fun aDocObj(
    id: String,
    type: DocumentObjectType = DocumentObjectType.Block,
    content: List<DocumentContent> = emptyList(),
    internal: Boolean = false,
    name: String? = id + "name",
    targetFolder: String? = null,
    options: DocumentObjectOptions? = null,
    displayRuleRef: String? = null,
    baseTemplate: String? = null,
    VariableStructureRef: String? = null,
    metadata: Map<String, List<MetadataPrimitive>> = emptyMap(),
): DocumentObject {
    return DocumentObject(
        id = id,
        name = name,
        type = type,
        content = content,
        internal = internal,
        targetFolder = targetFolder?.let { IcmPath.from(it).toString() },
        originLocations = emptyList(),
        customFields = CustomFieldMap(),
        created = null,
        lastUpdated = null,
        displayRuleRef = displayRuleRef?.let { DisplayRuleRef(it) },
        baseTemplate = baseTemplate,
        variableStructureRef = VariableStructureRef?.let { VariableStructureRef(it) },
        options = options,
        metadata = metadata,
        skip = SkipOptions(false, null, null),
        subject = null,
    )
}

fun aBlock(
    id: String = "block",
    content: List<DocumentContent> = emptyList(),
    name: String? = null,
    internal: Boolean = false,
    targetFolder: String? = null,
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    created: Instant? = null,
    lastUpdated: Instant? = null,
    type: DocumentObjectType = DocumentObjectType.Block,
    displayRuleRef: DisplayRuleRef? = null,
    baseTemplate: String? = null,
    metadata : Map<String, List<MetadataPrimitive>> = emptyMap(),
    skip: SkipOptions = SkipOptions(false, null, null),
): DocumentObject {
    return DocumentObject(
        id = id,
        name = name,
        type = type,
        content = content,
        internal = internal,
        targetFolder = targetFolder,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
        created = created,
        lastUpdated = lastUpdated,
        displayRuleRef = displayRuleRef,
        baseTemplate = baseTemplate,
        options = null,
        metadata = metadata,
        skip = skip,
        subject = null,
    )
}

fun anArea(
    content: List<DocumentContent>,
    position: Position? = null,
    interactiveFlowName: String? = null,
): Area {
    return Area(content, position, interactiveFlowName)
}

fun aTemplate(
    id: String,
    content: List<DocumentContent>,
    baseTemplate: String? = null,
): DocumentObject {
    return DocumentObject(
        id = id,
        name = null,
        type = DocumentObjectType.Template,
        content = content,
        internal = false,
        targetFolder = null,
        originLocations = emptyList(),
        customFields = CustomFieldMap(),
        created = null,
        lastUpdated = null,
        baseTemplate = baseTemplate,
        options = null,
        metadata = emptyMap(),
        skip = SkipOptions(false, null, null),
        subject = null,
    )
}

fun aVariable(
    id: String,
    name: String = "variable$id",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    dataType: DataType = DataType.String,
    defaultValue: String? = null
) = Variable(
    id = id,
    name = name,
    originLocations = originLocations,
    customFields = CustomFieldMap(customFields),
    dataType = dataType,
    defaultValue = defaultValue,
    lastUpdated = null,
    created = null,
)

fun aParaStyle(
    id: String,
    name: String? = "style$id",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    definition: ParagraphStyleDefOrRef = aParaDef()
): ParagraphStyle {
    return ParagraphStyle(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
        definition = definition,
        lastUpdated = null,
        created = null,
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
    tabs: Tabs? = null,
    pdfTaggingRule: ParagraphPdfTaggingRule? = null,
): ParagraphStyleDefinition {
    return ParagraphStyleDefinition(
        leftIndent = leftIndent,
        rightIndent = rightIndent,
        defaultTabSize = defaultTabSize,
        spaceBefore = spaceBefore,
        spaceAfter = spaceAfter,
        alignment = alignment,
        firstLineIndent = firstLineIndent,
        lineSpacing = lineSpacing,
        keepWithNextParagraph = keepWithNextParagraph,
        tabs = tabs,
        pdfTaggingRule = pdfTaggingRule,
    )
}

fun aTextStyle(
    id: String,
    name: String? = "style$id",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    definition: TextStyleDefOrRef = aTextDef(),
): TextStyle {
    return TextStyle(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
        definition = definition,
        lastUpdated = null,
        created = null,
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
): TextStyleDefinition {
    return TextStyleDefinition(
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

fun aVariableStructure(
    id: String = "variableStructure",
    name: String? = "variableStructureName",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    structure: Map<String, VariablePathData> = emptyMap(),
    languageVariable: String? = null,
    lastUpdated: Instant? = null,
): VariableStructure {
    return VariableStructure(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
        lastUpdated = lastUpdated,
        created = null,
        structure = structure,
        languageVariable = languageVariable?.let { VariableRef(it) }
    )
}

fun aDisplayRule(
    id: String,
    name: String? = "Rule$id",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    definition: DisplayRuleDefinition? = null
): DisplayRule {
    return DisplayRule(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
        lastUpdated = null,
        created = null,
        definition = definition
    )
}

fun aDisplayRule(
    left: LiteralOrFunctionCall,
    operator: BinOp,
    right: LiteralOrFunctionCall,
    groupOp: GroupOp = GroupOp.And,
    negation: Boolean = false,
    id: String = "R_1",
): DisplayRule {
    return aDisplayRule(
        id, definition = DisplayRuleDefinition(
            Group(
                listOf(Binary(left, operator, right)), groupOp, negation
            )
        )
    )
}

fun aParagraph(
    content: List<Text> = listOf<Text>(),
    styleRef: ParagraphStyleRef? = null,
    displayRuleRef: DisplayRuleRef? = null
): Paragraph = Paragraph(content, styleRef, displayRuleRef)

fun aParagraph(
    content: Text, styleRef: String? = null, displayRuleRef: DisplayRuleRef? = null
): Paragraph = Paragraph(listOf(content), styleRef?.let { ParagraphStyleRef(it) }, displayRuleRef)

fun aParagraph(string: String): Paragraph = aParagraph(aText(string))

fun aText(string: String): Text = Text(listOf(StringValue(string)), null, null)

fun aText(
    content: List<TextContent> = listOf<TextContent>(),
    styleRef: TextStyleRef? = null,
    displayRuleRef: DisplayRuleRef? = null
): Text = Text(content, styleRef, displayRuleRef)

fun aText(
    content: TextContent, styleRef: String? = null, displayRuleRef: DisplayRuleRef? = null
): Text = Text(listOf(content), styleRef?.let { TextStyleRef(it) }, displayRuleRef)

fun aRow(
    cells: List<Table.Cell>, displayRuleRef: String? = null
): Table.Row {
    return Table.Row(cells, displayRuleRef?.let { DisplayRuleRef(it) })
}

fun aCell(content: DocumentContent, mergeLeft: Boolean = false, mergeUp: Boolean = false): Table.Cell {
    return Table.Cell(listOf(content), mergeLeft, mergeUp)
}

fun aSelectByLanguage(
    cases: Map<String, List<DocumentContent>> = emptyMap()
): SelectByLanguage {
    return SelectByLanguage(
        cases = cases.entries.map { SelectByLanguage.Case(it.value, it.key) }
    )
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
    metadata : Map<String, List<MetadataPrimitive>> = emptyMap(),
    skip : SkipOptions = SkipOptions(false, null, null),
    alternateText: String? = null,
): Image {
    return Image(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
        created = null,
        lastUpdated = null,
        sourcePath = sourcePath,
        imageType = imageType,
        options = options,
        targetFolder = targetFolder,
        metadata = metadata,
        skip = skip,
        alternateText = alternateText,
    )
}

fun aFile(
    id: String,
    name: String = "File_$id",
    sourcePath: String? = "$name.pdf",
    fileType: FileType = FileType.Document,
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    targetFolder: String? = null,
    skip: SkipOptions = SkipOptions(false, null, null),
): File {
    return File(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
        created = null,
        lastUpdated = null,
        sourcePath = sourcePath,
        fileType = fileType,
        targetFolder = targetFolder,
        skip = skip,
    )
}

fun aDocumentObjectRef(id: String, displayRuleId: String? = null) =
    DocumentObjectRef(id, displayRuleId?.let { DisplayRuleRef(it) })
