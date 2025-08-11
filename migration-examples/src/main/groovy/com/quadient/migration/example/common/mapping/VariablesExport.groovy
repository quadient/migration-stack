package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.example.common.util.Csv

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def dstFile = Paths.get("mapping", "${migration.projectConfig.name}-variables.csv")

run(migration, dstFile)

static void run(Migration migration, Path dstFile) {
    def variables = migration.variableRepository.listAll()
    def existingStructure = migration
            .variableStructureRepository
            .find("$migration.projectConfig.name-datastructure")

    def file = dstFile.toFile()
    file.createParentDirectories()
    file.withWriter { writer ->
        writer.writeLine("id,name,origin_locations,inspire_path,data_type")

        for (variable in variables) {
            def mapping = migration.mappingRepository.getVariableMapping(variable.id)

            writer.write("${Csv.serialize(variable.id)},")
            writer.write("${Csv.serialize(mapping?.name ?: variable.name)},")
            writer.write("${Csv.serialize(variable.originLocations)},")
            writer.write("${Csv.serialize(mapping?.inspirePath ?: existingStructure?.structure?.get(variable.id) ?: "")},")
            writer.write("${Csv.serialize(mapping?.dataType ?: variable.dataType)}")
            writer.writeLine("")
        }
    }
}