package com.quadient.migration.persistence.upgrade

import com.quadient.migration.persistence.table.AttachmentTable
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

class V10__add_attachment_table : BaseJavaMigration() {
    override fun migrate(context: Context) {
        transaction {
            SchemaUtils.create(AttachmentTable)
        }
        
        val connection: Connection = context.connection
        val stmt = connection.prepareStatement(
            "ALTER TABLE image ADD COLUMN IF NOT EXISTS target_attachment_id varchar(255)"
        )
        stmt.executeUpdate()
        stmt.close()
    }
}
