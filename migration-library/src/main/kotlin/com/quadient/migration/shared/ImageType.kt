package com.quadient.migration.shared

enum class ImageType {
    Bmp, Gif, Jpeg, Png, Tga, Tiff, Svg, Unknown;

    fun extension(): String? {
        return when (this) {
            Bmp -> ".bmp"
            Gif -> ".gif"
            Jpeg -> ".jpg"
            Png -> ".png"
            Tga -> ".tga"
            Tiff -> ".tiff"
            Svg -> ".svg"
            Unknown -> null
        }
    }
}