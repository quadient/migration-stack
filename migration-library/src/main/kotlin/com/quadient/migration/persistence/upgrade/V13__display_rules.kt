package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

class V13__display_rules : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        connection.createStatement().use { stmt ->
            stmt.execute("ALTER TABLE display_rule ADD COLUMN IF NOT EXISTS internal BOOLEAN NOT NULL DEFAULT TRUE")
            stmt.execute("ALTER TABLE display_rule ADD COLUMN IF NOT EXISTS target_id VARCHAR(255) NULL")
            stmt.execute("ALTER TABLE display_rule ADD COLUMN IF NOT EXISTS subject VARCHAR(255) NULL")
            stmt.execute("ALTER TABLE display_rule ADD COLUMN IF NOT EXISTS target_folder VARCHAR(255) NULL")
            stmt.execute("ALTER TABLE display_rule ADD COLUMN IF NOT EXISTS base_template VARCHAR(255) NULL")
            stmt.execute("ALTER TABLE display_rule ADD COLUMN IF NOT EXISTS variable_structure_ref VARCHAR(255) NULL")
            stmt.execute("ALTER TABLE display_rule ADD COLUMN IF NOT EXISTS metadata JSON NOT NULL DEFAULT '{}'")
        }
    }
}