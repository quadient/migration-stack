@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.ImageModel
import com.quadient.migration.data.ParagraphStyleModel
import com.quadient.migration.data.TextStyleModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class DeployKind {
    New, Overwrite, Dependency
}

sealed class DeploymentReportItem(
    open val id: String,
    open val icmPath: String? = null,
    open val deployKind: DeployKind,
)

data class DeployedDocumentObject(
    override val id: String,
    override val icmPath: String? = null,
    override val deployKind: DeployKind,
    val documentObject: DocumentObject,
): DeploymentReportItem(id, icmPath, deployKind)

data class DeployedImage(
    override val id: String,
    override val icmPath: String? = null,
    override val deployKind: DeployKind,
    val image: Image,
): DeploymentReportItem(id, icmPath, deployKind)

data class DeployedTextStyle(
    override val id: String,
    override val icmPath: String? = null,
    override val deployKind: DeployKind,
    val style: TextStyle,
): DeploymentReportItem(id, icmPath, deployKind)

data class DeployedParagraphStyle(
    override val id: String,
    override val icmPath: String? = null,
    override val deployKind: DeployKind,
    val style: ParagraphStyle,
): DeploymentReportItem(id, icmPath, deployKind)

data class DeploymentReport(val id: Uuid, val items: MutableMap<Pair<String, ResourceType>, DeploymentReportItem>) {
    fun addDocumentObject(
        id: String,
        documentObject: DocumentObjectModel,
        icmPath: String? = null,
        deployKind: DeployKind,
    ): DeploymentReportItem {
        return items.getOrPut(Pair(documentObject.id, ResourceType.DocumentObject)) {
            DeployedDocumentObject(id, icmPath, deployKind, DocumentObject.fromModel(documentObject))
        }
    }

    fun addImage(
        id: String,
        image: ImageModel,
        icmPath: String? = null,
        deployKind: DeployKind,
    ): DeploymentReportItem {
        return items.getOrPut(Pair(id, ResourceType.Image)) {
            DeployedImage(id, icmPath, deployKind, Image.fromModel(image))
        }
    }

    fun addTextStyle(
        id: String,
        icmPath: String? = null,
        deployKind: DeployKind,
        style: TextStyleModel,
    ): DeploymentReportItem {
        return items.getOrPut(Pair(id, ResourceType.TextStyle)) {
            DeployedTextStyle(id, icmPath, deployKind, TextStyle.fromModel(style))
        }
    }

    fun addParagraphStyle(
        id: String,
        icmPath: String? = null,
        deployKind: DeployKind,
        style: ParagraphStyleModel,
    ): DeploymentReportItem {
        return items.getOrPut(Pair(id, ResourceType.ParagraphStyle)) {
            DeployedParagraphStyle(id, icmPath, deployKind, ParagraphStyle.fromModel(style))
        }
    }
}

