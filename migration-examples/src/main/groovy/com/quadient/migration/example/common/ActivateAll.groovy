package com.quadient.migration.example.common
//! ---
//! displayName: Activate All
//! description: Sets tracking status of all assets to active so they can be deployed again
//! category: Utils
//! ---

import com.quadient.migration.service.deploy.ResourceType

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def all = migration.statusTrackingRepository.listAll()
for (status in all) {
    migration.statusTrackingRepository.active(
        status.id.toString(),
        status.resourceType.toString() as ResourceType,
        [reason: "ActivateAll"]
    )
}