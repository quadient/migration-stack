package com.quadient.migration.service.deploy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseTemplateStructure(
    val print: BaseTemplateStructurePrint,
    val channels: List<BaseTemplateChannel>? = null,
    val insertedDocumentStartPage: String? = null,
    val onlyValidateUsedVariables: Boolean? = null,
    val preloadVariablesWithAdvancedProperties: Boolean? = null,
    val reloadTicketWhenInsertionPointIsUsed: Boolean? = null,
    val requireTaskToEditBlocks: Boolean? = null,
    val requireTaskToEditVariables: Boolean? = null,
    val resetPageNumbersForEachTemplate: Boolean? = null,
    val specifyVariableOptionsInTemplates: Boolean? = null,
)

@Serializable
data class BaseTemplateStructurePrint(
    val units: BaseTemplateUnits,
    val pageOrder: BaseTemplateStructurePageOrder,
    val pages: List<BaseTemplateStructurePage>,
)

@Serializable
data class BaseTemplateStructurePage(
    val displayName: String,
    val width: Double,
    val height: Double,
    val areas: List<BaseTemplateStructureArea> = emptyList(),
    val nextPage: BaseTemplateStructurePageSelection? = null,
)

@Serializable
sealed interface BaseTemplateStructurePageOrder {
    @Serializable
    @SerialName("Static")
    data object Static : BaseTemplateStructurePageOrder

    @Serializable
    @SerialName("Dynamic")
    data class Dynamic(val startPage: BaseTemplateStructurePageSelection? = null) : BaseTemplateStructurePageOrder
}

@Serializable
sealed interface BaseTemplateStructurePageSelection {
    @Serializable
    @SerialName("Specific")
    data class Specific(val page: String) : BaseTemplateStructurePageSelection

    @Serializable
    @SerialName("Dynamic")
    data class Dynamic(
        val conditions: List<BaseTemplateStructurePageCondition> = emptyList(),
        val defaultPage: String? = null,
    ) : BaseTemplateStructurePageSelection
}

@Serializable
data class BaseTemplateStructurePageCondition(
    val page: String,
    val rule: BaseTemplateStructurePageConditionRule,
    val rollback: Int? = null,
)

@Serializable
sealed interface BaseTemplateStructurePageConditionRule {
    @Serializable
    @SerialName("Overflow")
    data object Overflow : BaseTemplateStructurePageConditionRule
}

@Serializable
sealed interface BaseTemplateStructureArea {
    val displayName: String?
    val x: Double
    val y: Double
    val width: Double
    val height: Double
    val locked: Boolean?
    val rotation: Double?

    @Serializable
    @SerialName("text")
    data class Text(
        override val displayName: String? = null,
        override val x: Double,
        override val y: Double,
        override val width: Double,
        override val height: Double,
        override val locked: Boolean? = null,
        override val rotation: Double? = null,
        val main: Boolean? = null,
        val documentArea: Boolean? = null,
        val contentId: String? = null,
        val flowingToNextPage: Boolean? = null,
        val ignoreEmptyLinesAtBeginning: Boolean? = null,
        val channels: List<BaseTemplateChannel>? = null,
        val pdfTaggingType: PdfStructureTag? = null,
        val supplementaryCharSupport: Boolean? = null,
        val verticalAlignment: TextVerticalAlign? = null,
        val worldWideSupport: Boolean? = null,
        val writingDirection: TextWritingDirection? = null,
    ) : BaseTemplateStructureArea

    @Serializable
    @SerialName("image")
    data class Image(
        override val displayName: String? = null,
        override val x: Double,
        override val y: Double,
        override val width: Double,
        override val height: Double,
        override val locked: Boolean? = null,
        override val rotation: Double? = null,
        val image: BaseTemplateStructureImageReference,
        val clippingImage: Boolean? = null,
        val colorSpace: ImageColorSpace? = null,
        val dpiX: Double? = null,
        val dpiY: Double? = null,
        val horizontalAlign: ImageHorizontalAlign? = null,
        val verticalAlignment: ImageVerticalAlign? = null,
        val loadNatively: Boolean? = null,
        val offsetX: Double? = null,
        val offsetY: Double? = null,
        val preserveAspectRatio: Boolean? = null,
        val shrink: Boolean? = null,
        val stretch: Boolean? = null,
        val pdfTaggingType: PdfFigureTag? = null,
        val defaultAlternativeText: List<BaseTemplateStructureTextOrVariablePart>? = null,
        val languageAlternativeText: List<Map<String, List<BaseTemplateStructureTextOrVariablePart>>>? = null,
        val urlTarget: List<BaseTemplateStructureTextOrVariablePart>? = null,
        val openUrlTargetInNewWindow: Boolean? = null,
    ) : BaseTemplateStructureArea

    @Serializable
    @SerialName("shape")
    data class Shape(
        override val displayName: String? = null,
        override val x: Double,
        override val y: Double,
        override val width: Double,
        override val height: Double,
        override val locked: Boolean? = null,
        override val rotation: Double? = null,
        val shape: ShapeType,
        val fillColor: String? = null,
        val lineColor: String? = null,
        val lineStyle: String? = null,
        val lineWidth: Double? = null,
        val pdfTaggingType: PdfFigureTag? = null,
        val defaultAlternativeText: List<BaseTemplateStructureTextOrVariablePart>? = null,
        val languageAlternativeText: List<Map<String, List<BaseTemplateStructureTextOrVariablePart>>>? = null,
    ) : BaseTemplateStructureArea

    @Serializable
    @SerialName("objectGroup")
    data class ObjectGroup(
        override val displayName: String? = null,
        override val x: Double,
        override val y: Double,
        override val width: Double,
        override val height: Double,
        override val locked: Boolean? = null,
        override val rotation: Double? = null,
        val areas: List<BaseTemplateStructureArea> = emptyList(),
        val pdfTaggingType: PdfStructureTag? = null,
    ) : BaseTemplateStructureArea

    @Serializable
    @SerialName("variableImage")
    data class VariableImage(
        override val displayName: String? = null,
        override val x: Double,
        override val y: Double,
        override val width: Double,
        override val height: Double,
        override val locked: Boolean? = null,
        override val rotation: Double? = null,
        val variableImageLocation: List<BaseTemplateStructureTextOrVariablePart>,
        val defaultImageLocation: String? = null,
        val resolution: BaseTemplateStructureImageResolution? = null,
        val clippingImage: Boolean? = null,
        val colorSpace: ImageColorSpace? = null,
        val horizontalAlign: ImageHorizontalAlign? = null,
        val verticalAlignment: ImageVerticalAlign? = null,
        val loadNatively: Boolean? = null,
        val offsetX: Double? = null,
        val offsetY: Double? = null,
        val preserveAspectRatio: Boolean? = null,
        val shrink: Boolean? = null,
        val stretch: Boolean? = null,
        val pdfTaggingType: PdfFigureTag? = null,
        val defaultAlternativeText: List<BaseTemplateStructureTextOrVariablePart>? = null,
        val languageAlternativeText: List<Map<String, List<BaseTemplateStructureTextOrVariablePart>>>? = null,
    ) : BaseTemplateStructureArea
}

@Serializable
sealed interface BaseTemplateStructureTextOrVariablePart {
    @Serializable
    @SerialName("Text")
    data class Text(val text: String) : BaseTemplateStructureTextOrVariablePart

    @Serializable
    @SerialName("Variable")
    data class Variable(val variableId: String) : BaseTemplateStructureTextOrVariablePart
}

@Serializable
data class BaseTemplateStructureImageReference(
    val location: String,
    val binaryData: BaseTemplateStructureImageBinaryData? = null,
    val metadata: BaseTemplateStructureImageMetadata? = null,
    val pageCount: Double? = null,
    val selectedPage: Double? = null,
)

@Serializable
data class BaseTemplateStructureImageBinaryData(
    val data: String? = null,
    val width: Double? = null,
    val height: Double? = null,
)

@Serializable
data class BaseTemplateStructureImageMetadata(
    val dpiX: Double? = null,
    val dpiY: Double? = null,
    val width: Double? = null,
    val height: Double? = null,
    val size: Double? = null,
)

@Serializable
data class BaseTemplateStructureImageResolution(
    val value: Double,
    val unit: ImageResolutionUnit? = null,
)

@Serializable
enum class BaseTemplateUnits {
    @SerialName("mm") Millimeters,
    @SerialName("cm") Centimeters,
    @SerialName("in") Inches,
    @SerialName("m") Meters,
    @SerialName("px") Pixels,
    @SerialName("pt") Points,
}

@Serializable
enum class BaseTemplateChannel { Print, Email, Digital, SMS, PushNotification }

@Serializable
enum class PdfStructureTag {
    None, Artifact, Part, Article, Section, Division, BlockQuotation, Caption, Index, NonstructuralElement, PrivateElement
}

@Serializable
enum class PdfFigureTag { None, Artifact, Figure, Formula }

@Serializable
enum class ImageColorSpace { BlackAndWhite, GrayScale, RGB, CMYK }

@Serializable
enum class ImageHorizontalAlign { Left, Right, Center }

@Serializable
enum class ImageVerticalAlign { Top, Middle, Bottom }

@Serializable
enum class TextVerticalAlign { Top, Center, Bottom, Justify }

@Serializable
enum class TextWritingDirection { Horizontal, Vertical }

@Serializable
enum class ShapeType { Rectangle, Ellipse, Triangle }

@Serializable
enum class ImageResolutionUnit {
    @SerialName("pixelsPerInch") PixelsPerInch,
    @SerialName("pixelsPerCm") PixelsPerCm,
}
