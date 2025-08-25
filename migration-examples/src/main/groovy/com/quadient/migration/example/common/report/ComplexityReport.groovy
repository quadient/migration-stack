//! ---
//! category: Report
//! ---
package com.quadient.migration.example.common.report

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.service.deploy.ResourceType
import groovy.transform.Field

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding)

def dstFile = Paths.get("report", "${migration.projectConfig.name}-complexity-report.csv")

def header = ["Id",
              "Name",
              "Origin locations",
              "Type",
              "Used in",
              "Used in count",
              "Internal objects",
              "External objects",
              "External objects count",
              "Tables",
              "Variables",
              "Characters",
              "Words",
              "Lines",
              "Display rules",
              "Translated display rules",
              "Images",
              "Paragraph styles",
              "Text styles",
              "Last status",
              "Created",
              "Last updated"]

def allDocumentObjects = migration.documentObjectRepository.listAll()
Map<String, DocumentObject> internalDocumentObjects = [:]
List<DocumentObject> externalDocumentObjects = []

for (obj in allDocumentObjects) {
    if (obj.internal) {
        internalDocumentObjects[obj.id] = obj
    } else {
        externalDocumentObjects.add(obj)
    }
}

def file = dstFile.toFile()
file.createParentDirectories()
file.withWriter { writer ->
    writer.writeLine(header.join(","))
    Map<String, Set<String>> usageMap = [:]

    List<Tuple2<DocumentObject, Stats>> objectsWithStats
        = externalDocumentObjects.collect { Tuple.tuple(it, new Stats(migration, usageMap).collect(it)) }.toList()

    for (objWithStats in objectsWithStats) {
        def obj = objWithStats.getV1()
        def stats = objWithStats.getV2()
        def lastStatus = migration.statusTrackingRepository.findLastEventRelevantToOutput(obj.id,
            ResourceType.DocumentObject,
            migration.projectConfig.inspireOutput)

        writer.write("$obj.id,") // Id
        writer.write("$obj.name,") // Name
        writer.write("${Csv.serialize(obj.originLocations)},") // Origin locations
        writer.write("$obj.type,") // Type
        writer.write("[${usageMap.get(obj.id)?.join(";") ?: ""}],") // Used in
        writer.write("${usageMap.get(obj.id)?.size() ?: "0"},") // Used in count
        writer.write("$stats.internalObjectsCount,") // Internal objects count
        writer.write("[${stats.usedExternalObjects.join(";")}],") // External objects count
        writer.write("$stats.externalObjectsCount,") // External objects count
        writer.write("$stats.tablesCount,") // Tables count
        writer.write("$stats.variablesCount,") // Variables count
        writer.write("$stats.characterCount,") // Characters count
        writer.write("$stats.wordCount,") // Words count
        writer.write("$stats.lineCount,") // Lines count
        writer.write("$stats.displayRulesCount,") // Display rules count
        writer.write("$stats.translatedDisplayRulesCount,") // Translated display rules
        writer.write("$stats.imagesCount,") // Images count
        writer.write("$stats.usedParagraphStylesCount,") // Used Paragraph Styles Count
        writer.write("$stats.usedTextStylesCount,") // Used Text Styles Count
        writer.write("$lastStatus.class.simpleName,") // Last status
        writer.write("$obj.created,") // Created
        writer.writeLine("$obj.lastUpdated") // Last updated
    }
}

class Stats {
    String Id
    Migration migration
    Map<String, Set<String>> usageMap = [:]

    Set<String> usedInternalObjects = new HashSet()
    Set<String> usedExternalObjects = new HashSet()
    Set<String> usedVariables = new HashSet()
    Set<String> usedDisplayRules = new HashSet()
    Set<String> usedTranslatedDisplayRules = new HashSet()
    Set<String> usedImages = new HashSet()
    Set<String> usedParagraphStyles = new HashSet()
    Set<String> usedTextStyles = new HashSet()

    Number tablesCount = 0

    Number wordCount = 0
    Number lineCount = 0
    Number characterCount = 0

    Stats(Migration migration, Map<String, Set<String>> usageMap) {
        this.migration = migration
        this.usageMap = usageMap
    }

    Stats collect(DocumentObject obj) {
        this.id = obj.id
        this.collectContent(obj.content)
        return this
    }

    void collectContent(List<DocumentContent> contentList) {
        for (content in contentList) {
            switch (content) {
                case DocumentObjectRef -> this.collectDocumentObjectRef(content)
                case ImageRef -> this.usedImages.add(content.id)
                case Table -> this.collectTable(content)
                case Paragraph -> this.collectParagraph(content)
                case Area -> this.collectContent(content.content)
                case FirstMatch -> this.collectFirstMatch(content)
                default -> throw new IllegalStateException("Unknown content type: ${content.class.name}")
            }
        }
    }

    void collectFirstMatch(FirstMatch firstMatch) {
        this.collectContent(firstMatch.default)
        for (fmCase in firstMatch.cases) {
            this.collectDisplayRule(fmCase.displayRuleRef)
            this.collectContent(fmCase.content)
        }
    }

    void collectTextContent(List<TextContent> contentList) {
        for (content in contentList) {
            switch (content) {
                case DocumentObjectRef -> this.collectDocumentObjectRef(content)
                case ImageRef -> this.usedImages.add(content.id)
                case StringValue -> {
                    this.characterCount += content.value.chars.length
                    this.wordCount += content.value.split("\\s+").length
                    this.lineCount += content.value.lines().count()
                }
                case Table -> this.collectTable(content)
                case VariableRef -> this.usedVariables.add(content.id)
                case FirstMatch -> this.collectFirstMatch(content)
                default -> throw new IllegalStateException("Unknown text content type: ${content.class.name}")
            }
        }
    }

    void collectParagraph(Paragraph paragraph) {
        this.collectDisplayRule(paragraph.displayRuleRef)
        if (paragraph.styleRef != null) {
            this.usedParagraphStyles.add(paragraph.styleRef.id)
        }

        for (text in paragraph.content) {
            this.collectDisplayRule(text.displayRuleRef)
            if (text.styleRef != null) {
                this.usedTextStyles.add(text.styleRef.id)
            }
            this.collectTextContent(text.content)
        }
    }

    void collectTable(Table table) {
        this.tablesCount++
        for (row in table.rows) {
            this.collectDisplayRule(row.displayRuleRef)
            for (cell in row.cells) {
                this.collectContent(cell.content)
            }
        }
    }

    void collectDocumentObjectRef(DocumentObjectRef ref) {
        def dependency = migration.documentObjectRepository.find(ref.id)
        if (dependency.internal) {
            this.usedInternalObjects.add(ref.id)
            this.collectContent(dependency.content)
        } else {
            if (this.usageMap[ref.id] == null) {
                this.usageMap[ref.id] = new HashSet()
            }
            this.usageMap[ref.id].add(this.id)
            this.usedExternalObjects.add(ref.id)
        }
        this.collectDisplayRule(ref.displayRuleRef)
    }

    void collectDisplayRule(DisplayRuleRef ref) {
        if (ref != null) {
            def rule = migration.displayRuleRepository.find(ref.id)
            this.usedDisplayRules.add(ref.id)
            if (rule.customFields.get("error") == null) {
                this.usedTranslatedDisplayRules.add(ref.id)
            }
        }
    }

    Number getInternalObjectsCount() {
        return this.usedInternalObjects.size()
    }

    Number getExternalObjectsCount() {
        return this.usedExternalObjects.size()
    }

    Number getVariablesCount() {
        return this.usedVariables.size()
    }

    Number getDisplayRulesCount() {
        return this.usedDisplayRules.size()
    }

    Number getTranslatedDisplayRulesCount() {
        return this.usedTranslatedDisplayRules.size()
    }

    Number getImagesCount() {
        return this.usedImages.size()
    }

    Number getUsedParagraphStylesCount() {
        return this.usedParagraphStyles.size()
    }

    Number getUsedTextStylesCount() {
        return this.usedTextStyles.size()
    }

    Number getUsageCount() {
        return this.usageMap[this.id]?.size() ?: 0
    }
}