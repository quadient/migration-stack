package com.quadient.migration.service.inspirebuilder

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.data.AreaModel
import com.quadient.migration.data.DocumentContentModel
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
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import kotlin.collections.find

class InteractiveDocumentObjectBuilder(
    documentObjectRepository: DocumentObjectInternalRepository,
    textStyleRepository: TextStyleInternalRepository,
    paragraphStyleRepository: ParagraphStyleInternalRepository,
    variableRepository: VariableInternalRepository,
    variableStructureRepository: VariableStructureInternalRepository,
    displayRuleRepository: DisplayRuleInternalRepository,
    imageRepository: ImageInternalRepository,
    projectConfig: ProjectConfig,
    private val ipsService: IpsService,
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
    private val xmlMapper by lazy { XmlMapper().registerKotlinModule() }

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

        val variableStructure = initVariableStructure(layout)

        val interactiveFlowsWithContent = mutableMapOf<String, MutableList<DocumentContentModel>>()

        if (documentObject.type == DocumentObjectType.Template || documentObject.type == DocumentObjectType.Page) {
            for (contentPart in documentObject.content) {
                if (contentPart !is AreaModel || contentPart.interactiveFlowName.isNullOrBlank()) {
                    val mainFlowWithContent = interactiveFlowsWithContent.getOrPut("Def.MainFlow") { mutableListOf() }
                    mainFlowWithContent.add(contentPart)
                } else {
                    val interactiveFlowName = contentPart.interactiveFlowName

                    val interactiveFlowNames = getBaseTemplateInteractiveFlows(
                        documentObject.baseTemplate ?: projectConfig.baseTemplatePath
                    ).map { it.lowercase() }
                    val interactiveFlowIndex = interactiveFlowNames.indexOf(interactiveFlowName.lowercase())

                    val interactiveFlowId = "Def.InteractiveFlow$interactiveFlowIndex"

                    val interactiveFlowWithContent = interactiveFlowsWithContent.getOrPut(interactiveFlowId) { mutableListOf() }
                    interactiveFlowWithContent.addAll(contentPart.content)
                }
            }
        } else if (documentObject.type == DocumentObjectType.Block || documentObject.type == DocumentObjectType.Section) {
            interactiveFlowsWithContent.put("Def.MainFlow", documentObject.content.toMutableList())
        }

        interactiveFlowsWithContent.forEach {
            val interactiveFlowText =
                layout.addFlow().setId(it.key).setType(Flow.Type.SIMPLE).setSectionFlow(true).addParagraph().addText()

            val contentFlows =
                buildDocumentContentAsFlows(layout, variableStructure, it.value, documentObject.nameOrId())

            if (documentObject.displayRuleRef == null) {
                contentFlows.forEach { contentFlow -> interactiveFlowText.appendFlow(contentFlow) }
            } else {
                interactiveFlowText.appendFlow(
                    contentFlows.toSingleFlow(
                        layout, variableStructure, documentObject.nameOrId(), documentObject.displayRuleRef
                    )
                )
            }
        }

        logger.debug("Successfully built document object '${documentObject.nameOrId()}'")
        return builder.buildLayoutDelta()
    }

    fun getBaseTemplateInteractiveFlows(baseTemplatePath: String): List<String> {
        val baseTemplateXml = ipsService.wfd2xml(baseTemplatePath)
        val baseTemplateXmlTree = xmlMapper.readTree(baseTemplateXml.trimIndent())
        return baseTemplateXmlTree["Property"].find { it["Name"].textValue() == "InteractiveFlowsNames" }
            ?.let { it["Value"].textValue() }?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
    }
}