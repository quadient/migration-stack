package com.quadient.migration.service.inspirebuilder

import com.quadient.wfdxml.api.layoutnodes.Flow
import com.quadient.wfdxml.api.layoutnodes.Font
import com.quadient.wfdxml.api.layoutnodes.Image
import com.quadient.wfdxml.api.layoutnodes.data.DataType
import com.quadient.wfdxml.api.layoutnodes.data.Variable
import com.quadient.wfdxml.api.module.Layout
import com.quadient.wfdxml.internal.Group
import com.quadient.wfdxml.internal.layoutnodes.FlowImpl
import com.quadient.wfdxml.internal.layoutnodes.FontImpl
import com.quadient.wfdxml.internal.layoutnodes.ImageImpl
import com.quadient.wfdxml.internal.layoutnodes.data.DataImpl
import com.quadient.wfdxml.internal.layoutnodes.data.VariableImpl
import com.quadient.wfdxml.internal.module.layout.LayoutImpl
import com.quadient.migration.shared.DataType as DataTypeModel

fun getDataType(dataType: DataTypeModel): DataType {
    return when (dataType) {
        DataTypeModel.DateTime -> DataType.DATE_TIME
        DataTypeModel.Integer -> DataType.INT
        DataTypeModel.Integer64 -> DataType.INT64
        DataTypeModel.Double -> DataType.DOUBLE
        DataTypeModel.String -> DataType.STRING
        DataTypeModel.Boolean -> DataType.BOOL
        DataTypeModel.Currency -> DataType.CURRENCY
    }
}

fun removeDataFromVariablePath(path: String): String = if (path == "Data") "" else path.removePrefix("Data.")

fun removeValueFromVariablePath(path: String): String {
    return path.split(".").mapNotNull {
        when (it.lowercase()) {
            "value" -> null
            else -> it
        }
    }.joinToString(".")
}

sealed class VariablePathPart(val name: String) {
    val children = mutableMapOf<String, VariablePathPart>()
}

class ArrayVariable(name: String) : VariablePathPart(name)
class SubtreeVariable(name: String) : VariablePathPart(name)

fun buildVariableTree(paths: List<String>): Map<String, VariablePathPart> {
    val root = mutableMapOf<String, VariablePathPart>()

    for (path in paths) {
        val parts = path.split(".")
        var currentMap = root

        var i = 0
        while (i < parts.size) {
            val part = parts[i]

            if (part.lowercase() == "value") {
                i++
                continue
            }

            val isArray = (i + 1 < parts.size && parts[i + 1].lowercase() == "value")
            val variable = currentMap.getOrPut(part) {
                if (isArray) ArrayVariable(part) else SubtreeVariable(part)
            }

            currentMap = variable.children
            i++
        }
    }

    return root
}

fun getImageByName(layout: Layout, imageName: String?): Image? {
    val imageGroup = (layout as LayoutImpl).children.find { it.name == "Images" } as Group
    return imageGroup.children.find { (it as ImageImpl).name == imageName } as? Image
}

fun getFlowByName(layout: Layout, flowName: String?): Flow? {
    val flowGroup =
        (layout as LayoutImpl).children.find { it.name == "Flows" } as Group
    return flowGroup.children.find { (it as FlowImpl).name == flowName } as? Flow
}

fun getFontByName(layout: Layout, fontName: String): Font? {
    val fontGroup = (layout as LayoutImpl).children.find { it.name == "Fonts" } as Group
    return fontGroup.children.find { (it as FontImpl).name == fontName } as? Font
}

fun getVariable(data: DataImpl, name: String, parentPath: String): Variable? {
    return (data.children).find {
        val variable = it as VariableImpl
        variable.name == name && variable.existingParentId == parentPath
    } as? Variable
}

fun buildFontName(bold: Boolean, italic: Boolean): String {
    if (bold && italic) {
        return "Bold Italic"
    }

    if (bold) {
        return "Bold"
    }

    if (italic) {
        return "Italic"
    }

    return "Regular"
}

data class FontKey(
    val fontName: String,
    val subFontName: String,
)