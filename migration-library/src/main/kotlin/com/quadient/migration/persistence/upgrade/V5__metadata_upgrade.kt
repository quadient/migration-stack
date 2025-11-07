package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

class V5__metadata_upgrade  : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        val docObjStmt = connection.prepareStatement("ALTER TABLE document_object ADD COLUMN IF NOT EXISTS metadata jsonb")
        docObjStmt.executeUpdate()
        docObjStmt.close()
        val imageStmt = connection.prepareStatement("ALTER TABLE image ADD COLUMN IF NOT EXISTS metadata jsonb")
        imageStmt.executeUpdate()
        imageStmt.close()
    }
}