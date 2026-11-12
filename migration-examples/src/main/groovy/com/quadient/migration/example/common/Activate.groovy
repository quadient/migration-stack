package com.quadient.migration.example.common
//! ---
//! displayName: Activate
//! description: Sets tracking status of assets to active so they can be deployed again. With selected document objects, activates the selected document objects and all their dependencies. Without selection, activates all assets.
//! category: Utils
//! ---

import com.quadient.migration.example.common.util.Activation

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

Activation.activate(migration, true, "Activate")
