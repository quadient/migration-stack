package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.data.DocumentContentModel
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.DocumentObjectModelRef
import com.quadient.migration.data.FlowAreaModel
import com.quadient.migration.data.ImageModel
import com.quadient.migration.data.ImageModelRef
import com.quadient.migration.data.ParagraphStyleDefinitionModel
import com.quadient.migration.data.TextStyleDefinitionModel
import com.quadient.migration.data.VariableStructureModel
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository
import com.quadient.migration.service.getFolder
import com.quadient.migration.service.imageExtension
import com.quadient.migration.shared.DisplayRuleDefinition
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.millimeters
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Page
import com.quadient.wfdxml.api.layoutnodes.Pages
import com.quadient.wfdxml.api.layoutnodes.tables.GeneralRowSet
import com.quadient.wfdxml.api.layoutnodes.tables.RowSet
import com.quadient.wfdxml.api.module.Layout

class DesignerDocumentObjectBuilder(
    documentObjectRepository: DocumentObjectInternalRepository,
    textStyleRepository: TextStyleInternalRepository,
    paragraphStyleRepository: ParagraphStyleInternalRepository,
    variableRepository: VariableInternalRepository,
    variableStructureRepository: VariableStructureInternalRepository,
    displayRuleRepository: DisplayRuleInternalRepository,
    imageRepository: ImageInternalRepository,
    projectConfig: ProjectConfig,
) : InspireDocumentObjectBuilder(
    documentObjectRepository,
    textStyleRepository,
    paragraphStyleRepository,
    variableRepository,
    variableStructureRepository,
    displayRuleRepository,
    imageRepository,
    projectConfig
) {
    override fun getDocumentObjectPath(documentObject: DocumentObjectModel): String {
        return "icm://${getFolder(projectConfig, documentObject.targetFolder)}${documentObject.nameOrId()}.wfd"
    }

    override fun getImagePath(image: ImageModel): String {
        return "icm://${getFolder(projectConfig, image.targetFolder)}${image.nameOrId()}${imageExtension(image)}"
    }

    override fun getStyleDefinitionPath(): String {
        return "icm://${getFolder(projectConfig)}${projectConfig.name}Styles.wfd"
    }

    override fun buildDocumentObject(documentObject: DocumentObjectModel): String {
        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()

        val pageModels = mutableListOf<DocumentObjectModel>()
        val virtualPageContent = mutableListOf<DocumentContentModel>()

        val variableStructure = initVariableStructure(layout)
        documentObject.content.forEach {
            if (it is DocumentObjectModelRef) {
                val documentObjectModel = documentObjectRepository.findModelOrFail(it.id)
                if (documentObjectModel.type == DocumentObjectType.Page) {
                    pageModels.add(documentObjectModel)
                } else {
                    virtualPageContent.add(it)
                }
            } else {
                virtualPageContent.add(it)
            }
        }

        pageModels.forEach {
            buildPage(
                layout, variableStructure, it.nameOrId(), it.content, documentObject, it.options as? PageOptions
            )
        }

        if (virtualPageContent.isNotEmpty() || pageModels.isEmpty()) {
            buildPage(layout, variableStructure, "Virtual Page", virtualPageContent, documentObject)
        }

        buildTextStyles(
            layout, textStyleRepository.listAllModel().filter { it.definition is TextStyleDefinitionModel })
        buildParagraphStyles(
            layout, paragraphStyleRepository.listAllModel().filter { it.definition is ParagraphStyleDefinitionModel })

        return builder.build()
    }

    override fun wrapSuccessFlowInConditionFlow(
        layout: Layout, variableStructure: VariableStructureModel, ruleDef: DisplayRuleDefinition, successFlow: Flow
    ): Flow {
        return layout.addFlow().setType(Flow.Type.SELECT_BY_INLINE_CONDITION).addLineForSelectByInlineCondition(
            ruleDef.toScript(layout, variableStructure), successFlow
        )
    }

    override fun buildSuccessRowWrappedInConditionRow(
        layout: Layout,
        variableStructure: VariableStructureModel,
        ruleDef: DisplayRuleDefinition,
        multipleRowSet: GeneralRowSet
    ): GeneralRowSet {
        val successRow = layout.addRowSet().setType(RowSet.Type.SINGLE_ROW)

        multipleRowSet.addRowSet(
            layout.addRowSet().setType(RowSet.Type.SELECT_BY_INLINE_CONDITION).addLineForSelectByInlineCondition(
                ruleDef.toScript(layout, variableStructure), successRow
            )
        )

        return successRow
    }

    private fun buildPage(
        layout: Layout,
        variableStructure: VariableStructureModel,
        name: String,
        content: List<DocumentContentModel>,
        mainObject: DocumentObjectModel,
        options: PageOptions? = null
    ) {
        val page = layout.addPage().setName(name).setType(Pages.PageConditionType.SIMPLE)
        options?.height?.let { page.setHeight(it.toMeters()) }
        options?.width?.let { page.setWidth(it.toMeters()) }

        val flowAreaModels = mutableListOf<FlowAreaModel>()
        val virtualFlowAreaContent = mutableListOf<DocumentContentModel>()

        content.forEach {
            if (it is FlowAreaModel) {
                flowAreaModels.add(it)
            } else {
                virtualFlowAreaContent.add(it)
            }
        }

        flowAreaModels.forEach { buildArea(layout, variableStructure, page, it, mainObject) }

        if (virtualFlowAreaContent.isNotEmpty()) {
            buildArea(
                layout, variableStructure, page, FlowAreaModel(
                    Position(15.millimeters(), 15.millimeters(), 180.millimeters(), 267.millimeters()),
                    virtualFlowAreaContent
                ), mainObject
            )
        }
    }

    private fun buildArea(
        layout: Layout,
        variableStructure: VariableStructureModel,
        page: Page,
        flowAreaModel: FlowAreaModel,
        mainObject: DocumentObjectModel
    ) {
        val position = flowAreaModel.position

        val content = flowAreaModel.content
        if (content.size == 1 && content.first() is ImageModelRef) {
            val imageModel = imageRepository.findModelOrFail((content.first() as ImageModelRef).id)

            val imagePlaceholder = getImagePlaceholder(imageModel)
            if (imagePlaceholder == null) {
                page.addImageArea().setPosX(position.x.toMeters()).setPosY(position.y.toMeters())
                    .setWidth(position.width.toMeters()).setHeight(position.height.toMeters())
                    .setImage(buildImage(layout, imageModel))
            } else {
                val flow = layout.addFlow()
                flow.addParagraph().addText().appendText(imagePlaceholder)
                page.addFlowArea().setPosX(position.x.toMeters()).setPosY(position.y.toMeters())
                    .setWidth(position.width.toMeters()).setHeight(position.height.toMeters()).setFlowToNextPage(true)
                    .setFlow(flow)
            }
        } else {
            page.addFlowArea().setPosX(position.x.toMeters()).setPosY(position.y.toMeters())
                .setWidth(position.width.toMeters()).setHeight(position.height.toMeters()).setFlowToNextPage(true)
                .setFlow(
                    buildDocumentContentAsSingleFlow(
                        layout, variableStructure, flowAreaModel.content, displayRuleRef = mainObject.displayRuleRef
                    )
                )
        }
    }
}