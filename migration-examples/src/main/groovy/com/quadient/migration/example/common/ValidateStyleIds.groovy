//! ---
//! displayName: Validate Styles of Specified Document Objects
//! description: Validates styles of document objects whose IDs are listed in the 'deploy/<project-name>-document-objects'
//! category: Utils
//! ---
package com.quadient.migration.example.common

import com.quadient.migration.example.common.util.PathUtil
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field static Logger log = LoggerFactory.getLogger(this.class.name)
def migration = initMigration(this.binding)

def documentObjects = PathUtil.dataDirPath(binding, "deploy", "${migration.projectConfig.name}-document-objects")
    .toFile()
    .text
    .lines()
    .toList()

def result = migration.stylesValidator.validate(documentObjects)
if (!result.missingParagraphStyles.empty || !result.missingTextStyles.empty) {
    log.error "Validation found missing styles: ${result.toString()}"
    System.exit(1)
} else {
    log.info "Validation found no missing styles: ${result.toString()}"
}
