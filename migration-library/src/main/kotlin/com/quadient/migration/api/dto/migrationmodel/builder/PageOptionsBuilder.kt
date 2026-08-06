package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.api.dto.migrationmodel.PageOptions
import com.quadient.migration.shared.Size

class PageOptionsBuilder {
    var width: Size? = null; private set
    var height: Size? = null; private set

    fun width(width: Size?) = apply { this.width = width }
    fun height(height: Size?) = apply { this.height = height }

    fun build(): PageOptions = PageOptions(
        width = width,
        height = height,
    )
}
