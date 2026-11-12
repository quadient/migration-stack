package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
data class BaseTemplateArea @JvmOverloads constructor(
    var interactiveFlowName: String,
    var position: Position? = null,
    var flowToNextPage: Boolean = false,
    var name: String? = null,
)

@Serializable
data class BaseTemplatePage(
    var name: String? = null,
    var pageWidth: Size? = null,
    var pageHeight: Size? = null,
    var areas: List<BaseTemplateArea> = emptyList(),
)
