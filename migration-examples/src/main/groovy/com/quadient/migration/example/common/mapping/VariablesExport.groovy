package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.example.common.util.Csv

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration
import static com.quadient.migration.example.common.util.ScriptArgs.getValueOfArg

def migration = initMigration(this.binding.variables["args"])

def argUserInput = (getValueOfArg("--variable-structure-name", this.binding.variables["args"] as List<String>)).orElseGet { null }

def forbiddenChars = /[\\\/:\*\?"<>\|]/

def variables = migration.variableRepository.listAll()
def variableStructures = migration.variableStructureRepository.listAll()
def defaultVariableStructureName = constructDefaultName(variableStructures, migration)

if (!argUserInput) {
    if (variableStructures.isEmpty()) {
        println "No existing variable structures found."
        println "Type a name of new variable structure (leave empty for default: ${defaultVariableStructureName}):"
    } else {
        println "Existing variable structures for export:"
        variableStructures.eachWithIndex { variableStructure, i -> println "${i + 1} - ${variableStructure.nameOrId()}" }
        println "Either select a number of an existing variable structure, or type a name for a new variable structure (leave empty for default: ${defaultVariableStructureName}):"
    }
}

def variableStructureName
def selectedVariableStructure
def userInput

while (true) {
    userInput = argUserInput ?: System.in.newReader().readLine().trim()
    if (!userInput) {
        variableStructureName = defaultVariableStructureName
        println "No input provided. Generated new variable structure name: $variableStructureName"
        break
    } else if (userInput.isInteger()) {
        def i = userInput.toInteger() - 1
        if (i < 0 || i >= variableStructures.size()) {
            println "Invalid selection. Please enter a valid number:"
            continue
        }
        selectedVariableStructure = variableStructures[i]
        variableStructureName = selectedVariableStructure.nameOrId()
        println "Selected variable structure: $variableStructureName"
        break
    } else {
        if (userInput =~ forbiddenChars) {
            println "Invalid name. Please avoid using these characters: \\ / : * ? \" < > |"
            continue
        }
        variableStructureName = userInput
        break
    }
}

def fileName = variableStructureName.toLowerCase().endsWith(".csv")
        ? variableStructureName
        : "${variableStructureName}.csv"
def exportFile = Paths.get("mapping", "variables", fileName)

def file = exportFile.toFile()
file.createParentDirectories()
file.withWriter { writer ->
    writer.writeLine("id,name,origin_locations,inspire_path,data_type")

    for (variable in variables) {
        def path = ""
        if (selectedVariableStructure && selectedVariableStructure.structure.containsKey(variable.id)) {
            path = selectedVariableStructure.structure.get(variable.id)
        }
        writer.writeLine("${Csv.serialize(variable.id)},${Csv.serialize(variable.name)},${Csv.serialize(variable.originLocations)},${path},${Csv.serialize(variable.dataType)}")
    }
}

static String constructDefaultName(List<VariableStructure> variableStructures, Migration migration) {
    def baseName = "${migration.projectConfig.name}-variable-structure-"
    def number = 1
    def existingNames = variableStructures.collect { it.nameOrId() }
    while (existingNames.contains(baseName + number)) {
        number++
    }
    return baseName + number
}