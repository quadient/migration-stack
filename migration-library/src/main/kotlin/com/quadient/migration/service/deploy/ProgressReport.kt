@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy

import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.data.DocumentObjectModel
import com.quadient.migration.data.ImageModel
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class DeployKind {
    Create, Overwrite, Inline, Keep
}

sealed class LastStatus {
    data class Created(
        override val icmPath: String?,
        override val deployId: Uuid?,
        override val deployTimestamp: Instant?
    ) : LastStatus()

    data class Overwritten(
        override val icmPath: String?,
        override val deployId: Uuid?,
        override val deployTimestamp: Instant?
    ) : LastStatus()

    data class Unchanged(
        override val icmPath: String?,
        override val deployId: Uuid?,
        override val deployTimestamp: Instant?
    ) : LastStatus()

    object Inlined : LastStatus()
    data class Error(
        override val icmPath: String?,
        override val deployId: Uuid?,
        override val deployTimestamp: Instant?,
        override val errorMessage: String?
    ) : LastStatus()

    object None : LastStatus()

    open val icmPath: String?
        get() = when (this) {
            is Created -> icmPath
            is Overwritten -> icmPath
            is Unchanged -> icmPath
            is Error -> icmPath
            is Inlined -> null
            None -> null
        }

    open val deployId: Uuid?
        get() = when (this) {
            is Created -> deployId
            is Overwritten -> deployId
            is Unchanged -> deployId
            is Error -> deployId
            is Inlined -> null
            None -> null
        }

    open val deployTimestamp: Instant?
        get() = when (this) {
            is Created -> deployTimestamp
            is Overwritten -> deployTimestamp
            is Unchanged -> deployTimestamp
            is Error -> deployTimestamp
            is Inlined -> null
            None -> null
        }

    open val errorMessage: String?
        get() = when (this) {
            is Error -> errorMessage
            else -> null
        }
}

sealed class ProgressReportItem(
    open val id: String,
    open val previousIcmPath: String? = null,
    open val nextIcmPath: String? = null,
    open val deployKind: DeployKind,
    open val lastStatus: LastStatus,
    open val deploymentId: Uuid?,
    open val deployTimestamp: Instant?,
    open val errorMessage: String?,
)

data class ReportedDocObject(
    override val id: String,
    override val previousIcmPath: String? = null,
    override val nextIcmPath: String? = null,
    override val deployKind: DeployKind,
    override val lastStatus: LastStatus,
    override val deploymentId: Uuid?,
    override val deployTimestamp: Instant?,
    override val errorMessage: String?,
    val documentObject: DocumentObject,
) : ProgressReportItem(
    id,
    previousIcmPath,
    nextIcmPath,
    deployKind,
    lastStatus,
    deploymentId,
    deployTimestamp,
    errorMessage
)

data class ReportedImage(
    override val id: String,
    override val previousIcmPath: String? = null,
    override val nextIcmPath: String? = null,
    override val deployKind: DeployKind,
    override val lastStatus: LastStatus,
    override val deploymentId: Uuid?,
    override val deployTimestamp: Instant?,
    override val errorMessage: String?,
    val image: Image,
) : ProgressReportItem(
    id,
    previousIcmPath,
    nextIcmPath,
    deployKind,
    lastStatus,
    deploymentId,
    deployTimestamp,
    errorMessage
)

data class ProgressReport(val id: Uuid?, val items: MutableMap<Pair<String, ResourceType>, ProgressReportItem>) {
    fun addDocumentObject(
        id: String,
        documentObject: DocumentObjectModel,
        previousIcmPath: String? = null,
        nextIcmPath: String? = null,
        deployKind: DeployKind,
        lastStatus: LastStatus,
        deploymentId: Uuid?,
        deployTimestamp: Instant?,
        errorMessage: String?,
    ): ProgressReportItem {
        return items.getOrPut(Pair(documentObject.id, ResourceType.DocumentObject)) {
            ReportedDocObject(
                id = id,
                previousIcmPath = previousIcmPath,
                nextIcmPath = nextIcmPath,
                deployKind = deployKind,
                lastStatus = lastStatus,
                deploymentId = deploymentId,
                deployTimestamp = deployTimestamp,
                documentObject = DocumentObject.fromModel(documentObject),
                errorMessage = errorMessage,
            )
        }
    }

    fun addImage(
        id: String,
        image: ImageModel,
        previousIcmPath: String? = null,
        nextIcmPath: String? = null,
        deployKind: DeployKind,
        lastStatus: LastStatus,
        deploymentId: Uuid?,
        deployTimestamp: Instant?,
        errorMessage: String?,
    ): ProgressReportItem {
        return items.getOrPut(Pair(id, ResourceType.Image)) {
            ReportedImage(
                id = id,
                previousIcmPath = previousIcmPath,
                nextIcmPath = nextIcmPath,
                deployKind = deployKind,
                lastStatus = lastStatus,
                deploymentId = deploymentId,
                deployTimestamp = deployTimestamp,
                image = Image.fromModel(image),
                errorMessage = errorMessage,
            )
        }
    }
}

