package com.quadient.migration.persistence.upgrade

import com.quadient.migration.persistence.table.DisplayRuleTable
import com.quadient.migration.persistence.table.DocumentObjectTable
import com.quadient.migration.persistence.table.ImageTable
import com.quadient.migration.persistence.table.MappingTable
import com.quadient.migration.persistence.table.ParagraphStyleTable
import com.quadient.migration.persistence.table.StatusTrackingTable
import com.quadient.migration.persistence.table.TextStyleTable
import com.quadient.migration.persistence.table.VariableStructureTable
import com.quadient.migration.persistence.table.VariableTable
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class V1__init_schema : BaseJavaMigration() {
    override fun migrate(context: Context) {
        transaction {
            SchemaUtils.create(
                VariableTable,
                DocumentObjectTable,
                TextStyleTable,
                ParagraphStyleTable,
                VariableStructureTable,
                DisplayRuleTable,
                ImageTable,
                StatusTrackingTable,
                MappingTable,
            )
        }
    }
}