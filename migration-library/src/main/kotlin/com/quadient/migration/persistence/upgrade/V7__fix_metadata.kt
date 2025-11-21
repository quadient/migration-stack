package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

class V7__fix_metadata : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        val docObjStmt = connection.prepareStatement("UPDATE document_object SET metadata = '{}' WHERE metadata IS NULL")
        docObjStmt.executeUpdate()
        docObjStmt.close()

        val imageStmt = connection.prepareStatement("UPDATE image SET metadata = '{}' WHERE metadata IS NULL")
        imageStmt.executeUpdate()
        imageStmt.close()
    }
}
