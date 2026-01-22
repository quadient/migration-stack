package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.data.HyperlinkModel
import com.quadient.migration.persistence.migrationmodel.HyperlinkEntity

data class Hyperlink(
    val url: String,
    val displayText: String? = null,
    val alternateText: String? = null
) : TextContent {
    companion object {
        fun fromModel(model: HyperlinkModel) = Hyperlink(
            url = model.url,
            displayText = model.displayText,
            alternateText = model.alternateText
        )
    }

    fun toModel() = HyperlinkModel(
        url = url,
        displayText = displayText,
        alternateText = alternateText
    )

    fun toDb() = HyperlinkEntity(
        url = url,
        displayText = displayText,
        alternateText = alternateText
    )
}
