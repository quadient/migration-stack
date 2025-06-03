package com.quadient.migration.example.common.report

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.example.common.util.Csv
import groovy.transform.Field

import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding.variables["args"])

def dstFile = Paths.get("report", "${migration.projectConfig.name}-display-rules-report.csv")

@Field static HashMap<String, DocObjAndRefs> docObjWithRefsCache = new HashMap()
def allObjects = migration.documentObjectRepository.listAll()
for (obj in allObjects) {
    docObjWithRefsCache.put(obj.id, new DocObjAndRefs(obj, migration.documentObjectRepository.findRefs(obj.id)))
}

def displayRules = migration.displayRuleRepository.listAll()

List<String> definitionOrder = ["validationState"]

exportReport(displayRules, definitionOrder, dstFile)

static void exportReport(List<DisplayRule> rules, List<String> definitionOrder, Path exportFilePath) {
    def file =  exportFilePath.toFile()
    file.createParentDirectories()
    file.withWriter { writer ->
        writer.writeLine("id,name,usedBy,usedByOrigin,usedByTypeOrigin,translated,translationError,source_files,originContent,${definitionOrder.join(",")}")

        rules.each { rule ->
            def values = definitionOrder.collect { Csv.serialize(rule."${it}") }.join(",")
            def translationError = rule.customFields.get("error")
            def usedBy = findUsages(rule.id)
            def usedByValue = usedBy.collect { it.id }.join(";")

            def builder = new StringBuilder()
            builder.append(rule.id)
            builder.append("," + rule.name)
            builder.append("," + usedByValue)
            builder.append("," + rule.customFields.get("usedBy")?.replace(",", ";"))
            builder.append("," + rule.customFields.get("usedByType")?.replace(",", ";"))
            builder.append("," + Csv.serialize(rule.definition != null))
            builder.append("," + "\"${translationError?.replace("\n", "\\n")?.replace("\"", "\\")}\"")
            builder.append("," + rule.originLocations.join(";"))
            builder.append("," + "\"${rule.customFields.get("originContent").replace("\n", "\\n").replace("\"", "\\")}\"")
            builder.append("," + values)
            writer.writeLine(builder.toString())
        }
    }
}

static List<DocumentObject> findUsages(String id) {
    docObjWithRefsCache.findAll { it.value.refs.any { it.id == id } }.collect { it.value.obj }
}

class DocObjAndRefs {
    DocumentObject obj
    List<Ref> refs

    DocObjAndRefs(DocumentObject obj, List<Ref> refs) {
        this.obj = obj
        this.refs = refs
    }
}
