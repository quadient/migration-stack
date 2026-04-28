//! ---
//! displayName: Activate Ids
//! description: Sets tracking status of selected assets to active so they can be deployed again
//! category: Utils
//! ---
package com.quadient.migration.example.common

import com.quadient.migration.example.common.util.PathUtil
import com.quadient.migration.service.deploy.utility.ResourceType

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def documentObjects = PathUtil.dataDirPath(binding, "deploy", "${migration.projectConfig.name}-document-objects")
    .toFile()
    .text
    .lines()
    .toList()

for (id in documentObjects) {
    migration.statusTrackingRepository.active(id, ResourceType.DocumentObject, [reason: "ActivateIds"])
}
