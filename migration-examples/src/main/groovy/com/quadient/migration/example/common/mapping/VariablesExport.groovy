package com.quadient.migration.example.common.mapping

import com.quadient.migration.example.common.util.Csv

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def dstFile = Paths.get("mapping", "${migration.projectConfig.name}-variables.csv")

def variables = migration.variableRepository.listAll()
def existingStructure = migration
        .variableStructureRepository
        .find("$migration.projectConfig.name-datastructure")

def file = dstFile.toFile()
file.createParentDirectories()
file.withWriter { writer ->
    writer.writeLine("id,name,inspire_path,data_type")

    for (variable in variables) {
        def path = ""
        if (existingStructure && existingStructure.structure.containsKey(variable.id)) {
            path = existingStructure.structure.get(variable.id)
        }
        writer.writeLine("${Csv.serialize(variable.id)},${Csv.serialize(variable.name)},${path},${Csv.serialize(variable.dataType)}")
    }
}