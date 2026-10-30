package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.EmailOptions
import com.quadient.migration.api.dto.migrationmodel.SmsOptions
import com.quadient.migration.api.repository.BaseTemplateRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.service.IcmDataCache
import com.quadient.migration.service.ResourcePathProvider
import com.quadient.migration.service.deploy.utility.RefInheritanceServiceImpl
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.tools.logger
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Flow.WebEditingType.SECTION
import com.quadient.wfdxml.api.layoutnodes.Pages
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentPlaceHolder
import com.quadient.wfdxml.api.module.Layout

class InspireBaseTemplateBuilder(
    private val icmDataCache: IcmDataCache,
    private val resourcePathProvider: ResourcePathProvider,
    private val baseTemplateRepository: BaseTemplateRepository,
    private val documentObjectRepository: DocumentObjectRepository,
    private val variableStructureBuilder: InspireVariableStructureBuilder,
    private val refInheritanceService: RefInheritanceServiceImpl,
) {
    private val logger by logger()

    private val resolvedStyleDefinitionPath: IcmPath? by lazy {
        val path = resourcePathProvider.getStyleDefinitionPath()
        try {
            if (icmDataCache.fileExists(path)) path else null
        } catch (e: Exception) {
            throw RuntimeException("Failed to check for style definition existence", e)
        }
    }

    fun buildBaseTemplate(baseTemplate: BaseTemplate): String {
        logger.debug("Starting to build base template '${baseTemplate.nameOrId()}'.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        val root = layout.setName("DocumentLayout").addRoot().setAllowRuntimeModifications(true)
        if (resolvedStyleDefinitionPath != null) {
            root.setExternalStylesLayout(resolvedStyleDefinitionPath.toString())
        }

        val usages = baseTemplateRepository.findUsages(baseTemplate.id).filterIsInstance<DocumentObject>()
        val channelModels = findChannelModels(usages)
        val variableStructureId = resolveVariableStructureId(baseTemplate, usages, channelModels)
        resolveDataDefinitionPath(baseTemplate, variableStructureId)?.let { root.setDataDefinition(it.toString()) }

        resolveArialFont(layout, icmDataCache)

        var mainFlow: Flow? = null
        var mainFlowSize = -1.0

        baseTemplate.pages.forEachIndexed { pageIndex, page ->
            val wfdPage = layout.addPage().setType(Pages.PageConditionType.SIMPLE)
            page.name?.let { wfdPage.setName(it) }
            page.pageWidth?.let { wfdPage.setWidth(it.toMeters()) }
            page.pageHeight?.let { wfdPage.setHeight(it.toMeters()) }

            page.areas.forEach { area ->
                val flow = layout.addFlow()
                    .setName(area.interactiveFlowName)
                    .setType(Flow.Type.SIMPLE)
                    .setSectionFlow(true)
                    .setWebEditingType(SECTION)
                layout.pages.addInteractiveFlow(flow, Pages.InteractiveFlowType.NORMAL)

                val flowArea = wfdPage.addFlowArea().setName("${area.interactiveFlowName}Area").setFlow(flow)
                    .setFlowToNextPage(area.flowToNextPage)

                var areaSize = -1.0
                area.position?.let {
                    flowArea.setPosX(it.x.toMeters()).setPosY(it.y.toMeters()).setWidth(it.width.toMeters())
                        .setHeight(it.height.toMeters())
                    areaSize = it.width.toMeters() * it.height.toMeters()
                }

                if (pageIndex == 0 && areaSize > mainFlowSize) {
                    mainFlow = flow
                    mainFlowSize = areaSize
                }
            }
        }

        mainFlow?.let { layout.pages.setMainFlow(it) }

        enrichFromDocumentObjects(channelModels, variableStructureId, layout)

        val baseTemplateXml = builder.build()
        return enrichLayoutWithSourceBaseTemplate(icmDataCache, baseTemplateXml, resourcePathProvider.getSourceBaseTemplatePath())
    }

    fun findChannelModels(baseTemplate: BaseTemplate): BaseTemplateChannelModels {
        return findChannelModels(baseTemplateRepository.findUsages(baseTemplate.id).filterIsInstance<DocumentObject>())
    }

    private fun findChannelModels(usages: List<DocumentObject>): BaseTemplateChannelModels {
        var emailModel: DocumentObject? = null
        var smsModel: DocumentObject? = null

        for (usage in usages) {
            for (content in usage.content) {
                val referenced = (content as? DocumentObjectRef)?.id?.let(documentObjectRepository::find) ?: continue
                when (referenced.type) {
                    DocumentObjectType.Email -> if (emailModel == null) emailModel = referenced
                    DocumentObjectType.Sms -> if (smsModel == null) smsModel = referenced
                    else -> Unit
                }
            }
        }

        return BaseTemplateChannelModels(emailModel, smsModel)
    }

    private fun resolveVariableStructureId(
        baseTemplate: BaseTemplate, usages: List<DocumentObject>, channelModels: BaseTemplateChannelModels,
    ): String? {
        baseTemplate.variableStructureRef?.id?.let { return it }

        val candidates = listOfNotNull(channelModels.email, channelModels.sms) + usages
        if (candidates.isEmpty()) return null
        return refInheritanceService.apply(candidates).firstNotNullOfOrNull { it.variableStructureRef?.id }
    }

    private fun resolveDataDefinitionPath(baseTemplate: BaseTemplate, variableStructureId: String?): IcmPath? {
        val variableStructure = variableStructureBuilder.findVariableStructure(variableStructureId)
        if (variableStructure == null) {
            if (variableStructureId != null) {
                logger.warn("Variable structure '$variableStructureId' of base template '${baseTemplate.nameOrId()}' not found, data definition is not linked.")
            }
            return null
        }

        val path = resourcePathProvider.getDataDefinitionPath(variableStructure)
        val exists = try {
            icmDataCache.fileExists(path)
        } catch (e: Exception) {
            throw RuntimeException("Failed to check for data definition existence", e)
        }
        if (!exists) {
            logger.warn("Data definition '$path' of base template '${baseTemplate.nameOrId()}' does not exist, data definition is not linked.")
            return null
        }
        return path
    }

    private fun enrichFromDocumentObjects(
        channelModels: BaseTemplateChannelModels, variableStructureId: String?, layout: Layout,
    ) {
        val (emailModel, smsModel) = channelModels
        if (emailModel == null && smsModel == null) return

        val variableStructure = variableStructureBuilder.initVariableStructure(layout, variableStructureId)

        if (smsModel != null) {
            val smsFlow = layout.addFlow().setSectionFlow(true).setWebEditingType(SECTION)
                .addCustomProperty("customName", "SMS Content")
            layout.pages.addInteractiveFlow(smsFlow, Pages.InteractiveFlowType.NORMAL)
            layout.addSmsRoot().setContent(smsFlow)
            variableStructureBuilder.addSmsNumberToPages(
                layout, smsModel.options as? SmsOptions, variableStructure
            )
        }

        if (emailModel != null) {
            val emailBodyRootFlow = layout.addFlow().setSectionFlow(true).setWebEditingType(SECTION)
                .addCustomProperty("customName", "Body Content")
            layout.pages.addInteractiveFlow(emailBodyRootFlow, Pages.InteractiveFlowType.HTML)
            layout.addEmailComponentRoot().setEmailComponentsText(layout.addEmailTMText())
            layout.addEmailComponentPlaceHolder().setId("Def.EmailsBody").setType(EmailComponentPlaceHolder.Type.BODY)
                .setContent(emailBodyRootFlow)
            variableStructureBuilder.addEmailMetadataToPages(
                layout, emailModel.options as? EmailOptions, variableStructure
            )
        }
    }
}

data class BaseTemplateChannelModels(val email: DocumentObject?, val sms: DocumentObject?)
