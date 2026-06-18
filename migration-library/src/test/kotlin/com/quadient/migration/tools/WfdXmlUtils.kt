package com.quadient.migration.tools

import tools.jackson.databind.JsonNode
import kotlin.collections.last

fun getFlowAreaContentFlow(layoutNode: JsonNode, flowAreaFlowId: String? = null): JsonNode {
    val flowId = flowAreaFlowId ?: layoutNode["FlowArea"].last()["FlowId"].stringValue()
    val flowAreaFlow = layoutNode["Flow"].last { it["Id"].stringValue() == flowId }

    val contentFlowId = flowAreaFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
    return layoutNode["Flow"].last { it["Id"].stringValue() == contentFlowId }
}

fun getFlowAreaContentFlowId(layoutNode: JsonNode, flowAreaFlowId: String? = null): String {
    val flowId = flowAreaFlowId ?: layoutNode["FlowArea"].last()["FlowId"].stringValue()
    val flowAreaFlow = layoutNode["Flow"].last { it["Id"].stringValue() == flowId }

    return flowAreaFlow["FlowContent"]["P"]["T"]["O"]["Id"].stringValue()
}

