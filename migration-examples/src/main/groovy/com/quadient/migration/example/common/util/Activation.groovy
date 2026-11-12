package com.quadient.migration.example.common.util

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Attachment
import com.quadient.migration.api.dto.migrationmodel.BaseTemplate
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.service.deploy.utility.ResourceType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Activation {
    private static final Logger log = LoggerFactory.getLogger(Activation.class)

    static int activate(Migration migration, boolean includeDependencies, String reason) {
        def selectedIds = migration.projectConfig.getDocumentObjectsToProcess()
        def all = migration.statusTrackingRepository.listAll()

        if (selectedIds.empty) {
            if (!includeDependencies) {
                throw new IllegalStateException("No document objects are selected. Set selectedDocumentObjects or " +
                    "selectedDocumentObjectsFile in the project config, or use the Activate task to activate everything.")
            }
            def activatedCount = all.count { activateIfNotActive(migration, it.id, it.resourceType, reason) } as int
            log.info("No document objects selected, activated ${activatedCount} of ${all.size()} tracked resources " +
                "(the rest was already active).")
            return activatedCount
        }

        Set<String> toActivate = selectedIds.collect { key(ResourceType.DocumentObject, it) } as Set<String>
        if (includeDependencies) {
            collectDependencies(migration, selectedIds).each { toActivate.add(it) }
        }

        def activatedCount = 0
        Set<String> tracked = new HashSet<>()
        for (status in all) {
            def statusKey = key(status.resourceType, status.id)
            tracked.add(statusKey)
            if (toActivate.contains(statusKey) && activateIfNotActive(migration, status.id, status.resourceType, reason)) {
                activatedCount++
            }
        }

        def untrackedSelected = selectedIds.findAll { !tracked.contains(key(ResourceType.DocumentObject, it)) }
        if (!untrackedSelected.empty) {
            log.warn("Selected document objects without tracked status were skipped: ${untrackedSelected.join(", ")}")
        }
        log.info("Activated ${activatedCount} tracked resources for ${selectedIds.size()} selected document objects" +
            (includeDependencies ? " and their dependencies." : " (dependencies not included)."))
        return activatedCount
    }

    // Already active resources are skipped so repeated runs do not stack duplicate active statuses
    private static boolean activateIfNotActive(Migration migration, String id, ResourceType type, String reason) {
        return migration.statusTrackingRepository.activeIfNotActive(id, type, [reason: reason]) != null
    }

    private static Set<String> collectDependencies(Migration migration, List<String> selectedIds) {
        def selected = migration.documentObjectRepository.listIds(selectedIds)
        Set<MigrationObject> objects = new LinkedHashSet<>(selected)
        selected.each { objects.addAll(migration.referenceCollector.collectAllObjects(it)) }

        Set<String> keys = new HashSet<>()
        for (obj in objects) {
            def type = resourceTypeOf(obj)
            if (type != null) {
                keys.add(key(type, obj.id))
            }
        }

        def defaultVariableStructure = migration.projectConfig.defaultVariableStructure
        def usesDefaultStructure = objects.any { it instanceof DocumentObject && it.variableStructureRef == null }
        if (defaultVariableStructure && usesDefaultStructure) {
            keys.add(key(ResourceType.VariableStructure, defaultVariableStructure))
        }

        return keys
    }

    private static ResourceType resourceTypeOf(MigrationObject obj) {
        switch (obj) {
            case DocumentObject: return ResourceType.DocumentObject
            case Image: return ResourceType.Image
            case Attachment: return ResourceType.Attachment
            case TextStyle: return ResourceType.TextStyle
            case ParagraphStyle: return ResourceType.ParagraphStyle
            case DisplayRule: return ResourceType.DisplayRule
            case BaseTemplate: return ResourceType.BaseTemplate
            case VariableStructure: return ResourceType.VariableStructure
            default: return null // e.g. variables, which are not tracked
        }
    }

    private static String key(ResourceType type, String id) {
        return "${type.name()}:${id}".toString()
    }
}
