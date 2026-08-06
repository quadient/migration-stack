package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.Size

class ImageOptionsBuilder {
    private var resizeWidth: Size? = null
    private var resizeHeight: Size? = null

    fun resizeWidth(resizeWidth: Size?) = apply { this.resizeWidth = resizeWidth }

    fun resizeHeight(resizeHeight: Size?) = apply { this.resizeHeight = resizeHeight }

    fun build(): ImageOptions = ImageOptions(
        resizeWidth = resizeWidth,
        resizeHeight = resizeHeight,
    )
}
