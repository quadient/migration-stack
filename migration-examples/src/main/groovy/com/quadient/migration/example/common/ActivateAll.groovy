package com.quadient.migration.example.common

import com.quadient.migration.service.deploy.ResourceType

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def all = migration.statusTrackingRepository.listAll()
for (status in all) {
    migration.statusTrackingRepository.active(
        status.id.toString(),
        status.resourceType.toString() as ResourceType,
        [reason: "ActivateAll"]
    )
}