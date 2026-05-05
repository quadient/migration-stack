//! ---
//! displayName: Validate Conflicts
//! category: Utils
//! description: Validate that no object will overwrite an existing object in the target environment.
//! ---
package com.quadient.migration.example.common

import com.quadient.migration.example.common.util.ConflictsUtil
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)

def result = migration.deployClient.validateConflicts()

if (result.hasNoConflicts()) {
    log.info "No conflicts detected. Safe to deploy."
    return
}

ConflictsUtil.logConflictResult(result)
throw new RuntimeException("Conflict validation failed, check logs for details")