package com.quadient.migration.api.dto.migrationmodel

import com.quadient.migration.persistence.migrationmodel.BarcodeEntity
import com.quadient.migration.persistence.migrationmodel.Code39BarcodeEntity
import com.quadient.migration.persistence.migrationmodel.QrCodeEntity
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.QrCodeErrorCorrectionLevel
import com.quadient.migration.shared.QrCodeSize
import com.quadient.migration.shared.Size

sealed interface Barcode : DocumentContent, TextContent {
    val position: Position?
    val data: String?
    val barcodeFill: Color?
    val backgroundFill: Color?
    val variableRef: VariableRef?

    companion object {
        fun fromDb(entity: BarcodeEntity): Barcode = when (entity) {
            is QrCodeEntity -> QrCode.fromDb(entity)
            is Code39BarcodeEntity -> Code39Barcode.fromDb(entity)
        }
    }

    fun toDb(): BarcodeEntity
}

data class QrCode(
    override val position: Position?,
    override val data: String?,
    override val barcodeFill: Color?,
    override val backgroundFill: Color?,
    override val variableRef: VariableRef?,
    val errorCorrection: QrCodeErrorCorrectionLevel,
    val size: QrCodeSize,
    val moduleWidth: Size,
    val quiteZone: Size,
): Barcode {

    companion object {
        fun fromDb(entity: QrCodeEntity): QrCode = QrCode(
            position = entity.position,
            data = entity.data,
            errorCorrection = entity.errorCorrection,
            size = entity.size,
            moduleWidth = entity.moduleWidth,
            quiteZone = entity.quiteZone,
            barcodeFill = entity.barcodeFill,
            backgroundFill = entity.backgroundFill,
            variableRef = entity.variableRef?.let { VariableRef.fromDb(it) },
        )
    }

    override fun toDb(): QrCodeEntity = QrCodeEntity(
        position = position,
        data = data,
        errorCorrection = errorCorrection,
        size = size,
        moduleWidth = moduleWidth,
        quiteZone = quiteZone,
        barcodeFill = barcodeFill,
        backgroundFill = backgroundFill,
        variableRef = variableRef?.toDb(),
    )
}

data class Code39Barcode(
    override val position: Position?,
    override val data: String?,
    override val barcodeFill: Color?,
    override val backgroundFill: Color?,
    override val variableRef: VariableRef?,
    val height: Size,
    val useControlSum: Boolean,
    val moduleWidth: Size,
    val quietZone: Double,
    val ratio: Double = 2.0,
    val interCharacterSpaceRatio: Double = 1.0,
    val directMetric: Boolean = false,
    val firstBarWidth: Size,
    val secondBarWidth: Size,
    val firstBarSpace: Size,
    val secondBarSpace: Size,
) : Barcode {

    companion object {
        fun fromDb(entity: Code39BarcodeEntity): Code39Barcode = Code39Barcode(
            position = entity.position,
            data = entity.data,
            barcodeFill = entity.barcodeFill,
            backgroundFill = entity.backgroundFill,
            height = entity.height,
            moduleWidth = entity.moduleWidth,
            quietZone = entity.whiteSpace,
            useControlSum = entity.useControlSum,
            ratio = entity.ratio,
            interCharacterSpaceRatio = entity.interCharacterSpaceRatio,
            directMetric = entity.directMetric,
            firstBarWidth = entity.firstBarWidth,
            secondBarWidth = entity.secondBarWidth,
            firstBarSpace = entity.firstBarSpace,
            secondBarSpace = entity.secondBarSpace,
            variableRef = entity.variableRef?.let { VariableRef.fromDb(it) },
        )
    }

    override fun toDb(): Code39BarcodeEntity = Code39BarcodeEntity(
        position = position,
        data = data,
        barcodeFill = barcodeFill,
        backgroundFill = backgroundFill,
        height = height,
        moduleWidth = moduleWidth,
        whiteSpace = quietZone,
        useControlSum = useControlSum,
        ratio = ratio,
        interCharacterSpaceRatio = interCharacterSpaceRatio,
        directMetric = directMetric,
        firstBarWidth = firstBarWidth,
        secondBarWidth = secondBarWidth,
        firstBarSpace = firstBarSpace,
        secondBarSpace = secondBarSpace,
        variableRef = variableRef?.toDb(),
    )
}
