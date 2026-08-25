package com.quadient.migration.example.common
//! ---
//! displayName: Activate
//! description: Sets tracking status of all assets to active so they can be deployed again
//! category: Utils
//! ---

import com.quadient.migration.service.deploy.utility.ResourceType

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def all = migration.statusTrackingRepository.listAll()
def selectedObjects = migration.projectConfig.getDocumentObjectsToProcess()
for (status in all) {
    if (!selectedObjects.empty) {
        if (status.resourceType == ResourceType.DocumentObject
            && !selectedObjects.contains(status.id.toString())) {
            continue
        }
    }

    migration.statusTrackingRepository.active(
        status.id.toString(),
        status.resourceType.toString() as ResourceType,
        [reason: "Activate"]
    )
}