//! ---
//! displayName: Validate Conflict Ids
//! category: Utils
//! description: Validate that no object will overwrite an existing object in the target environment.
//! ---
package com.quadient.migration.example.common

import com.quadient.migration.example.common.util.ConflictsUtil
import com.quadient.migration.example.common.util.PathUtil
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)

try {
    def documentObjects = PathUtil.dataDirPath(binding, "deploy", "${migration.projectConfig.name}-document-objects")
        .toFile()
        .text
        .lines()
        .toList()
    log.info "Loaded ${documentObjects.size()} document object IDs for conflict validation"

    def result = migration.deployClient.validateConflicts(documentObjects)

    if (result.hasNoConflicts()) {
        log.info "No conflicts detected. Safe to deploy."
        return
    }

    ConflictsUtil.logConflictResult(result)
    System.exit(1)
} catch (Exception e) {
    log.error "Validation failed with error: ${e.message}"
    System.exit(1)
}
