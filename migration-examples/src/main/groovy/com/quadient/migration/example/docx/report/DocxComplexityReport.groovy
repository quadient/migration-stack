//! ---
//! displayName: DOCX Complexity Report
//! category: Report
//! description: Creates a CSV report with statistics captured from the parsed DOCX templates, including text length and counts of paragraphs, tables, cells, images, shapes, headers and footers for each template.
//! sourceFormat: DOCX
//! ---
package com.quadient.migration.example.docx.report

import com.quadient.migration.api.Migration
import com.quadient.migration.example.common.util.Csv
import java.nio.file.Path
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding)
Path dstFile = Paths.get("report", "${migration.projectConfig.name}-docx-complexity-report.csv")

run(migration, dstFile)

static void run(Migration migration, Path documentObjectsDstPath) {
    def objects = migration.documentObjectRepository.listAll().findAll { !it.internal }

    documentObjectsDstPath.toFile().createParentDirectories()

    documentObjectsDstPath.toFile().withWriter { writer ->
        writer.writeLine("id,folder,name,title,textLength,paragraphs,tables,cells,images,shapes,headers,footers,byteSize")
        objects.each { obj ->

            def builder = new StringBuilder()
            builder.append(Csv.serialize(obj.id))
            builder.append("," + Csv.serialize(obj.targetFolder))
            builder.append("," + Csv.serialize(obj.name))
            builder.append("," + Csv.serialize((obj.customFields.get("Title") ?: "")))
            builder.append("," + (obj.customFields.get("textLength") ?: "0"))
            builder.append("," + (obj.customFields.get("paragraphs") ?: "0"))
            builder.append("," + (obj.customFields.get("tables") ?: "0"))
            builder.append("," + (obj.customFields.get("cells") ?: "0"))
            builder.append("," + (obj.customFields.get("images") ?: "0"))
            builder.append("," + (obj.customFields.get("shapes") ?: "0"))
            builder.append("," + (obj.customFields.get("headers") ?: "0"))
            builder.append("," + (obj.customFields.get("footers") ?: "0"))
            builder.append("," + (obj.customFields.get("size") ?: "0"))
            writer.writeLine(builder.toString())
        }
    }
}

