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
import com.quadient.wfdxml.api.layoutnodes.SheetNameType
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind
import com.quadient.wfdxml.api.module.Layout
import com.quadient.wfdxml.internal.data.WorkFlowTreeDefinition
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeOptionality
import com.quadient.wfdxml.internal.layoutnodes.data.WorkFlowTreeEnums.NodeType.SUB_TREE
import kotlin.time.Clock

class InspireVariableStructureBuilder(
    private val variableRepository: VariableRepository,
    private val variableStructureRepository: VariableStructureRepository,
    private val projectConfig: ProjectConfig,
) {
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

        val normalizedVariablePaths = variableStructureModel.structure.map { (variableId, variablePathData) ->
            val literalPath = variablePathData.path.resolve(variableStructureModel, variableRepository::findOrFail)
                ?: error("Variable '$variableId' referenced as array path has no resolvable literal path in structure")
            removeDataFromVariablePath(literalPath)
        }.filter { it.isNotBlank() }.filter { it != "SystemVariable" && !it.startsWith("SystemVariable.") }

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
