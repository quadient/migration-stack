package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V10__add_attachment_table : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS attachment (
                    id VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    name VARCHAR(255),
                    origin_locations TEXT[] NOT NULL,
                    custom_fields JSONB NOT NULL,
                    last_updated TIMESTAMP NOT NULL,
                    created TIMESTAMP NOT NULL,
                    source_path VARCHAR(255),
                    target_folder VARCHAR(255),
                    attachment_type VARCHAR(50) NOT NULL DEFAULT 'Attachment',
                    skip JSONB NOT NULL,
                    target_image_id VARCHAR(255),
                    PRIMARY KEY (id, project_name)
                )
                """.trimIndent()
            )
        }
        val stmt = connection.prepareStatement(
            "ALTER TABLE image ADD COLUMN IF NOT EXISTS target_attachment_id varchar(255)"
        )
        stmt.executeUpdate()
        stmt.close()
    }
}
