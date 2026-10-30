package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.CustomFieldMap
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.EmailOptions
import com.quadient.migration.api.dto.migrationmodel.SmsOptions
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.shared.DataType
import com.quadient.wfdxml.api.layoutnodes.SheetNameType
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind
import com.quadient.wfdxml.api.module.Layout
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.internal.data.WorkFlowTreeDefinition
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType
import kotlin.time.Clock

class InspireVariableStructureBuilder(
    private val variableRepository: VariableRepository,
    private val variableStructureRepository: VariableStructureRepository,
    private val projectConfig: ProjectConfig,
) {
    fun buildDataDefinition(variableStructure: VariableStructure): String {
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        initVariableStructure(layout, variableStructure, includeLeaves = true)
        return builder.buildDataDefinitionLayoutDelta()
    }

    fun findVariableStructure(variableStructureId: String?): VariableStructure? {
        return (variableStructureId ?: projectConfig.defaultVariableStructure)?.let { variableStructureRepository.find(it) }
    }

    fun initVariableStructure(layout: Layout, variableStructureId: String?): VariableStructure {
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

        initVariableStructure(layout, variableStructureModel)
        return variableStructureModel
    }

    private fun initVariableStructure(
        layout: Layout, variableStructure: VariableStructure, includeLeaves: Boolean = false,
    ) {
        val paths = variableStructure.structure.map { (id, pathData) ->
            val path = pathData.path.resolve(variableStructure, variableRepository::findOrFail)
                ?: error("Variable '$id' has no resolvable path in structure '${variableStructure.id}'")
            if (includeLeaves) {
                require(path.isNotBlank()) {
                    "Variable '$id' has no resolvable path in structure '${variableStructure.id}'"
                }
                val variable = variableRepository.findOrFail(id)
                val name = pathData.name ?: variable.nameOrId()
                removeDataFromVariablePath("$path.$name") to variable.dataType
            } else {
                removeDataFromVariablePath(path) to null
            }
        }.filter { (path, _) -> path.isNotBlank() && !isSystemPath(path) }.toMap()

        val definition = WorkFlowTreeDefinition("Root", NodeType.SUB_TREE, NodeOptionality.ARRAY)
        val variableTypes = paths.mapNotNull { (path, type) -> type?.let { path to it } }.toMap()
        buildVariablePathPart(definition, buildVariableTree(paths.keys.toList(), variableTypes))

        layout.data.importDataDefinition(definition)
        val firstNode = definition.subNodes.firstOrNull()
        if (firstNode?.nodeOptionality == NodeOptionality.ARRAY) {
            layout.data.setRepeatedBy("Data.${firstNode.caption}")
        }
    }

    private fun buildVariablePathPart(
        parent: WorkFlowTreeDefinition, tree: Map<String, VariablePathPart>,
    ) {
        for (part in tree.values) {
            val optionality = if (part is ArrayVariable) NodeOptionality.ARRAY else NodeOptionality.MUST_EXIST
            val node = WorkFlowTreeDefinition(part.name, part.dataType.toNodeType(), optionality)
            parent.addSubNode(node)
            buildVariablePathPart(node, part.children)
        }
    }

    private fun isSystemPath(path: String) = path == "SystemVariable" || path.startsWith("SystemVariable.")

    private fun DataType.toNodeType(): NodeType = when (this) {
        DataType.String -> NodeType.STRING
        DataType.Integer -> NodeType.INT
        DataType.Integer64 -> NodeType.INT64
        DataType.Double -> NodeType.DOUBLE
        DataType.Currency -> NodeType.CURRENCY
        DataType.Boolean -> NodeType.BOOL
        DataType.DateTime -> NodeType.DATETIME
        DataType.Array, DataType.SubTree -> NodeType.SUB_TREE
    }

    fun addEmailMetadataToPages(layout: Layout, documentObject: DocumentObject, variableStructure: VariableStructure) =
        addEmailMetadataToPages(layout, documentObject.options as? EmailOptions, variableStructure)

    fun addEmailMetadataToPages(layout: Layout, emailOptions: EmailOptions?, variableStructure: VariableStructure) {
        addSheetNameVariable(
            layout, variableStructure, SheetNameType.EMAIL_FROM, "EmailFrom", emailOptions?.from, emitEmpty = true
        )
        addSheetNameVariable(
            layout,
            variableStructure,
            SheetNameType.EMAIL_FROM_NAME,
            "EmailFromName",
            emailOptions?.fromName,
            emitEmpty = true,
        )
        addSheetNameVariable(
            layout,
            variableStructure,
            SheetNameType.EMAIL_SUBJECT,
            "EmailSubject",
            emailOptions?.subject,
            emitEmpty = true,
        )
        addSheetNameVariable(
            layout, variableStructure, SheetNameType.EMAIL_TO, "EmailTo", emailOptions?.to, emitEmpty = true
        )
    }

    fun addSmsNumberToPages(layout: Layout, documentObject: DocumentObject, variableStructure: VariableStructure) =
        addSmsNumberToPages(layout, documentObject.options as? SmsOptions, variableStructure)

    fun addSmsNumberToPages(layout: Layout, smsOptions: SmsOptions?, variableStructure: VariableStructure) {
        addSheetNameVariable(
            layout,
            variableStructure,
            SheetNameType.SMS_NUMBER_TO,
            "NumberTo",
            smsOptions?.numberTo,
            emitEmpty = true,
        )
    }

    fun addPdfMetadataToPages(layout: Layout, documentObject: DocumentObject, variableStructure: VariableStructure) {
        val pdfMetadata = documentObject.pdfMetadata ?: return
        addSheetNameVariable(layout, variableStructure, SheetNameType.PDF_TITLE, "TaggingTitle", pdfMetadata.title)
        addSheetNameVariable(layout, variableStructure, SheetNameType.PDF_AUTHOR, "TaggingAuthor", pdfMetadata.author)
        addSheetNameVariable(
            layout, variableStructure, SheetNameType.PDF_SUBJECT, "TaggingSubject", pdfMetadata.subject
        )
        addSheetNameVariable(
            layout, variableStructure, SheetNameType.PDF_KEYWORDS, "TaggingKeywords", pdfMetadata.keywords
        )
        addSheetNameVariable(
            layout, variableStructure, SheetNameType.PDF_PRODUCER, "TaggingProduce", pdfMetadata.producer
        )
    }

    private fun addSheetNameVariable(
        layout: Layout,
        variableStructure: VariableStructure,
        type: SheetNameType,
        variableName: String,
        value: List<VariableStringContent>?,
        emitEmpty: Boolean = false,
    ) {
        if (value.isNullOrEmpty() && !emitEmpty) return

        val script = if (value.isNullOrEmpty()) {
            "return \"\";"
        } else {
            variableStringContentToScript(value, layout, variableStructure, variableRepository::findOrFail)
        }

        val variable = layout.data
            .addVariable()
            .setName(variableName)
            .setKind(VariableKind.CALCULATED)
            .setScript(script)
        layout.pages.addSheetName(type, variable)
    }
}
