//! ---
//! displayName: Parse DOCX and Apply Mapping
//! category: Parser
//! description: Parses input DOCX files specified in the project settings, translates their contents into the migration model, stores the resulting objects in the database and applies persisted mappings.
//! sourceFormat: DOCX
//! ---
package com.quadient.migration.example.docx

import static com.quadient.migration.example.common.util.InitMigration.initMigration
import static com.quadient.migration.example.docx.parser.DocxTemplateParser.parseDocxFiles

def migration = initMigration(this.binding)

println("\nStarting Parse step...\n")
parseDocxFiles(migration)
println("\nApplying persisted mapping...\n")
migration.mappingRepository.applyAll()
