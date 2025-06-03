package com.quadient.migration.tools

import com.quadient.migration.api.DbConfig
import com.quadient.migration.api.InspireConfig
import com.quadient.migration.api.IpsConfig
import com.quadient.migration.api.MigConfig
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.Paragraph.Text
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefOrRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.Table
import com.quadient.migration.api.dto.migrationmodel.Tabs
import com.quadient.migration.api.dto.migrationmodel.TextContent
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefOrRef
import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.data.DocumentContentModel
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectOptions
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.LineSpacing
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.SuperOrSubscript
import com.quadient.migration.tools.model.aDisplayRuleInternalRepository
import com.quadient.migration.tools.model.aDocumentObjectInternalRepository
import com.quadient.migration.tools.model.aImageInternalRepository
import com.quadient.migration.tools.model.aParaStyleInternalRepository
import com.quadient.migration.tools.model.aTextStyleInternalRepository
import com.quadient.migration.tools.model.aVariableInternalRepository
import com.quadient.migration.tools.model.aVariableStructureInternalRepository
import kotlinx.datetime.Clock

fun aBlockDto(
    id: String,
    content: List<DocumentContent> = emptyList(),
    name: String? = null,
    internal: Boolean = false,
    targetFolder: String? = null,
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    type: DocumentObjectType = DocumentObjectType.Block,
): DocumentObject {
    return DocumentObject(
        id = id,
        content = content,
        name = name ?: "block$id",
        type = type,
        internal = internal,
        targetFolder = targetFolder,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
    )
}

fun aBlockModel(
    id: String,
    content: List<DocumentContentModel> = emptyList(),
    name: String? = null,
    internal: Boolean = false,
    targetFolder: String? = null,
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    type: DocumentObjectType = DocumentObjectType.Block,
    baseTemplate: String? = null,
    options: DocumentObjectOptions? = null,
): DocumentObjectModel {
    return DocumentObjectModel(
        id = id,
        content = content,
        name = name ?: "block$id",
        type = type,
        internal = internal,
        targetFolder = targetFolder,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
        created = Clock.System.now(),
        lastUpdated = Clock.System.now(),
        displayRuleRef = null,
        baseTemplate = baseTemplate,
        options = options
    )
}


fun aDbConfig(
    host: String = "host", port: Int = 5432, dbName: String = "db", user: String = "user", password: String = "pw"
): DbConfig {
    return DbConfig(
        host = host,
        port = port,
        dbName = dbName,
        user = user,
        password = password,
    )
}

fun aInspireConfig(ipsConfig: IpsConfig = IpsConfig(host = "localhost", port = 30354)): InspireConfig {
    return InspireConfig(ipsConfig = ipsConfig)
}

fun aProjectConfig(
    baseTemplatePath: String = "templatepath", interactiveTenant: String = "tenant", targetDefaultFolder: String? = null
): ProjectConfig {
    return ProjectConfig(
        name = "name",
        baseTemplatePath = baseTemplatePath,
        inputDataPath = "inputpath",
        interactiveTenant = interactiveTenant,
        defaultTargetFolder = targetDefaultFolder
    )
}

fun aMigConfig(
    dbConfig: DbConfig = aDbConfig(),
    inspireConfig: InspireConfig = aInspireConfig(),
): MigConfig {
    return MigConfig(dbConfig = dbConfig, inspireConfig = inspireConfig, storageRoot = "")
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
    defaultValue = defaultValue
)

fun aParagraph(
    styleRef: ParagraphStyleRef? = null, displayRuleRef: DisplayRuleRef? = null, content: List<Text> = listOf(aText())
): Paragraph {
    return Paragraph(styleRef = styleRef, displayRuleRef = displayRuleRef, content = content)
}

fun aText(
    styleRef: TextStyleRef? = null, content: List<TextContent> = listOf(StringValue("content")), displayRuleRef: DisplayRuleRef? = null,
): Text {
    return Text(styleRef = styleRef, displayRuleRef = displayRuleRef, content = content)
}

fun aTable(
    columnWidths: List<Table.ColumnWidth> = listOf(), rows: List<Table.Row> = listOf(aRow())
): Table {
    return Table(columnWidths = columnWidths, rows = rows)
}

fun aRow(
    cells: List<Table.Cell> = listOf(aCell())
): Table.Row {
    return Table.Row(cells = cells)
}

fun aCell(
    content: List<DocumentContent> = listOf(aParagraph()), mergeLeft: Boolean = false, mergeUp: Boolean = false
): Table.Cell {
    return Table.Cell(content = content, mergeLeft = mergeLeft, mergeUp = mergeUp)
}

fun aParagraphStyle(
    id: String,
    name: String? = "style$id",
    originLocations: List<String> = emptyList(),
    customFields: MutableMap<String, String> = mutableMapOf(),
    definition: ParagraphStyleDefOrRef = aParagraphStyleDefinition()
): ParagraphStyle {
    return ParagraphStyle(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
        definition = definition
    )
}

fun aParagraphStyleDefinition(
    leftIndent: Size? = null,
    rightIndent: Size? = null,
    defaultTabSize: Size? = null,
    spaceBefore: Size? = null,
    spaceAfter: Size? = null,
    alignment: Alignment = Alignment.Left,
    firstLineIndent: Size? = null,
    lineSpacingValue: Size? = null,
    lineSpacing: LineSpacing = LineSpacing.Additional,
    keepWithNextParagraph: Boolean? = null,
    tabs: Tabs? = null
): ParagraphStyleDefinition {
    return ParagraphStyleDefinition(
        leftIndent = leftIndent,
        rightIndent = rightIndent,
        defaultTabSize = defaultTabSize,
        spaceBefore = spaceBefore,
        spaceAfter = spaceAfter,
        alignment = alignment,
        firstLineIndent = firstLineIndent,
        lineSpacingValue = lineSpacingValue,
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
    definition: TextStyleDefOrRef = aTextStyleDefinition()
): TextStyle {
    return TextStyle(
        id = id,
        name = name,
        originLocations = originLocations,
        customFields = CustomFieldMap(customFields),
        definition = definition
    )
}

fun aTextStyleDefinition(
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

fun aDocumentObjectRepository() = DocumentObjectRepository(aDocumentObjectInternalRepository())
fun aVariableRepository() = VariableRepository(aVariableInternalRepository())
fun aVariableStructureRepository() = VariableStructureRepository(aVariableStructureInternalRepository())
fun aParaStyleRepository() = ParagraphStyleRepository(aParaStyleInternalRepository())
fun aTextStyleRepository() = TextStyleRepository(aTextStyleInternalRepository())
fun aDisplayRuleRepository() = DisplayRuleRepository(aDisplayRuleInternalRepository())
fun aImageRepository() = ImageRepository(aImageInternalRepository())