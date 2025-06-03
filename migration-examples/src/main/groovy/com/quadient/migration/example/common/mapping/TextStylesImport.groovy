package com.quadient.migration.example.common.mapping

import com.quadient.migration.api.dto.migrationmodel.TextStyleDefinition
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleDefinitionBuilder
import com.quadient.migration.example.common.util.Csv

import java.lang.reflect.Modifier
import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

def migration = initMigration(this.binding.variables["args"])

def file = Paths.get("mapping", "${migration.projectConfig.name}-text-styles.csv").toFile().readLines()
def columnNames = file.removeFirst().split(",")

def definitionFields = TextStyleDefinition.declaredFields.findAll { !it.synthetic && !Modifier.isStatic(it.getModifiers()) }
for (line in file) {
    def values = Csv.getCells(line, columnNames)

    def saved = migration.textStyleRepository.find(values.get("id"))
    def styleRefId = values.get("targetId")

    if (styleRefId.empty) {
        if (saved == null) {
            def style = new TextStyleBuilder(values.get("id"))
                    .name(values.get("name"))
                    .definition {
                        for (field in definitionFields) {
                            it."$field.name"(Csv.deserialize(values.get(field.name), field.type))
                        }
                        it
                    }.build()
            migration.textStyleRepository.upsert(style)
        } else {
            if (saved.definition instanceof TextStyleRef) {
                saved.definition = new TextStyleDefinitionBuilder().build()
            }

            for (field in definitionFields) {
                saved.definition."$field.name" = Csv.deserialize(values.get(field.name), field.type)
            }
            saved.name = values.get("name")
            migration.textStyleRepository.upsert(saved)
        }
    } else {
        if (saved == null) {
            def style = new TextStyleBuilder(values.get("id"))
                    .name(values.get("name"))
                    .styleRef(styleRefId)
                    .build()
            migration.textStyleRepository.upsert(style)
        } else {
            if (saved.definition instanceof TextStyleDefinition) {
                saved.customFields.put("originalDefinition", saved.definition.toString())
            }

            saved.name = values.get("name")
            saved.definition = new TextStyleRef(styleRefId)
            migration.textStyleRepository.upsert(saved)
        }
    }
}