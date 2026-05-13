package com.quadient.migration.api.dto.migrationmodel.builder.documentcontent

import com.quadient.migration.api.dto.migrationmodel.Barcode
import com.quadient.migration.api.dto.migrationmodel.Code39Barcode
import com.quadient.migration.api.dto.migrationmodel.QrCode
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasPosition
import com.quadient.migration.api.dto.migrationmodel.builder.components.HasVariableRef
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.QrCodeErrorCorrectionLevel
import com.quadient.migration.shared.QrCodeSize
import com.quadient.migration.shared.Size

class BarcodeBuilder {
    private var result: Barcode? = null

    fun qr() = QrCodeBuilder()
    fun qr(block: QrCodeBuilder.() -> Unit) = apply {
        this.result = QrCodeBuilder().apply(block).build()
    }

    fun code39() = Code39BarcodeBuilder()
    fun code39(block: Code39BarcodeBuilder.() -> Unit) = apply {
        this.result = Code39BarcodeBuilder().apply(block).build()
    }

    fun build(): Barcode {
        return requireNotNull(result) { "A barcode type must be configured (e.g. qr { ... })." }
    }
}

class QrCodeBuilder : HasPosition<QrCodeBuilder>, HasVariableRef<QrCodeBuilder> {
    override var position: Position? = null
    override var variableRef: VariableRef? = null
    private var data: String? = null
    private var errorCorrection: QrCodeErrorCorrectionLevel = QrCodeErrorCorrectionLevel.M
    private var size: QrCodeSize = QrCodeSize.Auto
    private var moduleWidth: Size = Size.ofMillimeters(1)
    private var quietZone: Size = Size.ofMillimeters(4)
    private var barcodeFill: Color? = Color.fromHex("#000000")
    private var backgroundFill: Color? = null

    /**
     * Sets the barcode data content.
     * @param data The data to encode.
     * @return This builder instance for method chaining.
     */
    fun data(data: String?) = apply { this.data = data }

    /**
     * Sets the barcode fill color.
     * @param color The [Color] for the barcode modules.
     * @return This builder instance for method chaining.
     */
    fun barcodeFill(color: Color?) = apply { this.barcodeFill = color }

    /**
     * Sets the background fill color.
     * @param color The [Color] for the barcode background.
     * @return This builder instance for method chaining.
     */
    fun backgroundFill(color: Color?) = apply { this.backgroundFill = color }

    /**
     * Sets the error correction level.
     * @param level The [QrCodeErrorCorrectionLevel].
     * @return This builder instance for method chaining.
     */
    fun errorCorrection(level: QrCodeErrorCorrectionLevel) = apply { this.errorCorrection = level }

    /**
     * Sets the QR code size.
     * @param size The [QrCodeSize].
     * @return This builder instance for method chaining.
     */
    fun size(size: QrCodeSize) = apply { this.size = size }

    /**
     * Sets the module width.
     * @param width The module width as a [Size].
     * @return This builder instance for method chaining.
     */
    fun moduleWidth(width: Size) = apply { this.moduleWidth = width }

    /**
     * Sets the quiet zone size around the barcode.
     * @param size The quiet zone as a [Size].
     * @return This builder instance for method chaining.
     */
    fun quietZone(size: Size) = apply { this.quietZone = size }

    /**
     * Builds a [QrCode] from the current builder state.
     * @return A fully configured [QrCode].
     */
    fun build(): QrCode {
        return QrCode(
            position = position,
            data = data,
            variableRef = variableRef,
            errorCorrection = errorCorrection,
            size = size,
            moduleWidth = moduleWidth,
            quiteZone = quietZone,
            barcodeFill = barcodeFill,
            backgroundFill = backgroundFill,
        )
    }
}

class Code39BarcodeBuilder : HasPosition<Code39BarcodeBuilder>, HasVariableRef<Code39BarcodeBuilder> {
    override var position: Position? = null
    override var variableRef: VariableRef? = null
    private var data: String? = null
    private var barcodeFill: Color? = Color.fromHex("#000000")
    private var backgroundFill: Color? = null
    private var height: Size = Size.ofMillimeters(10)
    private var moduleSize: Size = Size.ofMillimeters(0.19)
    private var quietZone: Double = 0.0
    private var useControlSum: Boolean = false
    private var ratio: Double = 2.0
    private var interCharacterSpaceRatio: Double = 1.0
    private var directMetric: Boolean = false
    private var firstBarWidth: Size = Size.ofMillimeters(0.19)
    private var secondBarWidth: Size = Size.ofMillimeters(0.57)
    private var firstBarSpace: Size = Size.ofMillimeters(0.19)
    private var secondBarSpace: Size = Size.ofMillimeters(0.57)

    /**
     * Sets the barcode data content.
     * @param data The data to encode.
     * @return This builder instance for method chaining.
     */
    fun data(data: String?) = apply { this.data = data }

    /**
     * Sets the barcode fill color.
     * @param color The [Color] for the barcode modules.
     * @return This builder instance for method chaining.
     */
    fun barcodeFill(color: Color?) = apply { this.barcodeFill = color }

    /**
     * Sets the background fill color.
     * @param color The [Color] for the barcode background.
     * @return This builder instance for method chaining.
     */
    fun backgroundFill(color: Color?) = apply { this.backgroundFill = color }

    /**
     * Sets the barcode height.
     * @param height The barcode height as a [Size].
     * @return This builder instance for method chaining.
     */
    fun height(height: Size) = apply { this.height = height }

    /**
     * Sets the module width.
     * @param width The size width as a [Size].
     * @return This builder instance for method chaining.
     */
    fun moduleWidth(width: Size) = apply { this.moduleSize = width }

    /**
     * Sets the quiet zone.
     * @param quietZone The quiet zone value.
     * @return This builder instance for method chaining.
     */
    fun quietZone(quietZone: Double) = apply { this.quietZone = quietZone }

    /**
     * Sets whether to use a control sum character.
     * @param value `true` to enable control sum, `false` to disable.
     * @return This builder instance for method chaining.
     */
    fun useControlSum(value: Boolean) = apply { this.useControlSum = value }

    /**
     * Sets the wide-to-narrow bar ratio.
     * @param ratio The ratio value.
     * @return This builder instance for method chaining.
     */
    fun ratio(ratio: Double) = apply { this.ratio = ratio }

    /**
     * Sets the inter-character space ratio.
     * @param ratio The inter-character space ratio value.
     * @return This builder instance for method chaining.
     */
    fun interCharacterSpaceRatio(ratio: Double) = apply { this.interCharacterSpaceRatio = ratio }

    /**
     * Sets whether to use direct metric.
     * @param value `true` to enable direct metric, `false` to disable.
     * @return This builder instance for method chaining.
     */
    fun directMetric(value: Boolean) = apply { this.directMetric = value }

    /**
     * Sets the first bar width.
     * @param width The first bar width value.
     * @return This builder instance for method chaining.
     */
    fun firstBarWidth(width: Size) = apply { this.firstBarWidth = width }

    /**
     * Sets the second bar width.
     * @param width The second bar width value.
     * @return This builder instance for method chaining.
     */
    fun secondBarWidth(width: Size) = apply { this.secondBarWidth = width }

    /**
     * Sets the first bar space.
     * @param width The first bar space value.
     * @return This builder instance for method chaining.
     */
    fun firstBarSpace(width: Size) = apply { this.firstBarSpace = width }

    /**
     * Sets the second bar space.
     * @param width The second bar space value.
     * @return This builder instance for method chaining.
     */
    fun secondBarSpace(width: Size) = apply { this.secondBarSpace = width }

    /**
     * Builds a [Code39Barcode] from the current builder state.
     * @return A fully configured [Code39Barcode].
     */
    fun build(): Code39Barcode {
        return Code39Barcode(
            position = position,
            data = data,
            variableRef = variableRef,
            barcodeFill = barcodeFill,
            backgroundFill = backgroundFill,
            height = height,
            moduleWidth = moduleSize,
            quietZone = quietZone,
            useControlSum = useControlSum,
            ratio = ratio,
            interCharacterSpaceRatio = interCharacterSpaceRatio,
            directMetric = directMetric,
            firstBarWidth = firstBarWidth,
            secondBarWidth = secondBarWidth,
            firstBarSpace = firstBarSpace,
            secondBarSpace = secondBarSpace,
        )
    }
}
