package com.quadient.migration.data

import com.quadient.migration.persistence.migrationmodel.HyperlinkEntity

data class HyperlinkModel(
    val url: String,
    val displayText: String? = null,
    val alternateText: String? = null
) : TextContentModel {
    companion object {
        fun fromDb(entity: HyperlinkEntity) = HyperlinkModel(
            url = entity.url,
            displayText = entity.displayText,
            alternateText = entity.alternateText
        )
    }
}
