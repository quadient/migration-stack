//! ---
//! displayName: Export Variable Structure
//! category: Mapping
//! description: Creates a CSV report with variable structure from the migration project. The export is interactive, prompting the user to select or enter a variable structure to export.
//! target: gradle
//! stdin: true
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration
import static com.quadient.migration.example.common.util.ScriptArgs.getValueOfArg

def migration = initMigration(this.binding)

def argUserInput = (getValueOfArg("--variable-structure-id", this.binding.variables["args"] as List<String>)).orElseGet { null }

def forbiddenChars = /[\\\/:\*\?"<>\|]/

def variableStructures = migration.variableStructureRepository.listAll()
def defaultVariableId = constructDefaultId(variableStructures)

if (!argUserInput) {
    if (variableStructures.isEmpty()) {
        println "No existing variable structures found."
        println "Type an id of new variable structure (leave empty for: ${defaultVariableId}):"
    } else {
        println "Existing variable structures for export:"
        variableStructures.eachWithIndex { variableStructure, i -> println "${i + 1}) ${variableStructure.id}" }
        println "Either select a number of an existing variable structure, or type an id for a new variable structure (leave empty for: ${defaultVariableId}):"
    }
}

def variableStructureId
def selectedVariableStructure
def userInput

while (true) {
    userInput = argUserInput ?: System.in.newReader().readLine().trim()
    if (!userInput) {
        variableStructureId = defaultVariableId
        println "No input provided. Generated new variable structure : $variableStructureId"
        break
    } else if (userInput.isInteger()) {
        def i = userInput.toInteger() - 1
        if (i < 0 || i >= variableStructures.size()) {
            println "Invalid selection. Please enter a valid number:"
            continue
        }
        selectedVariableStructure = variableStructures[i]
        variableStructureId = selectedVariableStructure.id
        println "Selected variable structure: $variableStructureId"
        break
    } else {
        if (userInput =~ forbiddenChars) {
            println "Invalid id. Please avoid using these characters: \\ / : * ? \" < > |"
            continue
        }
        variableStructureId = userInput
        break
    }
}

def fileName = Mapping.variableStructureFileNameFromId(variableStructureId, migration.projectConfig.name)
def exportFile = Paths.get("mapping", fileName)

def file = exportFile.toFile()

run(migration, file.toPath())

static void run(Migration migration, Path filePath) {
    def variables = migration.variableRepository.listAll()

    def structureId = Mapping.variableStructureIdFromFileName(filePath.fileName.toString(), migration.projectConfig.name)
    def existingStructure = migration.variableStructureRepository.find(structureId)

    def file = filePath.toFile()
    file.createParentDirectories()
    file.withWriter { writer ->
        writer.writeLine("id,name,origin_locations,inspire_path,data_type")

        for (variable in variables) {
            def variablePathData = existingStructure?.structure?.get(variable.id)
            def variableName = variablePathData?.name
            def inspirePath = variablePathData?.path

            writer.write("${Csv.serialize(variable.id)},")
            writer.write("${Csv.serialize(variableName)},")
            writer.write("${Csv.serialize(variable.originLocations)},")
            writer.write("${Csv.serialize(inspirePath)},")
            writer.write("${Csv.serialize(variable.dataType)}")
            writer.writeLine("")
        }
    }
}

static String constructDefaultId(List<VariableStructure> variableStructures) {
    def baseName = "default-"
    def number = 1
    def existingNames = variableStructures.collect { it.nameOrId() }
    while (existingNames.contains(baseName + number)) {
        number++
    }
    return baseName + number
}