package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.ProjectConfig
import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.service.IcmDataCache
import com.quadient.migration.service.ResourcePathProvider
import com.quadient.migration.shared.IcmPath
import com.quadient.migration.shared.toIcmPath
import com.quadient.migration.tools.logger
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Flow.WebEditingType.SECTION
import com.quadient.wfdxml.api.layoutnodes.Pages

class InspireBaseTemplateBuilder(
    private val projectConfig: ProjectConfig,
    private val icmDataCache: IcmDataCache,
    private val resourcePathProvider: ResourcePathProvider,
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

        val interactiveFlows = mutableListOf<Flow>()
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
                interactiveFlows.add(flow)

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

        layout.pages.setInteractiveFlows(interactiveFlows)
        mainFlow?.let { layout.pages.setMainFlow(it) }

        val baseTemplateXml = builder.build()
        val sourceBaseTemplatePath = if (projectConfig.sourceBaseTemplatePath.isNullOrBlank()) {
            IcmPath.root().join("Interactive").join("StandardPackage").join("Sources").join("SourceTemplate.wfd")
        } else {
            projectConfig.sourceBaseTemplatePath.toIcmPath()
        }

        return enrichLayoutWithSourceBaseTemplate(icmDataCache, baseTemplateXml, sourceBaseTemplatePath)
    }
}
