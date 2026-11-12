package com.quadient.migration.example.common
//! ---
//! displayName: Activate Without Dependencies
//! description: Sets tracking status of the selected document objects only to active so they can be deployed again. Dependencies of the selected document objects are left untouched. Requires selected document objects in the project config.
//! category: Utils
//! ---

import com.quadient.migration.example.common.util.Activation

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

Activation.activate(migration, false, "ActivateWithoutDependencies")
