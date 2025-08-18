//! ---
//! category: migration mapping
//! description: Deploy style definition
//! ---
package com.quadient.migration.example.common

import static com.quadient.migration.example.common.util.InitMigration.initMigration

initMigration(this.binding.variables["args"]).deployClient.deployStyles()