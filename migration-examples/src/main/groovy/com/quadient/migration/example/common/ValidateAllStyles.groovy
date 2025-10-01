package com.quadient.migration.example.common
//! ---
//! displayName: Validate Styles of All Document Objects
//! description: Validates all styles in the migration project
//! category: Utils
//! ---

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)
def migration = initMigration(this.binding)

def result = migration.stylesValidator.validateAll()
if (!result.missingParagraphStyles.empty || !result.missingTextStyles.empty) {
    log.error "Validation found missing styles: ${result.toString()}"
    System.exit(1)
} else {
    log.info "Validation found no missing styles: ${result.toString()}"
}