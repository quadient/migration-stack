package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.repository.BaseTemplateRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.service.IcmDataCache
import com.quadient.migration.service.ResourcePathProvider
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.toIcmPath
import com.quadient.migration.tools.logger
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Flow.WebEditingType.SECTION
import com.quadient.wfdxml.api.layoutnodes.Pages
import com.quadient.wfdxml.api.layoutnodes.email.EmailComponentPlaceHolder
import com.quadient.wfdxml.api.module.Layout

class InspireBaseTemplateBuilder(
    private val projectConfig: ProjectConfig,
    private val icmDataCache: IcmDataCache,
    private val resourcePathProvider: ResourcePathProvider,
    private val baseTemplateRepository: BaseTemplateRepository,
    private val documentObjectRepository: DocumentObjectRepository,
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

        enrichFromDocumentObjects(baseTemplate, layout)

        val baseTemplateXml = builder.build()
        val sourceBaseTemplatePath = if (projectConfig.sourceBaseTemplatePath.isNullOrBlank()) {
            IcmPath.root().join("Interactive").join("StandardPackage").join("Sources").join("SourceTemplate.wfd")
        } else {
            projectConfig.sourceBaseTemplatePath.toIcmPath()
        }

        return enrichLayoutWithSourceBaseTemplate(icmDataCache, baseTemplateXml, sourceBaseTemplatePath)
    }

    private fun enrichFromDocumentObjects(baseTemplate: BaseTemplate, layout: Layout) {
        val usages = baseTemplateRepository.findUsages(baseTemplate.id).filterIsInstance<DocumentObject>()

        var needsEmail = false
        var needsSms = false

        for (usage in usages) {
            for (content in usage.content) {
                val referencedType = (content as? DocumentObjectRef)?.id?.let(documentObjectRepository::find)?.type
                when (referencedType) {
                    DocumentObjectType.Email -> needsEmail = true
                    DocumentObjectType.Sms -> needsSms = true
                    else -> Unit
                }
            }
        }

        if (needsSms) {
            val smsFlow = layout.addFlow().setSectionFlow(true).setWebEditingType(SECTION)
                .addCustomProperty("customName", "SMS Content")
            layout.pages.addInteractiveFlow(smsFlow, Pages.InteractiveFlowType.NORMAL)
            layout.addSmsRoot().setContent(smsFlow)
        }

        if (needsEmail) {
            val emailBodyRootFlow = layout.addFlow().setSectionFlow(true).setWebEditingType(SECTION)
                .addCustomProperty("customName", "Body Content")
            layout.pages.addInteractiveFlow(emailBodyRootFlow, Pages.InteractiveFlowType.HTML)
            layout.addEmailComponentRoot().setEmailComponentsText(layout.addEmailTMText())
            layout.addEmailComponentPlaceHolder().setId("Def.EmailsBody").setType(EmailComponentPlaceHolder.Type.BODY)
                .setContent(emailBodyRootFlow)
        }
    }
}
