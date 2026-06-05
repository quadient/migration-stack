package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.Area
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentContent
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository
import com.quadient.migration.service.IcmDataCache
import com.quadient.migration.service.ResourcePathProvider
import com.quadient.migration.service.getBaseTemplateFullPath
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Flow.WebEditingType.SECTION
import com.quadient.wfdxml.api.layoutnodes.Image as WfdXmlImage
import com.quadient.wfdxml.api.layoutnodes.data.DataType
import com.quadient.wfdxml.api.layoutnodes.data.VariableKind
import com.quadient.wfdxml.api.module.Layout

class InteractiveDocumentObjectBuilder(
    documentObjectRepository: DocumentObjectRepository,
    textStyleRepository: TextStyleRepository,
    paragraphStyleRepository: ParagraphStyleRepository,
    variableRepository: VariableRepository,
    variableStructureRepository: VariableStructureRepository,
    displayRuleRepository: DisplayRuleRepository,
    imageRepository: ImageRepository,
    attachmentRepository: AttachmentRepository,
    projectConfig: ProjectConfig,
    resourcePathProvider: ResourcePathProvider,
    icmDataCache: IcmDataCache,
) : InspireDocumentObjectBuilder(
    documentObjectRepository,
    textStyleRepository,
    paragraphStyleRepository,
    variableRepository,
    variableStructureRepository,
    displayRuleRepository,
    imageRepository,
    attachmentRepository,
    projectConfig,
    resourcePathProvider,
    projectConfig.inspireOutput,
    icmDataCache,
) {
    private val mainFlowId = "Def.MainFlow"
    private val snippetBuilder = InteractiveSnippetBuilder(
        mainFlowId,
        variableRepository,
        displayRuleRepository,
        projectConfig.interactiveTenant,
        resourcePathProvider::getDisplayRulePath
    )

    override fun applyImageAlternateText(layout: Layout, image: WfdXmlImage, alternateText: String) {
        val variable = layout.data.addVariable()
            .setName("Alternate text variable for ${image.name}")
            .setKind(VariableKind.CALCULATED)
            .setDataType(DataType.STRING)
            .setScript("return ${toScriptStringLiteral(alternateText)};")
            .addCustomProperty("ValueWrapperVariable", true)
        val layoutRoot = layout.root ?: layout.addRoot()
        layoutRoot.addLockedWebNode(variable)

        image.setAlternateTextVariable(variable)
    }

    override fun buildDocumentObject(documentObject: DocumentObject): String {
        logger.debug("Starting to build document object '${documentObject.nameOrId()}'.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        if (documentObject.subject != null) {
            val root = layout.addRoot()
            root.setSubject(documentObject.subject)
        }


        val baseTemplatePath = getBaseTemplateFullPath(projectConfig, documentObject.baseTemplate)
        val currentBaseTemplateData = icmDataCache.getOrLoadBaseTemplateData(baseTemplatePath)
            ?: error("Unable to deploy document object ${documentObject.id}. Base template '$baseTemplatePath' does not exist.")

        val languages = collectLanguages(documentObject)
        val variableStructure = initVariableStructure(layout, documentObject.variableStructureRef?.id)

        addPdfMetadataToPages(layout, documentObject, variableStructure)

        val interactiveFlowsWithContent = mutableMapOf<String, MutableList<DocumentContent>>()
        when (documentObject.type) {
            DocumentObjectType.Snippet -> return snippetBuilder.buildSnippet(
                documentObject,
                builder,
                layout,
                variableStructure
            )

            else -> {
                documentObject.content.paragraphIfEmpty().forEach { documentContentPart ->
                    if (documentContentPart is DocumentObjectRef) {
                        val referencedModel = documentObjectRepository.findOrFail(documentContentPart.id)
                        if (referencedModel.type == DocumentObjectType.Page && referencedModel.internal == true) {
                            referencedModel.content.paragraphIfEmpty().forEach { pageContentPart ->
                                mapContentItemToInteractiveFlow(pageContentPart, currentBaseTemplateData, interactiveFlowsWithContent)
                            }
                        } else {
                            interactiveFlowsWithContent.getOrPut(mainFlowId) { mutableListOf() }.add(documentContentPart)
                        }
                    } else {
                        mapContentItemToInteractiveFlow(documentContentPart, currentBaseTemplateData, interactiveFlowsWithContent)
                    }
                }
            }
        }

        val hasMultipleFlows = interactiveFlowsWithContent.size > 1

        interactiveFlowsWithContent.forEach {
            val interactiveFlowText =
                layout.addFlow().setId(it.key).setType(Flow.Type.SIMPLE).setSectionFlow(true).setWebEditingType(SECTION)
                    .addParagraph().addText()

            val flowName = if (hasMultipleFlows && it.key != mainFlowId) {
                val interactiveFlowName =
                    currentBaseTemplateData.interactiveFlowNamesToIds.entries.firstOrNull { (_, id) -> id == it.key }?.key
                        ?: it.key
                "${documentObject.nameOrId()}_$interactiveFlowName"
            } else {
                documentObject.nameOrId()
            }

            val contentFlows = buildDocumentContentAsFlows(layout, variableStructure, it.value, flowName, languages)

            when (val ref = documentObject.displayRuleRef) {
                null -> contentFlows.forEach { contentFlow -> interactiveFlowText.appendFlow(contentFlow) }
                else -> interactiveFlowText.appendFlow(
                    contentFlows.toSingleFlow(layout, variableStructure, flowName, DisplayRuleRef(ref.id))
                )
            }
        }

        logger.debug("Successfully built document object '${documentObject.nameOrId()}'")
        return builder.buildLayoutDelta()
    }

    override fun buildDocumentObjectRef(
        documentModel: DocumentObject,
        layout: Layout,
        variableStructure: VariableStructure,
        documentObjectRef: DocumentObjectRef,
        languages: List<String>
    ): Flow? {
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
                .setLocation(resourcePathProvider.getDocumentObjectPath(documentModel).toString())
        }

        if (documentObjectRef.displayRuleRef != null) {
            val displayRule = displayRuleRepository.findOrFail(documentObjectRef.displayRuleRef.id)

            return wrapSuccessFlowInConditionFlow(layout, variableStructure, displayRule, flow)
        }

        return flow
    }

    override fun shouldIncludeInternalDependency(documentObject: DocumentObject): Boolean {
        return documentObject.internal == true
    }

    override fun resolveParagraphStyleName(name: String): String =
        icmDataCache.styleDefinitionData?.paragraphStyleDisplayNamesToNames?.get(name) ?: name

    override fun resolveTextStyleName(name: String): String =
        icmDataCache.styleDefinitionData?.textStyleDisplayNamesToNames?.get(name) ?: name

    override fun resolveTableStyleName(name: String): String =
        icmDataCache.styleDefinitionData?.tableStyleDisplayNamesToName?.get(name) ?: name

    private fun mapContentItemToInteractiveFlow(
        contentItem: DocumentContent,
        baseTemplateData: IcmDataCache.BaseTemplateData,
        interactiveFlowsWithContent: MutableMap<String, MutableList<DocumentContent>>,
    ) {
        if (contentItem is Area && !contentItem.interactiveFlowName.isNullOrBlank()) {
            val flowName = contentItem.interactiveFlowName!!
            val interactiveFlowId = if (flowName.startsWith("Def.")) {
                flowName
            } else {
                baseTemplateData.interactiveFlowNamesToIds[flowName]
            }

            if (interactiveFlowId.isNullOrBlank()) {
                val errorMessage = "Failed to find interactive flow '$flowName' in the base template."
                logger.error(errorMessage)
                error(errorMessage)
            }

            interactiveFlowsWithContent.getOrPut(interactiveFlowId) { mutableListOf() }.addAll(contentItem.content)
        } else {
            interactiveFlowsWithContent.getOrPut(mainFlowId) { mutableListOf() }.add(contentItem)
        }
    }
}

