//! ---
//! category: Utils
//! description: Export document object ids to deploy to a file
//! ---
package com.quadient.migration.example.common

import com.quadient.migration.example.common.util.PathUtil
import com.quadient.migration.shared.DocumentObjectType

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)

def objects = migration.documentObjectRepository
        .listAll()
        .findAll { !it.internal && it.type != DocumentObjectType.Unsupported }

def documentObjFile = PathUtil.dataDirPath(binding, "deploy", "${migration.projectConfig.name}-document-objects").toFile()

documentObjFile.createParentDirectories()

documentObjFile.withWriter { writer ->
    for (obj in objects) {
        writer.writeLine(obj.id)
    }
}