package com.quadient.migration.example.common

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def documentObjects = Paths.get("deploy", "${migration.projectConfig.name}-document-objects")
        .toFile()
        .text
        .lines()
        .toList()

migration.deployClient.deployDocumentObjects(documentObjects, false)