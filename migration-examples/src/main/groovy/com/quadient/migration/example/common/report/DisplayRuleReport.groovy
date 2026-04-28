//! ---
//! displayName: Display Rule Report
//! category: Report
//! description: Creates a CSV report with information on display rules in the migration project, including usage, translation status, errors, and origin details for each rule.
//! ---
package com.quadient.migration.example.common.report

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.example.common.util.PathUtil
import com.quadient.migration.service.deploy.utility.ResourceType
import groovy.transform.Field

import java.nio.file.Path

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding)

def dstFile  = PathUtil.dataDirPath(binding, "report", "${migration.projectConfig.name}-display-rules-report.csv")

@Field static HashMap<String, DocObjAndRefs> docObjWithRefsCache = new HashMap()
def allObjects = migration.documentObjectRepository.listAll()
for (obj in allObjects) {
    docObjWithRefsCache.put(obj.id, new DocObjAndRefs(obj, collectRefsRecursively(migration, new DocumentObjectRef(obj.id))))
}

static List<Ref> collectRefsRecursively(Migration migration, Ref ref, Set<Ref> visited = new HashSet<>()) {
    if (visited.contains(ref)) {
        return []
    }
    visited.add(ref)

    def result = []
    def refs = switch (ref) {
        case DocumentObjectRef -> {
            def docObj = migration.documentObjectRepository.findOrFail(ref.id)
            docObj.collectRefs()
        }
        case DisplayRuleRef -> {
            def displayRule = migration.displayRuleRepository.findOrFail(ref.id)
            displayRule.collectRefs()
        }
        case AttachmentRef -> {
            def attachment = migration.attachmentRepository.findOrFail(ref.id)
            attachment.collectRefs()
        }
        case ImageRef -> {
            def image = migration.imageRepository.findOrFail(ref.id)
            image.collectRefs()
        }
        case ParagraphStyleRef -> {
            def paragraphStyle = migration.paragraphStyleRepository.findOrFail(ref.id)
            paragraphStyle.collectRefs()
        }
        case TextStyleRef -> {
            def textStyle = migration.textStyleRepository.findOrFail(ref.id)
            textStyle.collectRefs()
        }
        case VariableRef -> {
            def variable = migration.variableRepository.findOrFail(ref.id)
            variable.collectRefs()
        }
        case VariableStructureRef -> {
            def variableStructure = migration.variableStructureRepository.findOrFail(ref.id)
            variableStructure.collectRefs()
        }
        default -> throw new IllegalArgumentException("Unknown ref type: ${ref.class}")
    }

    for (r in refs) {
        result.add(r)
        result.addAll(collectRefsRecursively(migration, r, visited))
    }

    return result
}

def displayRules = migration.displayRuleRepository.listAll()

exportReport(migration, displayRules, dstFile)

static void exportReport(Migration migration, List<DisplayRule> rules, Path exportFilePath) {
    def file =  exportFilePath.toFile()
    file.createParentDirectories()
    file.withWriter { writer ->
        writer.writeLine("id,name,internal,baseTemplate,targetFolder,targetId,variableStructureRef,status,usedBy,usedByOrigin,usedByTypeOrigin,translated,translationError,source_files,originContent")

        rules.each { rule ->
            def translationError = rule.customFields.get("error")
            def usedBy = findUsages(rule.id)
            def usedByValue = usedBy.collect { it.id }.join(";")

            def status = migration.statusTrackingRepository.findLastEventRelevantToOutput(rule.id,
                ResourceType.DisplayRule,
                migration.projectConfig.inspireOutput)

            def builder = new StringBuilder()
            builder.append(rule.id)
            builder.append("," + rule.name)
            builder.append("," + Csv.serialize(rule.internal))
            builder.append("," + Csv.serialize(rule.baseTemplate))
            builder.append("," + Csv.serialize(rule.targetFolder))
            builder.append("," + Csv.serialize(rule.targetId?.id))
            builder.append("," + Csv.serialize(rule.variableStructureRef?.id))
            builder.append("," + Csv.serialize(status?.class?.simpleName))
            builder.append("," + usedByValue)
            builder.append("," + rule.customFields.get("usedBy")?.replace(",", ";"))
            builder.append("," + rule.customFields.get("usedByType")?.replace(",", ";"))
            builder.append("," + Csv.serialize(rule.definition != null))
            builder.append("," + "\"${translationError?.replace("\n", "\\n")?.replace("\"", "\\")}\"")
            builder.append("," + rule.originLocations.join(";"))
            builder.append("," + "\"${rule.customFields?.get("originContent")?.replace("\n", "\\n")?.replace("\"", "\\")}\"")
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
