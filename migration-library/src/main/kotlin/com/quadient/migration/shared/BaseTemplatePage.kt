package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class BaseTemplateArea(
    var interactiveFlowName: String,
    var position: Position? = null,
    var flowToNextPage: Boolean = false,
)

@Serializable
data class BaseTemplatePage(
    var name: String? = null,
    var pageWidth: Size? = null,
    var pageHeight: Size? = null,
    var areas: List<BaseTemplateArea> = emptyList(),
)
