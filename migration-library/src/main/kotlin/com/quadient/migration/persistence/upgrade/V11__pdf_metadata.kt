package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

class V11__pdf_metadata : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        val docObjStmt =
            connection.prepareStatement("ALTER TABLE document_object ADD COLUMN IF NOT EXISTS pdf_metadata jsonb")
        docObjStmt.executeUpdate()
        docObjStmt.close()
    }
}
