package com.quadient.migration.example.common.util

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.ScriptArgs.getValueOfArg

static void mapProp(Object mapping, Object obj, String key, Object newValue) {
    if (newValue != null && newValue != obj[key] && mapping[key] != newValue && newValue != "") {
        mapping[key] = newValue
    }
}

Path getVariablesMappingPath(String[] args, String projectName) {
    def variablesMappingDir = Paths.get("mapping").toFile()
    def csvFiles = variablesMappingDir.listFiles()?.findAll {
        it.name.startsWith(variableStructureFileNamePrefix(projectName)) && it.name.toLowerCase().endsWith(".csv")
    } ?: []

    if (csvFiles.isEmpty()) {
        println "No CSV files found in mapping with matching pattern '${variableStructureFileNamePrefix(projectName)}<id>.csv'."
        System.exit(1)
    }

    def selectedFile = null
    def argUserInput = (getValueOfArg("--variable-structure-id", args as List<String>)).orElseGet { null }
    if (argUserInput) {
        def fileName = variableStructureFileNameFromId(argUserInput, projectName)
        def csvFile = csvFiles.find { it.name.equalsIgnoreCase(fileName) }
        if (csvFile) {
            selectedFile = csvFile
            println "Selected file: ${selectedFile.name}"
        } else {
            println "CSV file '${fileName}' not found in mapping. Please provide a valid file name."
            System.exit(1)
        }
    } else {
        println "Available CSV files for import:"
        csvFiles.eachWithIndex { file, i -> println "${i + 1}) ${file.name}" }
        println "Select a number of the CSV file to import:"

        while (true) {
            def userInput = System.in.newReader().readLine().trim()
            if (userInput.isInteger()) {
                def idx = userInput.toInteger() - 1
                if (idx >= 0 && idx < csvFiles.size()) {
                    selectedFile = csvFiles[idx]
                    println "Selected file: ${selectedFile.name}"
                    break
                }
            }
            println "Invalid selection. Please enter a valid number:"
        }
    }

    return selectedFile.toPath()
}

static String variableStructureFileNamePrefix(String projectName) {
    return "${projectName}-variable-structure-"
}

static String variableStructureFileNameFromId(String id, String projectName) {
    return "${variableStructureFileNamePrefix(projectName)}${id}.csv"
}

static String variableStructureIdFromFileName(String fileName, String projectName) {
    def prefix = variableStructureFileNamePrefix(projectName)
    if (!fileName.startsWith(prefix) || !fileName.endsWith(".csv")) {
        throw new IllegalArgumentException("Invalid variable structure file name: ${fileName}")
    }
    return fileName.substring(prefix.length(), fileName.length() - ".csv".length())
}
