@file:OptIn(ExperimentalUuidApi::class)

package com.quadient.migration.service.deploy.utility

import com.quadient.migration.api.InspireOutput
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.Repository
import com.quadient.migration.api.repository.StatusTrackingRepository
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.service.inspirebuilder.InspireDocumentObjectBuilder
import com.quadient.migration.shared.DocumentObjectType
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import com.quadient.migration.data.Error as StatusError

data class LastDeployment(val id: Uuid, val timestamp: Instant)

interface ProgressReporter {
    fun createProgressReport(objects: List<DocumentObject>, deployId: Uuid? = null): ProgressReport
}

class ProgressReporterImpl(
    private val documentObjectRepository: DocumentObjectRepository,
    private val imageRepository: Repository<Image>,
    private val attachmentRepository: Repository<Attachment>,
    private val displayRuleRepository: DisplayRuleRepository,
    private val documentObjectBuilder: InspireDocumentObjectBuilder,
    private val statusTrackingRepository: StatusTrackingRepository,
    private val output: InspireOutput,
) : ProgressReporter {
    override fun createProgressReport(objects: List<DocumentObject>, deployId: Uuid?): ProgressReport {
        val lastDeployment = deployId?.let { LastDeployment(it, Clock.System.now()) } ?: getLastDeployEvent()

        val report = ProgressReport(deployId ?: Uuid.random(), mutableMapOf())

        val queue: MutableList<Ref> = mutableListOf()
        val alreadyVisitedRefs = mutableSetOf<Pair<String, KClass<*>>>()

        for (obj in objects) {
            val nextIcmPath = documentObjectBuilder.getDocumentObjectPath(obj)
            val deployKind = obj.getDeployKind(nextIcmPath)
            val lastStatus = obj.getLastStatus(lastDeployment)

            report.addDocumentObject(
                id = obj.id,
                deploymentId = lastStatus.deployId,
                deployTimestamp = lastStatus.deployTimestamp,
                documentObject = obj,
                previousIcmPath = lastStatus.icmPath,
                nextIcmPath = nextIcmPath,
                lastStatus = lastStatus,
                deployKind = deployKind,
                errorMessage = lastStatus.errorMessage,
            )
            alreadyVisitedRefs.add(Pair(obj.id, DocumentObjectRef::class))
            val refs = obj.collectRefs()
            queue.addAll(refs)
        }

        while (!queue.isEmpty()) {
            val ref = queue.removeFirst()

            if (alreadyVisitedRefs.contains(Pair(ref.id, ref::class))) {
                continue
            }
            alreadyVisitedRefs.add(Pair(ref.id, ref::class))

            val resource = when (ref) {
                is DocumentObjectRef -> {
                    when (val obj = documentObjectRepository.find(ref.id)) {
                        null -> {
                            report.addDocumentObject(
                                id = ref.id,
                                documentObject = obj,
                                deploymentId = null,
                                deployTimestamp = null,
                                previousIcmPath = null,
                                nextIcmPath = null,
                                deployKind = DeployKind.NotFound,
                                lastStatus = LastStatus.None,
                                errorMessage = null,
                            )
                            null
                        }
                        else -> {
                            val nextIcmPath =
                                if (obj.internal == true || (obj.type == DocumentObjectType.Page && output == InspireOutput.Designer)) {
                                    null
                                } else {
                                    documentObjectBuilder.getDocumentObjectPath(obj)
                                }
                            val deployKind = obj.getDeployKind(nextIcmPath)
                            val lastStatus = obj.getLastStatus(lastDeployment)


                            report.addDocumentObject(
                                id = obj.id,
                                deploymentId = lastStatus.deployId,
                                deployTimestamp = lastStatus.deployTimestamp,
                                documentObject = obj,
                                previousIcmPath = lastStatus.icmPath,
                                nextIcmPath = nextIcmPath,
                                deployKind = deployKind,
                                lastStatus = lastStatus,
                                errorMessage = lastStatus.errorMessage,
                            )
                            obj
                        }
                    }
                }

                is ImageRef -> {
                    when (val img = imageRepository.find(ref.id)) {
                        null -> {
                            report.addImage(
                                id = ref.id,
                                image = img,
                                deploymentId = null,
                                deployTimestamp = null,
                                previousIcmPath = null,
                                nextIcmPath = null,
                                lastStatus = LastStatus.None,
                                deployKind = DeployKind.NotFound,
                                errorMessage = null,
                            )
                            null
                        }
                        else -> {
                            val nextIcmPath = documentObjectBuilder.getImagePath(img)
                            val deployKind = img.getDeployKind(nextIcmPath)
                            val lastStatus = img.getLastStatus(lastDeployment)

                            report.addImage(
                                id = img.id,
                                image = img,
                                deploymentId = lastStatus.deployId,
                                deployTimestamp = lastStatus.deployTimestamp,
                                previousIcmPath = lastStatus.icmPath,
                                nextIcmPath = nextIcmPath,
                                lastStatus = lastStatus,
                                deployKind = deployKind,
                                errorMessage = lastStatus.errorMessage,
                            )
                            img
                        }
                    }
                }

                is AttachmentRef -> {
                    when (val attachment = attachmentRepository.find(ref.id)) {
                        null -> {
                            report.addAttachment(
                                id = ref.id,
                                attachment = attachment,
                                deploymentId = null,
                                deployTimestamp = null,
                                previousIcmPath = null,
                                nextIcmPath = null,
                                lastStatus = LastStatus.None,
                                deployKind = DeployKind.NotFound,
                                errorMessage = null,
                            )
                            null
                        }
                        else -> {
                            val nextIcmPath = documentObjectBuilder.getAttachmentPath(attachment)
                            val deployKind = attachment.getDeployKind(nextIcmPath)
                            val lastStatus = attachment.getLastStatus(lastDeployment)

                            report.addAttachment(
                                id = attachment.id,
                                attachment = attachment,
                                deploymentId = lastStatus.deployId,
                                deployTimestamp = lastStatus.deployTimestamp,
                                previousIcmPath = lastStatus.icmPath,
                                nextIcmPath = nextIcmPath,
                                lastStatus = lastStatus,
                                deployKind = deployKind,
                                errorMessage = lastStatus.errorMessage,
                            )
                            attachment
                        }
                    }
                }

                is DisplayRuleRef -> {
                    when (output) {
                        InspireOutput.Interactive, InspireOutput.Evolve -> {
                            when (val rule = displayRuleRepository.find(ref.id)) {
                                null -> {
                                    report.addDisplayRule(
                                        id = ref.id,
                                        displayRule = rule,
                                        deploymentId = null,
                                        deployTimestamp = null,
                                        previousIcmPath = null,
                                        nextIcmPath = null,
                                        lastStatus = LastStatus.None,
                                        deployKind = DeployKind.NotFound,
                                        errorMessage = null,
                                    )
                                    null
                                }
                                else -> {
                                    val nextIcmPath = documentObjectBuilder.getDisplayRulePath(rule).toString()
                                    val deployKind = rule.getDeployKind(nextIcmPath)
                                    val lastStatus = rule.getLastStatus(lastDeployment)

                                    report.addDisplayRule(
                                        id = rule.id,
                                        displayRule = rule,
                                        deploymentId = lastStatus.deployId,
                                        deployTimestamp = lastStatus.deployTimestamp,
                                        previousIcmPath = lastStatus.icmPath,
                                        nextIcmPath = nextIcmPath,
                                        lastStatus = lastStatus,
                                        deployKind = deployKind,
                                        errorMessage = lastStatus.errorMessage,
                                    )
                                    rule
                                }
                            }
                        }
                        InspireOutput.Designer -> null
                    }
                }

                is TextStyleRef -> null
                is ParagraphStyleRef -> null
                is VariableRef -> null
                is VariableStructureRef -> null
            }

            if (resource != null) {
                val refs = resource.collectRefs()
                queue.addAll(refs)
            }
        }

        return report
    }

    private fun getLastStatus(
        id: String,
        lastDeployment: LastDeployment?,
        resourceType: ResourceType,
        output: InspireOutput,
        internal: Boolean,
        isPage: Boolean
    ): LastStatus {
        if (internal || (isPage && output == InspireOutput.Designer)) return LastStatus.Inlined

        val objectEvents = statusTrackingRepository.findEventsRelevantToOutput(id, resourceType, output)
            .filter { ev -> lastDeployment?.timestamp?.let { ev.timestamp <= it } ?: true }
        val lastEvent = objectEvents.lastOrNull()
        val lastDeployEvent = objectEvents.lastOrNull { it is Deployed || it is StatusError }

        return when (lastDeployEvent) {
            null -> {
                if (lastEvent is Active) LastStatus.None
                else error("No events tracked for $resourceType with id $id and output $output")
            }
            is StatusError -> LastStatus.Error(
                lastDeployEvent.icmPath, lastDeployEvent.deploymentId, lastDeployEvent.timestamp, lastDeployEvent.error
            )

            else -> {
                val deployEvent = lastDeployEvent as Deployed
                if (deployEvent.deploymentId != lastDeployment?.id) {
                    LastStatus.Unchanged(deployEvent.icmPath, deployEvent.deploymentId, deployEvent.timestamp)
                } else {
                    val hasPreviousSuccessfulDeploy =
                        objectEvents.any { it.timestamp < deployEvent.timestamp && it is Deployed }
                    if (hasPreviousSuccessfulDeploy) {
                        LastStatus.Overwritten(deployEvent.icmPath, deployEvent.deploymentId, deployEvent.timestamp)
                    } else {
                        LastStatus.Created(deployEvent.icmPath, deployEvent.deploymentId, deployEvent.timestamp)
                    }
                }
            }
        }
    }

    private fun getLastDeployEvent(): LastDeployment? {
        return statusTrackingRepository.listAll().mapNotNull {
            it.statusEvents.findLast { status ->
                status is Deployed || status is StatusError
            }
        }.maxByOrNull { it.timestamp }?.let {
            when (it) {
                is Deployed -> LastDeployment(it.deploymentId, it.timestamp)
                is StatusError -> LastDeployment(it.deploymentId, it.timestamp)
                is Active -> null
            }
        }
    }

    private fun DocumentObject.getLastStatus(lastDeployment: LastDeployment?): LastStatus {
        return getLastStatus(
            id = this.id,
            lastDeployment = lastDeployment,
            resourceType = ResourceType.DocumentObject,
            output = output,
            internal = this.internal ?: false,
            isPage = this.type == DocumentObjectType.Page
        )
    }

    private fun Image.getLastStatus(lastDeployment: LastDeployment?): LastStatus {
        return getLastStatus(
            id = this.id,
            lastDeployment = lastDeployment,
            resourceType = ResourceType.Image,
            output = output,
            internal = false,
            isPage = false
        )
    }

    private fun Attachment.getLastStatus(lastDeployment: LastDeployment?): LastStatus {
        return getLastStatus(
            id = this.id,
            lastDeployment = lastDeployment,
            resourceType = ResourceType.Attachment,
            output = output,
            internal = false,
            isPage = false
        )
    }

    private fun DocumentObject.getDeployKind(nextIcmPath: String?): DeployKind {
        return getDeployKind(
            this.id,
            ResourceType.DocumentObject,
            output,
            this.internal ?: false,
            nextIcmPath,
            this.type == DocumentObjectType.Page
        )
    }

    private fun Image.getDeployKind(nextIcmPath: String?): DeployKind {
        return getDeployKind(this.id, ResourceType.Image, output, false, nextIcmPath)
    }

    private fun Attachment.getDeployKind(nextIcmPath: String?): DeployKind {
        return getDeployKind(this.id, ResourceType.Attachment, output, false, nextIcmPath)
    }

    private fun getDeployKind(
        id: String,
        resourceType: ResourceType,
        output: InspireOutput,
        internal: Boolean = false,
        nextIcmPath: String?,
        isPage: Boolean = false
    ): DeployKind {
        if (internal) {
            return DeployKind.Inline
        }

        val objectEvents = statusTrackingRepository.findEventsRelevantToOutput(id, resourceType, output)
        val lastEvent = objectEvents.lastOrNull()
        return if (lastEvent is Active || lastEvent is StatusError) {
            val lastDeployEvent = objectEvents.filterIsInstance<Deployed>().lastOrNull()
            if (lastDeployEvent != null && lastDeployEvent.icmPath != nextIcmPath) {
                DeployKind.Create
            } else if (lastDeployEvent != null) {
                DeployKind.Overwrite
            } else if (isPage && output == InspireOutput.Designer) {
                DeployKind.Inline
            } else {
                DeployKind.Create
            }
        } else {
            DeployKind.Keep
        }
    }

    private fun DisplayRule.getDeployKind(nextIcmPath: String?): DeployKind {
        return getDeployKind(this.id, ResourceType.DisplayRule, output, this.internal, nextIcmPath)
    }

    private fun DisplayRule.getLastStatus(lastDeployment: LastDeployment?): LastStatus {
        return getLastStatus(
            id = this.id,
            lastDeployment = lastDeployment,
            resourceType = ResourceType.DisplayRule,
            output = output,
            internal = this.internal,
            isPage = false
        )
    }
}

enum class DeployKind {
    Create, Overwrite, Inline, Keep, NotFound
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
    val documentObject: DocumentObject?,
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
    val image: Image?,
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

data class ReportedFile(
    override val id: String,
    override val previousIcmPath: String? = null,
    override val nextIcmPath: String? = null,
    override val deployKind: DeployKind,
    override val lastStatus: LastStatus,
    override val deploymentId: Uuid?,
    override val deployTimestamp: Instant?,
    override val errorMessage: String?,
    val attachment: Attachment?,
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

data class ReportedDisplayRule(
    override val id: String,
    override val previousIcmPath: String? = null,
    override val nextIcmPath: String? = null,
    override val deployKind: DeployKind,
    override val lastStatus: LastStatus,
    override val deploymentId: Uuid?,
    override val deployTimestamp: Instant?,
    override val errorMessage: String?,
    val displayRule: DisplayRule?,
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
        documentObject: DocumentObject?,
        previousIcmPath: String? = null,
        nextIcmPath: String? = null,
        deployKind: DeployKind,
        lastStatus: LastStatus,
        deploymentId: Uuid?,
        deployTimestamp: Instant?,
        errorMessage: String?,
    ): ProgressReportItem {
        return items.getOrPut(Pair(id, ResourceType.DocumentObject)) {
            ReportedDocObject(
                id = id,
                previousIcmPath = previousIcmPath,
                nextIcmPath = nextIcmPath,
                deployKind = deployKind,
                lastStatus = lastStatus,
                deploymentId = deploymentId,
                deployTimestamp = deployTimestamp,
                documentObject = documentObject,
                errorMessage = errorMessage,
            )
        }
    }

    fun addImage(
        id: String,
        image: Image?,
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
                image = image,
                errorMessage = errorMessage,
            )
        }
    }

    fun addAttachment(
        id: String,
        attachment: Attachment?,
        previousIcmPath: String? = null,
        nextIcmPath: String? = null,
        deployKind: DeployKind,
        lastStatus: LastStatus,
        deploymentId: Uuid?,
        deployTimestamp: Instant?,
        errorMessage: String?,
    ): ProgressReportItem {
        return items.getOrPut(Pair(id, ResourceType.Attachment)) {
            ReportedFile(
                id = id,
                previousIcmPath = previousIcmPath,
                nextIcmPath = nextIcmPath,
                deployKind = deployKind,
                lastStatus = lastStatus,
                deploymentId = deploymentId,
                deployTimestamp = deployTimestamp,
                attachment = attachment,
                errorMessage = errorMessage,
            )
        }
    }

    fun addDisplayRule(
        id: String,
        displayRule: DisplayRule?,
        previousIcmPath: String? = null,
        nextIcmPath: String? = null,
        deployKind: DeployKind,
        lastStatus: LastStatus,
        deploymentId: Uuid?,
        deployTimestamp: Instant?,
        errorMessage: String?,
    ): ProgressReportItem {
        return items.getOrPut(Pair(id, ResourceType.DisplayRule)) {
            ReportedDisplayRule(
                id = id,
                previousIcmPath = previousIcmPath,
                nextIcmPath = nextIcmPath,
                deployKind = deployKind,
                lastStatus = lastStatus,
                deploymentId = deploymentId,
                deployTimestamp = deployTimestamp,
                displayRule = displayRule,
                errorMessage = errorMessage,
            )
        }
    }
}

