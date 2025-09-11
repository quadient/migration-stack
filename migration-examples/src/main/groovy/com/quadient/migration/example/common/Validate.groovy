//! ---
//! displayName: Validate References
//! category: Utils
//! description: Validates references on all migration objects. Task fails if any referenced object is missing
//! ---
package com.quadient.migration.example.common

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)
def migration = initMigration(this.binding)

def result = migration.referenceValidator.validateAll()
if (!result.missingRefs.isEmpty()) {
    log.error "Missing references: ${result.toString()}"
    System.exit(1)
}
