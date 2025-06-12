package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.ImageModel
import com.quadient.migration.persistence.repository.DisplayRuleInternalRepository
import com.quadient.migration.persistence.repository.DocumentObjectInternalRepository
import com.quadient.migration.persistence.repository.ImageInternalRepository
import com.quadient.migration.persistence.repository.ParagraphStyleInternalRepository
import com.quadient.migration.persistence.repository.TextStyleInternalRepository
import com.quadient.migration.persistence.repository.VariableInternalRepository
import com.quadient.migration.persistence.repository.VariableStructureInternalRepository
import com.quadient.migration.service.getFolder
import com.quadient.migration.service.imageExtension
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow

class InteractiveDocumentObjectBuilder(
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
        return "icm://Interactive/${projectConfig.interactiveTenant}/${documentObject.type.toInteractiveFolder()}/${
            getFolder(projectConfig, documentObject.targetFolder)
        }${documentObject.nameOrId()}.jld"
    }

    override fun getImagePath(image: ImageModel): String {
        return "icm://Interactive/${projectConfig.interactiveTenant}/Resources/Images/${
            getFolder(projectConfig, image.targetFolder)
        }${image.nameOrId()}${imageExtension(image)}"
    }

    override fun getStyleDefinitionPath(): String {
        return "icm://Interactive/${projectConfig.interactiveTenant}/CompanyStyles/${getFolder(projectConfig)}${projectConfig.name}Styles.wfd"
    }

    override fun buildDocumentObject(documentObject: DocumentObjectModel): String {
        logger.debug("Starting to build document object '${documentObject.nameOrId()}'.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()

        val mainFlow = layout.addFlow().setId("Def.MainFlow").setType(Flow.Type.SIMPLE).setSectionFlow(true)
        val mainFlowText = mainFlow.addParagraph().addText()

        val variableStructure = initVariableStructure(layout)
        val flows = buildDocumentContentAsFlows(
            layout, variableStructure, documentObject.content, documentObject.nameOrId()
        )

        if (documentObject.displayRuleRef == null) {
            flows.forEach { mainFlowText.appendFlow(it) }
        } else {
            mainFlowText.appendFlow(
                flows.toSingleFlow(layout, variableStructure, documentObject.nameOrId(), documentObject.displayRuleRef)
            )
        }

        logger.debug("Successfully built document object '${documentObject.nameOrId()}'")
        return builder.buildLayoutDelta()
    }
}