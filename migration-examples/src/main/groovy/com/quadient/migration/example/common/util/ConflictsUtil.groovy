package com.quadient.migration.example.common.util

import com.quadient.migration.api.dto.migrationmodel.ResourceId
import com.quadient.migration.service.deploy.utility.DeployedPath
import com.quadient.migration.service.deploy.utility.ValidationResult
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

static void logConflictResult(ValidationResult result) {
    def deploymentResult = result.deploymentResult

    log.info "Validation finished. ${deploymentResult.deployed.size()} items will be deployed with ${deploymentResult.warnings.size()} warnings and ${deploymentResult.errors.size()} errors"
    if (!deploymentResult.deployed.empty) {
        for (def item : deploymentResult.deployed) {
            log.info "Item ${item.type.toString()} '${item.id}' will be deployed to '${item.targetPath}'"
        }
    }

    if (!deploymentResult.warnings.empty) {
        for (def item : deploymentResult.warnings) {
            log.warn "Item '${item.id}' will be deployed with warning: ${item.message}"
        }
    }

    if (!deploymentResult.errors.empty) {
        for (def item : deploymentResult.errors) {
            log.error "Item '${item.id}' will fail to deploy with error: ${item.message}"
        }
    }

    log.error "Conflict validation found potential overwrite risks:"
    logConflictGroup("Within this deployment batch", result.conflictingInBatchResources)
    logConflictGroup("With previously deployed resources", result.conflictingWithPreviousResources)
}

private static void logConflictGroup(String title, Map<DeployedPath, Set<ResourceId>> conflicts) {
    if (!conflicts || conflicts.isEmpty()) {
        return
    }

    def lines = ["${title} (${conflicts.size()} path${conflicts.size() == 1 ? '' : 's'})"]

    conflicts.entrySet()
        .sort { a, b -> a.key.toString() <=> b.key.toString() }
        .each { entry ->
            def resources = entry.value
                .toList()
                .sort { a, b -> formatResource(a) <=> formatResource(b) }
                .collect { formatResource(it) }
                .join(', ')

            lines << "  - ${entry.key.toString()}"
            lines << "    resources: [${resources}]"
        }

    log.error(lines.join(System.lineSeparator()))
}

private static String formatResource(ResourceId resource) {
    return "${resource.type}:${resource.id}"
}