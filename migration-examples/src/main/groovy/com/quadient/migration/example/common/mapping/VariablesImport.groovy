package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.shared.DataType
import com.quadient.migration.example.common.util.Csv

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def file = Paths.get("mapping", "${migration.projectConfig.name}-variables.csv").toFile()
def lines = file.readLines()
def columnNames = lines.removeFirst().split(",")

def builder = new VariableStructureBuilder("$migration.projectConfig.name-datastructure")
for (line in lines) {
    def values = Csv.getCells(line, columnNames)
    builder.structure.put(values.get("id"), values.get("inspire_path"))

    def variable = migration.variableRepository.find(values.get("id"))
    variable.name = values.get("name")
    variable.dataType = Csv.deserialize(values.get("data_type"), DataType.class)
    migration.variableRepository.upsert(variable)
}

def structure = builder.build()
for (key in structure.structure.keySet().toList()) {
    if (structure.structure.get(key) == "") {
        structure.structure.remove(key)
    }
}
migration.variableStructureRepository.upsert(structure)