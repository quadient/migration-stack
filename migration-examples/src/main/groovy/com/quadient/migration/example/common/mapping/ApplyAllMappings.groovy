//! ---
//! category: Mapping
//! description: Applies all mappings
//! target: gradle
//! ---
package com.quadient.migration.example.common.mapping

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)
migration.mappingRepository.applyAll()
