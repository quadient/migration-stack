package com.quadient.migration.example.common.mapping

import com.quadient.migration.example.common.util.Mapping

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def selectedFilePath = new Mapping().getVariablesMappingPath()
def structureName = selectedFilePath.fileName.toString().split("\\.")[0]

migration.mappingRepository.applyAll(structureName)
