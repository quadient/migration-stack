package com.quadient.migration.service.deploy.utility

import com.quadient.migration.api.dto.migrationmodel.BaseTemplateLocation
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.repository.DocumentObjectRepository

interface RefInheritanceService {
    fun apply(documentObjects: List<DocumentObject>): List<DocumentObject>
}

class RefInheritanceServiceImpl(
    private val documentObjectRepository: DocumentObjectRepository,
) : RefInheritanceService {
    override fun apply(documentObjects: List<DocumentObject>): List<DocumentObject> {
        val parentsById = mutableMapOf<String, MutableList<String>>()
        for (obj in documentObjectRepository.listAll()) {
            for (ref in obj.collectRefs()) {
                if (ref is DocumentObjectRef) {
                    parentsById.getOrPut(ref.id) { mutableListOf() }.add(obj.id)
                }
            }
        }

        val baseTemplateCache = mutableMapOf<String, BaseTemplateLocation?>()
        val variableStructureCache = mutableMapOf<String, VariableStructureRef?>()

        fun resolveBaseTemplate(id: String, visiting: MutableSet<String>): BaseTemplateLocation? {
            baseTemplateCache[id]?.let { return it }
            if (!visiting.add(id)) return null

            val obj = documentObjectRepository.find(id)
            val resolved = obj?.baseTemplate ?: parentsById[id]?.firstNotNullOfOrNull { parentId ->
                resolveBaseTemplate(parentId, visiting)
            }

            visiting.remove(id)
            if (resolved != null) baseTemplateCache[id] = resolved
            return resolved
        }

        fun resolveVariableStructureRef(id: String, visiting: MutableSet<String>): VariableStructureRef? {
            variableStructureCache[id]?.let { return it }
            if (!visiting.add(id)) return null

            val obj = documentObjectRepository.find(id)
            val resolved = obj?.variableStructureRef ?: parentsById[id]?.firstNotNullOfOrNull { parentId ->
                resolveVariableStructureRef(parentId, visiting)
            }

            visiting.remove(id)
            if (resolved != null) variableStructureCache[id] = resolved
            return resolved
        }

        return documentObjects.map { obj ->
            val effectiveBaseTemplate = obj.baseTemplate ?: resolveBaseTemplate(obj.id, mutableSetOf())
            val effectiveVariableStructureRef = obj.variableStructureRef
                ?: resolveVariableStructureRef(obj.id, mutableSetOf())

            obj.copy(
                baseTemplate = effectiveBaseTemplate,
                variableStructureRef = effectiveVariableStructureRef,
            )
        }
    }
}
