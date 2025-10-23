package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

class V4__language_variable_upgrade  : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        val stmt = connection.prepareStatement("ALTER TABLE variable_structure ADD COLUMN IF NOT EXISTS language_variable VARCHAR(255)")
        stmt.executeUpdate()
        stmt.close()
    }
}
