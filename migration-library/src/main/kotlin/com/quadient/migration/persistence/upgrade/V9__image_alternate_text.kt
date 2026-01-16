package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

class V9__image_alternate_text : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        // Add alternate_text column to image table
        val imageStmt = connection.prepareStatement("ALTER TABLE image ADD COLUMN IF NOT EXISTS alternate_text varchar(255)")
        imageStmt.executeUpdate()
        imageStmt.close()
    }
}

