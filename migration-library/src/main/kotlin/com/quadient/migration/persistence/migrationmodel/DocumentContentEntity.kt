package com.quadient.migration.persistence.migrationmodel

import com.quadient.migration.shared.BorderOptions
import com.quadient.migration.shared.CellAlignment
import com.quadient.migration.shared.CellHeight
import com.quadient.migration.shared.CellOverflow
import com.quadient.migration.shared.Color
import com.quadient.migration.shared.VariablePath
import com.quadient.migration.shared.Position
import com.quadient.migration.shared.QrCodeErrorCorrectionLevel
import com.quadient.migration.shared.QrCodeSize
import com.quadient.migration.shared.Size
import com.quadient.migration.shared.TableAlignment
import com.quadient.migration.shared.TableAction
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import com.quadient.migration.shared.TablePdfTaggingRule

@Serializable
sealed interface DocumentContentEntity

@Serializable
data class TableEntity(
    val rows: List<TableRow>,
    val header: List<TableRow> = emptyList(),
    val firstHeader: List<TableRow> = emptyList(),
    val footer: List<TableRow> = emptyList(),
    val lastFooter: List<TableRow> = emptyList(),
    val columnWidths: List<ColumnWidthEntity>,
    val pdfTaggingRule: TablePdfTaggingRule = TablePdfTaggingRule.Default,
    val pdfAlternateText: String? = null,
    val minWidth: Size? = null,
    val maxWidth: Size? = null,
    val percentWidth: Double? = null,
    val border: BorderOptions? = null,
    val alignment: TableAlignment = TableAlignment.Left,
    val tableStyleName: String? = null,
    val action: TableAction = TableAction.Keep,
) : DocumentContentEntity, TextContentEntity {
    @Serializable(with = TableRowEntitySerializer::class)
    sealed interface TableRow

    @Serializable
    data class Row(val cells: List<Cell>, val displayRuleRef: DisplayRuleEntityRef? = null) : TableRow

    @Serializable
    data class RepeatedRow(
        val rows: List<TableRow>,
        val variable: VariablePath,
        val displayRuleRef: DisplayRuleEntityRef? = null,
    ) : TableRow

    @Serializable
    data class Cell(
        val content: List<DocumentContentEntity>,
        val mergeLeft: Boolean,
        val mergeUp: Boolean,
        val height: CellHeight? = null,
        val border: BorderOptions? = null,
        val alignment: CellAlignment? = null,
        val overflow: CellOverflow? = null,
    )

    @Serializable
    data class ColumnWidthEntity(val minWidth: Size, val percentWidth: Double)
}

object TableRowEntitySerializer : KSerializer<TableEntity.TableRow> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TableEntity.TableRow")

    override fun deserialize(decoder: Decoder): TableEntity.TableRow {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("TableEntity.TableRow can only be deserialized from JSON")
        val element = jsonDecoder.decodeJsonElement().jsonObject
        return when {
            "cells" in element -> jsonDecoder.json.decodeFromJsonElement<TableEntity.Row>(element)
            "rows" in element -> jsonDecoder.json.decodeFromJsonElement<TableEntity.RepeatedRow>(element)
            else -> error("Cannot determine TableRow type: neither 'cells' nor 'rows' field present")
        }
    }

    override fun serialize(encoder: Encoder, value: TableEntity.TableRow) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("TableEntity.TableRow can only be serialized to JSON")
        val element = when (value) {
            is TableEntity.Row -> jsonEncoder.json.encodeToJsonElement(value)
            is TableEntity.RepeatedRow -> jsonEncoder.json.encodeToJsonElement(value)
        }
        jsonEncoder.encodeJsonElement(element)
    }
}

@Serializable
data class ParagraphEntity(
    val content: MutableList<TextEntity>,
    val styleRef: ParagraphStyleEntityRef?,
    val displayRuleRef: DisplayRuleEntityRef?
) : DocumentContentEntity {
    @Serializable
    data class TextEntity(
        val content: MutableList<TextContentEntity>,
        val styleRef: TextStyleEntityRef?,
        val displayRuleRef: DisplayRuleEntityRef?
    )
}

@Serializable
data class AreaEntity(
    val content: List<DocumentContentEntity>, val position: Position?, val interactiveFlowName: String?, val flowToNextPage: Boolean = false
) : DocumentContentEntity

@Serializable
data class RepeatedContentEntity(
    val variablePath: VariablePath,
    val content: List<DocumentContentEntity>,
) : DocumentContentEntity


@Serializable
sealed interface BarcodeEntity : DocumentContentEntity, TextContentEntity {
    val position: Position?
    val data: String?
    val barcodeFill: Color?
    val backgroundFill: Color?
    val variableRef: VariableEntityRef?
}

@Serializable
data class QrCodeEntity(
    override val position: Position?,
    override val data: String?,
    override val barcodeFill: Color?,
    override val backgroundFill: Color?,
    override val variableRef: VariableEntityRef?,
    val errorCorrection: QrCodeErrorCorrectionLevel,
    val size: QrCodeSize,
    val moduleWidth: Size,
    val quiteZone: Size,
): BarcodeEntity

@Serializable
data class Code39BarcodeEntity(
    override val position: Position?,
    override val data: String?,
    override val barcodeFill: Color?,
    override val backgroundFill: Color?,
    override val variableRef: VariableEntityRef?,
    val height: Size,
    val moduleWidth: Size,
    val whiteSpace: Double,
    val useControlSum: Boolean,
    val ratio: Double,
    val interCharacterSpaceRatio: Double,
    val directMetric: Boolean,
    val firstBarWidth: Size,
    val secondBarWidth: Size,
    val firstBarSpace: Size,
    val secondBarSpace: Size,
): BarcodeEntity

