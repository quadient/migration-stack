package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V17__base_template_variable_structure : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                ALTER TABLE base_template
                ADD COLUMN IF NOT EXISTS variable_structure_ref VARCHAR(255)
                """.trimIndent()
            )
        }
    }
}
