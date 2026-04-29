package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.FirstMatch
import com.quadient.migration.api.dto.migrationmodel.StringValue
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStringContent
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.ScriptResult
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.ScriptResult.Failure
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder.ScriptResult.Success
import com.quadient.migration.shared.IcmPath
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.data.DataType
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind
import com.quadient.wfdxml.api.module.Layout

class InteractiveSnippetBuilder(
    private val mainFlowId: String,
    private val variableRepository: VariableRepository,
    private val displayRuleRepository: DisplayRuleRepository,
    private val interactiveTenant: String,
    private val getDisplayRulePath: (DisplayRule) -> IcmPath,
) {
    fun buildSnippet(
        documentObject: DocumentObject,
        wfdXmlBuilder: WfdXmlBuilder,
        layout: Layout,
        variableStructure: VariableStructure
    ): String {
        return if (documentObject.content.first() is FirstMatch) {
            wfdXmlBuilder.buildFirstMatchSnippet(documentObject, layout, variableStructure)
        } else if (documentObject.content.all { it is VariableStringContent }) {
            wfdXmlBuilder.buildSimpleSnippet(documentObject, layout, variableStructure)
        } else {
            error("Snippet '${documentObject.nameOrId()}' has invalid content. Snippets must contain either only variable string content or a first match with only variable string content.")
        }
    }

    private fun variableStringContentToVff(
        variableStringContent: List<VariableStringContent>,
        layout: Layout,
        variableStructure: VariableStructure,
    ): String {
        var wasLastItemVariable = false
        val scriptParts = variableStringContent.map {
            when (it) {
                is StringValue -> {
                    wasLastItemVariable = false
                    toScriptStringLiteral(it.value)
                }

                is VariableRef -> {
                    when (val variableScript = variableToScript(it.id, layout, variableStructure)) {
                        is Success -> {
                            if (wasLastItemVariable) {
                                wasLastItemVariable = true
                                "'' + '<var name=\"$variableScript\">'"
                            } else {
                                wasLastItemVariable = true
                                "'<var name=\"$variableScript\">'"
                            }
                        }

                        is Failure -> toScriptStringLiteral("$${variableScript.variableName}$")
                    }
                }
            }
        }

        return "return ${scriptParts.joinToString(" + ")};"
    }

    private fun variableToScript(id: String, layout: Layout?, variableStructure: VariableStructure): ScriptResult {
        val variableModel = variableRepository.findOrFail(id)
        val variablePathData = variableStructure.structure[id]
        val resolvedPath = variablePathData?.path?.resolve(variableStructure, variableRepository::findOrFail)
            ?.takeIf { it.isNotBlank() }
        return if (resolvedPath.isNullOrBlank()) {
            Failure(variablePathData?.name ?: variableModel.nameOrId())
        } else {
            val variableName = variablePathData.name ?: variableModel.nameOrId()

            if (layout != null) {
                getOrCreateVariable(layout.data, variableName, variableModel, resolvedPath)
            }

            Success((resolvedPath.split(".") + variableName).filter { it.lowercase() != "data" }
                .joinToString(".") { pathPart ->
                    when (pathPart.lowercase()) {
                        "value" -> "Value"
                        "data" -> "DATA"
                        else -> sanitizeVariablePart(if (pathPart.first().isDigit()) "_$pathPart" else pathPart)
                    }
                })
        }
    }


    private fun WfdXmlBuilder.buildSimpleSnippet(
        documentObject: DocumentObject, layout: Layout, variableStructure: VariableStructure
    ): String {
        val content = documentObject.content.filterIsInstance<VariableStringContent>()
        val script = variableStringContentToVff(content, layout, variableStructure)

        val variable = layout.data.addVariable().setName("Variable for snippet '${documentObject.nameOrId()}'")
            .setKind(VariableKind.CALCULATED).setDataType(DataType.STRING).setScript(script)

        layout.addFlow().setId(mainFlowId).setType(Flow.Type.OVERFLOWABLE_VARIABLE_FORMATTED).setSectionFlow(false)
            .setVariable(variable)

        return this.buildLayoutDelta()
    }

    private fun WfdXmlBuilder.buildFirstMatchSnippet(
        documentObject: DocumentObject, layout: Layout, variableStructure: VariableStructure
    ): String {
        val fm = documentObject.content.first() as FirstMatch
        val firstMatchFlow = layout
            .addFlow()
            .setId(mainFlowId)
            .setType(Flow.Type.SELECT_BY_INLINE_CONDITION)
            .setSectionFlow(false)
        firstMatchFlow.addParagraph().addText()

        fm.cases.forEachIndexed { i, case ->
            if (!case.content.all { it is VariableStringContent }) {
                error("Invalid content in case ${i + 1} of first match in snippet '${documentObject.nameOrId()}'. All content in first match cases must be variable string content.")
            }

            val displayRule = displayRuleRepository.findOrFail(case.displayRuleRef.id)
            if (displayRule.definition == null) {
                error("Display rule '${case.displayRuleRef.id}' definition is null.")
            }

            val caseName = case.name ?: "Case ${i + 1}"

            val content = case.content.filterIsInstance<VariableStringContent>()
            val script = variableStringContentToVff(content, layout, variableStructure)
            val variable = layout.data.addVariable()
                .setName("Variable for snippet '${documentObject.nameOrId()}' + case '$caseName'")
                .setKind(VariableKind.CALCULATED).setDataType(DataType.STRING).setScript(script)

            val flow = layout.addFlow().setType(Flow.Type.OVERFLOWABLE_VARIABLE_FORMATTED).setSectionFlow(false)
                .setVariable(variable)

            firstMatchFlow.addLineForSelectByInlineCondition(
                displayRule.toScript(
                    layout,
                    variableStructure,
                    variableRepository::findOrFail,
                    displayRuleRepository::findOrFail,
                    getDisplayRulePath,
                    InspireOutput.Interactive,
                    interactiveTenant
                ), flow
            )
        }

        if (fm.default.isNotEmpty()) {
            if (!fm.default.all { it is VariableStringContent }) {
                error("Invalid content in default case of first match in snippet '${documentObject.nameOrId()}'. All content in first match cases must be variable string content.")
            }

            val content = fm.default.filterIsInstance<VariableStringContent>()
            val script = variableStringContentToVff(content, layout, variableStructure)
            val variable =
                layout.data.addVariable().setName("Variable for snippet '${documentObject.nameOrId()}' + default case")
                    .setKind(VariableKind.CALCULATED).setDataType(DataType.STRING).setScript(script)

            val flow = layout.addFlow().setType(Flow.Type.OVERFLOWABLE_VARIABLE_FORMATTED).setSectionFlow(false)
                .setVariable(variable)

            firstMatchFlow.setDefaultFlow(flow)
        }

        return this.buildLayoutDelta()
    }
}