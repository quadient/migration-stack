package com.quadient.migration.api.dto.migrationmodel.builder

import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.Size

class ImageOptionsBuilder {
    var resizeWidth: Size? = null; private set
    var resizeHeight: Size? = null; private set

    fun resizeWidth(resizeWidth: Size?) = apply { this.resizeWidth = resizeWidth }

    fun resizeHeight(resizeHeight: Size?) = apply { this.resizeHeight = resizeHeight }

    fun build(): ImageOptions = ImageOptions(
        resizeWidth = resizeWidth,
        resizeHeight = resizeHeight,
    )
}
