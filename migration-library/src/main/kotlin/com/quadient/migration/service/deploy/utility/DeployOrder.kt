package com.quadient.migration.service.deploy.utility

import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateRef
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.tools.logger

interface DeployOrder {
    fun deployOrder(documentObjects: List<DocumentObject>): List<DocumentObject>
}

class DeployOrderImpl(private val documentObjectRepository: DocumentObjectRepository) : DeployOrder {
    val logger by logger()

    override fun deployOrder(documentObjects: List<DocumentObject>): List<DocumentObject> {
        val documentObjectIds = documentObjects.map { it.id }.toSet()

        val deployOrder = mutableListOf<DocumentObject>()

        var toCheck = documentObjects.map { obj ->
            DocObjectWithRef(obj, obj.collectAllDocumentObjectRefs().map { it.id }.toSet())
        }
        val deployed = mutableSetOf<String>()

        var lastSize = toCheck.size
        while (deployed.size < documentObjects.size) {
            val (canDeploy, cantDeploy) = toCheck.partition { docObj ->
                docObj.documentObjectRefs.all {
                    deployed.contains(it) || !documentObjectIds.contains(it)
                }
            }

            if (cantDeploy.isEmpty()) {
                for (item in canDeploy) {
                    deployOrder.add(item.obj)
                }
                return deployOrder
            }

            if (lastSize == cantDeploy.size) {
                logger.error(
                    "Cannot determine deploy order. Either circular reference or some references are missing. Deployed: ${
                        deployed.joinToString(
                            separator = ",", prefix = "'[", postfix = "]'"
                        )
                    }, Can deploy: ${
                        canDeploy.joinToString(
                            separator = ",", prefix = "'[", postfix = "]'"
                        )
                    }, Cannot deploy: ${cantDeploy.joinToString(separator = ",", prefix = "'[", postfix = "]'")}"
                )
                throw RuntimeException("Cannot determine deploy order. Either circular reference or some references are missing.")
            }

            for (item in canDeploy) {
                deployOrder.add(item.obj)
                deployed.add(item.obj.id)
            }

            lastSize = cantDeploy.size
            toCheck = cantDeploy
        }

        return deployOrder
    }

    private fun DocumentObject.collectAllDocumentObjectRefs(): Set<DocumentObjectRef> {
        val result = mutableSetOf<DocumentObjectRef>()

        val queue = this.collectRefs().toMutableList()
        val visited = mutableSetOf<Ref>()

        while (queue.isNotEmpty()) {
            val ref = queue.removeFirst()
            if (!visited.add(ref)) {
                continue
            }

            when (ref) {
                is DocumentObjectRef -> {
                    result.add(ref)
                    val obj = documentObjectRepository.find(ref.id)
                    if (obj != null) {
                        queue.addAll(obj.collectRefs())
                    }
                }
                is DisplayRuleRef -> {}
                is ParagraphStyleRef -> {}
                is AttachmentRef -> {}
                is ImageRef -> {}
                is TextStyleRef -> {}
                is VariableRef -> {}
                is VariableStructureRef -> {}
                is BaseTemplateRef -> {}
            }
        }

        return result
    }

    private data class DocObjectWithRef(val obj: DocumentObject, val documentObjectRefs: Set<String>)
}