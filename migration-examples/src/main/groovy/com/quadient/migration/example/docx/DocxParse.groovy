//! ---
//! displayName: Parse DOCX
//! category: Parser
//! description: Parses input DOCX files specified in the project settings, translates their contents into the migration model and stores the resulting objects in the database. Persisted mappings are not applied.
//! sourceFormat: DOCX
//! ---
package com.quadient.migration.example.docx

import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.quadient.migration.example.common.util.InitMigration.initMigration
import static com.quadient.migration.example.docx.parser.DocxTemplateParser.parseDocxFiles

def migration = initMigration(this.binding)
@Field static Logger log = LoggerFactory.getLogger(this.class.name)

log.info "\nStarting Parse step...\n"
parseDocxFiles(migration)
log.info "\nApplying persisted mapping...\n"
migration.mappingRepository.applyAll()
