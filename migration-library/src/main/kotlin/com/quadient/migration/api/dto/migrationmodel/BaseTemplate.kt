package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.shared.BaseTemplatePage
import kotlin.time.Instant

data class BaseTemplate(
    override val id: String,
    override var name: String? = null,
    override var originLocations: List<String> = emptyList(),
    override var customFields: CustomFieldMap,
    var targetFolder: String? = null,
    var pages: List<BaseTemplatePage> = emptyList(),
    override var created: Instant? = null,
    override var lastUpdated: Instant? = null,
) : MigrationObject
