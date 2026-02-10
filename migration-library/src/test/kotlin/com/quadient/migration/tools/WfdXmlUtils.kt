package com.quadient.migration.tools

import com.fasterxml.jackson.databind.JsonNode
import kotlin.collections.last

fun getFlowAreaContentFlow(layoutNode: JsonNode, flowAreaFlowId: String? = null): JsonNode {
    val flowId = flowAreaFlowId ?: layoutNode["FlowArea"].last()["FlowId"].textValue()
    val flowAreaFlow = layoutNode["Flow"].last { it["Id"].textValue() == flowId }

    val contentFlowId = flowAreaFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()
    return layoutNode["Flow"].last { it["Id"].textValue() == contentFlowId }
}

fun getFlowAreaContentFlowId(layoutNode: JsonNode, flowAreaFlowId: String? = null): String {
    val flowId = flowAreaFlowId ?: layoutNode["FlowArea"].last()["FlowId"].textValue()
    val flowAreaFlow = layoutNode["Flow"].last { it["Id"].textValue() == flowId }

    return flowAreaFlow["FlowContent"]["P"]["T"]["O"]["Id"].textValue()
}

