package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleDefinitionBuilder
import com.quadient.migration.example.common.util.Csv

import java.lang.reflect.Modifier
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def file = Paths.get("mapping", "${migration.projectConfig.name}-paragraph-styles.csv").toFile().readLines()
def columnNames = file.removeFirst().split(",")

def definitionFields = ParagraphStyleDefinition.declaredFields.findAll { !it.synthetic && !Modifier.isStatic(it.getModifiers()) }
for (line in file) {
    def values = Csv.getCells(line, columnNames)

    def saved = migration.paragraphStyleRepository.find(values.get("id"))
    def styleRefId = values.get("targetId")

    if (styleRefId.empty) {
        if (saved == null) {
            def style = new ParagraphStyleBuilder(values.get("id"))
                    .name(values.get("name"))
                    .definition {
                        for (field in definitionFields) {
                            it."$field.name"(Csv.deserialize(values.get(field.name), field.type))
                        }
                        it
                    }.build()
            migration.paragraphStyleRepository.upsert(style)
        } else {
            if (saved.definition instanceof ParagraphStyleRef) {
                saved.definition = new ParagraphStyleDefinitionBuilder().build()
            }

            for (field in definitionFields) {
                saved.definition."$field.name" = Csv.deserialize(values.get(field.name), field.type)
            }
            saved.name = values.get("name")
            migration.paragraphStyleRepository.upsert(saved)
        }
    } else {
        if (saved == null) {
            def style = new ParagraphStyleBuilder(values.get("id"))
                    .name(values.get("name"))
                    .styleRef(styleRefId)
                    .build()
            migration.paragraphStyleRepository.upsert(style)
        } else {
            if (saved.definition instanceof ParagraphStyleDefinition) {
                saved.customFields.put("originalDefinition", saved.definition.toString())
            }

            saved.name = values.get("name")
            saved.definition = new ParagraphStyleRef(styleRefId)
            migration.paragraphStyleRepository.upsert(saved)
        }
    }
}
