//! ---
//! displayName: Delete All
//! category: Utils
//! description: Deleted all project data from database and storage
//! stdin: true
//! ---
package com.quadient.migration.example.common

import com.quadient.migration.example.common.util.UserInput

import static com.quadient.migration.example.common.util.InitMigration.initMigration

// **********************************************
//               !!!CAREFUL!!!
//      THIS WILL DELETE ALL PROJECT DATA
// **********************************************

if (!UserInput.from(this.binding).confirm("!!!CAREFUL!!! Are you really sure you want to delete all project data?")) {
    throw new IllegalStateException("Destroy declined by user, nothing was deleted")
}

println("Destroying all project data...")
def migration = initMigration(this.binding)

migration.repositories.forEach { it.deleteAll() }
migration.statusTrackingRepository.deleteAll()
migration.mappingRepository.deleteAll()
migration.storage.deleteAll()