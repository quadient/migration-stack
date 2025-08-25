//! ---
//! category: Utils
//! description: Run validation on all migration objects
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
