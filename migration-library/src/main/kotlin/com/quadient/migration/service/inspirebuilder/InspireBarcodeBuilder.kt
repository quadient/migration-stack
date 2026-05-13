package com.quadient.migration.service.inspirebuilder

import com.quadient.migration.api.dto.migrationmodel.Barcode
import com.quadient.migration.api.dto.migrationmodel.Code39Barcode
import com.quadient.migration.api.dto.migrationmodel.QrCode
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.shared.Color
import com.quadient.wfdxml.api.layoutnodes.BarcodeFactory
import com.quadient.wfdxml.api.layoutnodes.FillStyle
import com.quadient.wfdxml.api.layoutnodes.data.Variable
import com.quadient.wfdxml.api.module.Layout

fun Barcode.buildContent(
    variableRepository: VariableRepository,
    barcodeFactory: BarcodeFactory,
    layout: Layout,
    variableStructure: VariableStructure,
    inline: Boolean,
): com.quadient.wfdxml.api.layoutnodes.Barcode<*>? {
    val barcode = when (val model = this) {
        is QrCode -> {
            val code = barcodeFactory
                .addQr()
                .setData(model.data)
                .setModuleWidth(model.moduleWidth.toMeters())
                .setWhiteSpace(model.quiteZone.toMeters())
                .setErrorCorrection(model.errorCorrection.inspireValue)
                .setBarcodeFill(model.barcodeFill?.resolve(layout))
                .setBackgroundFill(model.backgroundFill?.resolve(layout))
                .setShowDataTextProcessed(true)
                .setVariable(model.variableRef?.resolve(layout, variableRepository, variableStructure))

            if (!inline && model.position != null) {
                code.setPosX(model.position.x.toMeters())
                code.setPosY(model.position.y.toMeters())
                code.setWidth(model.position.width.toMeters())
                code.setHeight(model.position.height.toMeters())
            }

            if (model.size.inspireValue != null) {
                code.setBarcodeSize(model.size.inspireValue)
            }

            code
        }

        is Code39Barcode -> {
            val code = barcodeFactory
                .addCode39()
                .setData(model.data)
                .setBarcodeHeight(model.height.toMeters())
                .setModuleWidth(model.moduleWidth.toMeters())
                .setWhiteSpace(model.quietZone)
                .useControlSum(model.useControlSum)
                .setRatio(model.ratio)
                .setInterCharacterSpaceRatio(model.interCharacterSpaceRatio)
                .setDirectMetric(model.directMetric)
                .setFirstBarWidth(model.firstBarWidth.toMeters())
                .setSecondBarWidth(model.secondBarWidth.toMeters())
                .setFirstBarSpace(model.firstBarSpace.toMeters())
                .secondBarSpace(model.secondBarSpace.toMeters())
                .setBarcodeFill(model.barcodeFill?.resolve(layout))
                .setBackgroundFill(model.backgroundFill?.resolve(layout))
                .setVariable(model.variableRef?.resolve(layout, variableRepository, variableStructure))

            if (!inline && model.position != null) {
                code.setPosX(model.position.x.toMeters())
                code.setPosY(model.position.y.toMeters())
                code.setWidth(model.position.width.toMeters())
                code.setHeight(model.position.height.toMeters())
            }

            code
        }

    }

    return barcode
}

private fun Color.resolve(layout: Layout): FillStyle? {
    val layoutColor = getColorByRGB(layout, this.red(), this.green(), this.blue())
        ?: layout.addColor().setRGB(this.red(), this.green(), this.blue())

    return getFillStyleByColor(layout, layoutColor) ?: layout.addFillStyle().setColor(layoutColor)
}

private fun VariableRef.resolve(layout: Layout, variableRepository: VariableRepository, variableStructure: VariableStructure): Variable {
    val variableModel = variableRepository.findOrFail(this.id)
    val variablePath = variableStructure.structure[this.id]?.path?.resolve(
        variableStructure,
        variableRepository::findOrFail
    )?.takeIf { it.isNotBlank() }
    if (variablePath.isNullOrBlank()) {
        error("Variable reference with id '${this.id}' used in Barcode does not resolve to a valid variable path.")
    }
    return getOrCreateVariable(layout.data, variableModel.nameOrId(), variableModel, variablePath)
}
