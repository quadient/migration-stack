package com.quadient.migration.example.common.util

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.MappingItem
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.repository.MappingRepository
import org.slf4j.Logger

import java.nio.file.Path

import static com.quadient.migration.example.common.util.ScriptArgs.getValueOfArg

static void mapProp(Object mapping, Object obj, String key, Object newValue) {
    mapping[key] = newValue
}

static <T extends MigrationObject> List<T> collectSelectedOrAll(Migration migration, Class<T> type, Closure<List<T>> listAll) {
    if (!migration.projectConfig.getDocumentObjectsToProcess().empty) {
        def selected = migration.documentObjectRepository.listIds(migration.projectConfig.getDocumentObjectsToProcess())
        def referenced = selected.collect { migration.referenceCollector.collectAllObjects(it) }.flatten()
        return (selected + referenced)
            .toSet()
            .findAll { type.isInstance(it) } as List<T>
    }
    return listAll()
}

static Path csvPath(Binding binding, String projectName, String subProjectId, String mapping) {
    return PathUtil.dataDirPath(binding, "mapping", "${namePrefix(projectName, subProjectId)}-${mapping}.csv")
}

static String namePrefix(String projectName, String subProjectId) {
    return subProjectId ? "${projectName}-${subProjectId}" : projectName
}

Path getVariablesMappingPath(Binding binding, String projectName, String subProjectId) {
    def csvFiles = PathUtil.dataDirPath(binding, "mapping").toFile().listFiles()?.findAll {
        it.name.startsWith(variableStructureFileNamePrefix(projectName, subProjectId)) && it.name.toLowerCase().endsWith(".csv")
    } ?: []

    if (csvFiles.isEmpty()) {
        throw new IllegalStateException("No CSV files found in mapping with matching pattern '${variableStructureFileNamePrefix(projectName, subProjectId)}<id>.csv'.")
    }

    def argUserInput = (getValueOfArg("--variable-structure-id", binding.variables["args"] as List<String>)).orElseGet { null }
    if (argUserInput) {
        def fileName = variableStructureFileNameFromId(argUserInput, projectName, subProjectId)
        File csvFile = csvFiles.find { (it as File).name.equalsIgnoreCase(fileName) } as File
        if (!csvFile) {
            throw new IllegalStateException("CSV file '${fileName}' not found in mapping. Please provide a valid file name.")
        }
        println "Selected file: ${csvFile.name}"
        return csvFile.toPath()
    }

    return selectFile(UserInput.from(binding), "Select the variable structure CSV file to import", csvFiles).toPath()
}

Path getLayoutMappingPath(Binding binding, String projectName, String subProjectId) {
    def pattern = layoutFileNamePattern(projectName, subProjectId)
    def csvFiles = PathUtil.dataDirPath(binding, "mapping").toFile().listFiles()?.findAll {
        it.name.toLowerCase().contains(pattern) && it.name.toLowerCase().endsWith(".csv")
    } ?: []

    if (csvFiles.isEmpty()) {
        throw new IllegalStateException("No CSV files found in mapping with matching pattern '*${pattern}*.csv'.")
    }

    return selectFile(UserInput.from(binding), "Select the layout CSV file to import", csvFiles).toPath()
}

static String layoutFileNamePattern(String projectName, String subProjectId) {
    return "${namePrefix(projectName, subProjectId)}-layout".toLowerCase()
}

private static File selectFile(UserInput input, String message, List<File> csvFiles) {
    def selectedFile = csvFiles.first()
    if (csvFiles.size() > 1) {
        def selectedName = input.choose(message, csvFiles*.name)
        selectedFile = csvFiles.find { it.name == selectedName }
    }
    println "Selected file: ${selectedFile.name}"
    return selectedFile
}

static String variableStructureFileNamePrefix(String projectName, String subProjectId) {
    return "${namePrefix(projectName, subProjectId)}-variable-structure-"
}

static Path variableStructureCsvPath(Binding binding, String id, String projectName, String subProjectId) {
    return PathUtil.dataDirPath(binding, "mapping", variableStructureFileNameFromId(id, projectName, subProjectId))
}

static String variableStructureFileNameFromId(String id, String projectName, String subProjectId) {
    return "${variableStructureFileNamePrefix(projectName, subProjectId)}${id}.csv"
}

static String variableStructureIdFromFileName(String fileName, String projectName, String subProjectId) {
    def prefix = variableStructureFileNamePrefix(projectName, subProjectId)
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
