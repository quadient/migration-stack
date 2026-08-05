package com.quadient.migration.example.common.util

import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.repository.MappingRepository
import org.slf4j.Logger

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.ScriptArgs.getValueOfArg

static void mapProp(Object mapping, Object obj, String key, Object newValue) {
    mapping[key] = newValue
}

static Path csvPath(Binding binding, String projectName, String mapping) {
    return PathUtil.dataDirPath(binding, "mapping", "${projectName}-${mapping}.csv")
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

    File selectedFile = null
    def argUserInput = (getValueOfArg("--variable-structure-id", args as List<String>)).orElseGet { null }
    if (argUserInput) {
        def fileName = variableStructureFileNameFromId(argUserInput, projectName)
        File csvFile = csvFiles.find { (it as File).name.equalsIgnoreCase(fileName) } as File
        if (csvFile) {
            selectedFile = csvFile
            println "Selected file: ${selectedFile.name}"
        } else {
            println "CSV file '${fileName}' not found in mapping. Please provide a valid file name."
            System.exit(1)
        }
    } else if (csvFiles.size() == 1) {
        selectedFile = csvFiles.first() as File
        println "Selected file: ${selectedFile.name}"
    } else {
        selectedFile = promptForFileSelection(csvFiles)
    }

    return selectedFile.toPath()
}

Path getLayoutMappingPath(String projectName) {
    def mappingDir = Paths.get("mapping").toFile()
    def pattern = layoutFileNamePattern(projectName)
    def csvFiles = mappingDir.listFiles()?.findAll {
        it.name.toLowerCase().contains(pattern) && it.name.toLowerCase().endsWith(".csv")
    } ?: []

    if (csvFiles.isEmpty()) {
        println "No CSV files found in mapping with matching pattern '*${pattern}*.csv'."
        System.exit(1)
    }

    File selectedFile
    if (csvFiles.size() == 1) {
        selectedFile = csvFiles.first() as File
        println "Selected file: ${selectedFile.name}"
    } else {
        selectedFile = promptForFileSelection(csvFiles)
    }

    return selectedFile.toPath()
}

static String layoutFileNamePattern(String projectName) {
    return "${projectName}-layout".toLowerCase()
}

private static File promptForFileSelection(List<File> csvFiles) {
    println "Available CSV files for import:"
    csvFiles.eachWithIndex { file, i -> println "${i + 1}) ${file.name}" }
    println "Select a number of the CSV file to import:"

    while (true) {
        def userInput = System.in.newReader().readLine().trim()
        if (userInput.isInteger()) {
            def idx = userInput.toInteger() - 1
            if (idx >= 0 && idx < csvFiles.size()) {
                def selectedFile = csvFiles[idx]
                println "Selected file: ${selectedFile.name}"
                return selectedFile
            }
        }
        println "Invalid selection. Please enter a valid number:"
    }
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

static String displayHeader(String logicalName, boolean readOnly) {
    if (logicalName == null) return ""
    if (readOnly) {
        return logicalName + " (read-only)"
    }
    return logicalName
}

static String normalizeHeader(String displayName) {
    if (displayName == null) return null
    def s = displayName.trim()
    def suffix = " (read-only)"
    if (s.endsWith(suffix)) {
        s = s.substring(0, s.length() - suffix.length())
    }
    return s.trim()
}

static void upsertBatched(MappingRepository mappingRepository, Map<String, MappingItem> mappings, String label, Logger log) {
    def batches = mappings.entrySet().collate(1000)
    for (int i = 0; i < batches.size(); i++) {
        log.info "Upserting ${label} batch ${i + 1}/${batches.size()} (${batches[i].size()} items)"
        mappingRepository.upsertBatch(batches[i].collectEntries())
    }
}
