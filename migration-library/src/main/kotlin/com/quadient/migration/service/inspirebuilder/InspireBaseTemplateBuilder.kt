package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.tools.logger
import com.quadient.wfdxml.WfdXmlBuilder
import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Flow.WebEditingType.SECTION
import com.quadient.wfdxml.api.layoutnodes.Pages

class InspireBaseTemplateBuilder {
    private val logger by logger()

    fun buildBaseTemplate(baseTemplate: BaseTemplate): String {
        logger.debug("Starting to build base template '${baseTemplate.nameOrId()}'.")

        val builder = WfdXmlBuilder()
        val layout = builder.addLayout()
        layout.setName("DocumentLayout")
        layout.addRoot()

        val interactiveFlows = mutableListOf<Flow>()
        var mainFlow: Flow? = null

        baseTemplate.pages.forEach { page ->
            val wfdPage = layout.addPage().setType(Pages.PageConditionType.SIMPLE)
            page.name?.let { wfdPage.setName(it) }
            page.pageWidth?.let { wfdPage.setWidth(it.toMeters()) }
            page.pageHeight?.let { wfdPage.setHeight(it.toMeters()) }

            page.areas.forEach { area ->
                val flow = layout.addFlow()
                    .setId("Def.InteractiveFlow${interactiveFlows.size}")
                    .setName(area.interactiveFlowName)
                    .setType(Flow.Type.SIMPLE)
                    .setSectionFlow(true)
                    .setWebEditingType(SECTION)
                interactiveFlows.add(flow)

                // TODO: main flow selection is a placeholder until we decide the real rule for it.
                if (mainFlow == null) mainFlow = flow

                val flowArea = wfdPage.addFlowArea().setFlow(flow).setFlowToNextPage(area.flowToNextPage)
                area.position?.let {
                    flowArea.setPosX(it.x.toMeters()).setPosY(it.y.toMeters()).setWidth(it.width.toMeters())
                        .setHeight(it.height.toMeters())
                }
            }
        }

        layout.pages.setInteractiveFlows(interactiveFlows)
        mainFlow?.let { layout.pages.setMainFlow(it) }

        logger.debug("Successfully built base template '${baseTemplate.nameOrId()}'.")
        return builder.build()
    }
}
