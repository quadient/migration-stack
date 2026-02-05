package com.quadient.migration.persistence.upgrade

import com.quadient.migration.persistence.table.FileTable
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class V10__add_file_table : BaseJavaMigration() {
    override fun migrate(context: Context) {
        transaction {
            SchemaUtils.create(FileTable)
        }
    }
}
