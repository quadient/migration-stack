package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.HyperlinkEntity

data class Hyperlink(
    val url: String,
    val displayText: String? = null,
    val alternateText: String? = null
) : TextContent {
    companion object {
        fun fromDb(entity: HyperlinkEntity) = Hyperlink(
            url = entity.url,
            displayText = entity.displayText,
            alternateText = entity.alternateText
        )
    }

    fun toDb() = HyperlinkEntity(
        url = url,
        displayText = displayText,
        alternateText = alternateText
    )
}
