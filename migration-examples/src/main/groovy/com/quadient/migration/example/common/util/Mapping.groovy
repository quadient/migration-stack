package com.quadient.migration.example.common.util

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.ScriptArgs.getValueOfArg

static void mapProp(Object mapping, Object obj, String key, Object newValue) {
    if (newValue != null && newValue != obj[key] && mapping[key] != newValue && newValue != "") {
        mapping[key] = newValue
    }
}

Path getVariablesMappingPath() {
    def variablesMappingDir = Paths.get("mapping", "variables").toFile()
    def csvFiles = variablesMappingDir.listFiles()?.findAll { it.name.toLowerCase().endsWith(".csv") } ?: []

    if (csvFiles.isEmpty()) {
        println "No CSV files found in mapping/variables/. Cannot import variable structure."
        System.exit(1)
    }

    def selectedFile = null
    def argUserInput = (getValueOfArg("--variable-structure-name", this.binding.variables["args"] as List<String>)).orElseGet { null }
    if (argUserInput) {
        def csvName = argUserInput.toLowerCase().endsWith(".csv") ? argUserInput : "${argUserInput}.csv"
        def csvFile = csvFiles.find { it.name.equalsIgnoreCase(csvName) }
        if (csvFile) {
            selectedFile = csvFile
            println "Selected file: ${selectedFile.name}"
        } else {
            println "CSV file '${csvName}' not found in mapping/variables/. Please provide a valid file name."
            System.exit(1)
        }
    } else {
        println "Available CSV files for import:"
        csvFiles.eachWithIndex { file, i -> println "${i + 1} - ${file.name}" }
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
