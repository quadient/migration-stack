//! ---
//! displayName: Export Variables
//! category: Mapping
//! description: Creates a CSV report with variable structure from the migration project. The export is interactive, prompting the user to select or enter a variable structure to export.
//! stdin: true
//! ---
package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.Mapping
import com.quadient.migration.example.common.util.UserInput
import com.quadient.migration.shared.LiteralPath
import com.quadient.migration.shared.VariableRefPath

import groovy.transform.Field

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration
import static com.quadient.migration.example.common.util.ScriptArgs.getValueOfArg

@Field static final String FORBIDDEN_ID_CHARS = /[\\\/:\*\?"<>\|]/
@Field static final String INVALID_ID_MESSAGE = "Invalid id. Please avoid using these characters: \\ / : * ? \" < > |"

def migration = initMigration(this.binding)

def argUserInput = (getValueOfArg("--variable-structure-id", this.binding.variables["args"] as List<String>)).orElseGet { null }

def variableStructures = migration.variableStructureRepository.listAll()

def variableStructureId = argUserInput
if (variableStructureId && variableStructureId =~ FORBIDDEN_ID_CHARS) {
    throw new IllegalArgumentException(INVALID_ID_MESSAGE)
}
if (!variableStructureId) {
    variableStructureId = askForVariableStructureId(UserInput.from(this.binding), variableStructures*.id, constructDefaultId(variableStructures))
}
println "Selected variable structure: $variableStructureId"

def exportFile = Mapping.variableStructureCsvPath(this.binding, variableStructureId, migration.projectConfig.name, migration.projectConfig.subProjectId)

run(migration, exportFile)

static void run(Migration migration, Path filePath) {
    List<Variable> variables = Mapping.collectSelectedOrAll(migration, Variable) { migration.variableRepository.listAll() }

    def structureId = Mapping.variableStructureIdFromFileName(filePath.fileName.toString(), migration.projectConfig.name, migration.projectConfig.subProjectId)
    def existingStructure = migration.variableStructureRepository.find(structureId)

    def file = filePath.toFile()
    file.createParentDirectories()
    file.withWriter { writer ->
        def headers = ["id", "name", "data_type", "inspire_path", "inspire_name", "language_variable", Mapping.displayHeader("originLocations", true)]
        writer.writeLine(headers.join(","))

        for (variable in variables) {
            def variablePathData = existingStructure?.structure?.get(variable.id)
            def variableName = variablePathData?.name
            def inspirePath = serializeVariablePath(variablePathData?.path)

            writer.write("${Csv.serialize(variable.id)},")
            writer.write("${Csv.serialize(variable.name)},")
            writer.write("${Csv.serialize(variable.dataType)},")
            writer.write("${Csv.serialize(inspirePath)},")
            writer.write("${Csv.serialize(variableName)},")
            if (existingStructure?.languageVariable?.id == variable.id) {
                writer.write("true,")
            } else {
                writer.write(",")
            }
            writer.write(Csv.serialize(variable.originLocations))
            writer.writeLine("")
        }
    }
}

static String serializeVariablePath(path) {
    if (path == null) return null
    if (path instanceof VariableRefPath) return "\$${path.variableId}"
    if (path instanceof LiteralPath) return path.path
    return path.toString()
}

static String askForVariableStructureId(UserInput input, List<String> existingIds, String defaultId) {
    def question = "Select an existing variable structure or type an id for a new one"
    def answer = input.chooseOrType(question, existingIds, defaultId)
    while (answer =~ FORBIDDEN_ID_CHARS) {
        answer = input.chooseOrType("$INVALID_ID_MESSAGE\n$question", existingIds, defaultId)
    }
    return answer
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