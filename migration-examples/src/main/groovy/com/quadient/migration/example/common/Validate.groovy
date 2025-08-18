//! ---
//! category: migration deploy
//! description: Run validation on all migration objects
//! ---
package com.quadient.migration.example.common

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def result = migration.referenceValidator.validateAll()
if (!result.missingRefs.isEmpty()) {
    System.err.println("Missing references: ${result.toString()}")
    System.exit(1)
}
