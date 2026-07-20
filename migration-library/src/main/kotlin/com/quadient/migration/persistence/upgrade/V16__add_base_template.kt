package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V16__add_base_template : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS base_template (
                    id VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    name VARCHAR(255),
                    origin_locations TEXT[] NOT NULL,
                    custom_fields JSONB NOT NULL,
                    last_updated TIMESTAMP NOT NULL,
                    created TIMESTAMP NOT NULL,
                    target_folder VARCHAR(255),
                    pages JSONB NOT NULL,
                    PRIMARY KEY (id, project_name)
                )
                """.trimIndent()
            )

            for (table in listOf("document_object", "display_rule")) {
                stmt.execute(
                    """
                    ALTER TABLE $table
                    ALTER COLUMN base_template TYPE JSONB
                    USING (
                        CASE
                            WHEN base_template IS NULL THEN NULL
                            ELSE jsonb_build_object('type', 'BaseTemplatePath', 'path', base_template)
                        END
                    )
                    """.trimIndent()
                )
            }

            for (type in listOf("DocumentObject", "DisplayRule")) {
                stmt.execute(
                    """
                    UPDATE mapping
                    SET mappings = jsonb_set(
                        mappings,
                        '{baseTemplate}',
                        jsonb_build_object('type', 'BaseTemplatePath', 'path', mappings->>'baseTemplate'),
                        false
                    )
                    WHERE type = '$type'
                      AND mappings ? 'baseTemplate'
                      AND jsonb_typeof(mappings->'baseTemplate') = 'string'
                    """.trimIndent()
                )
            }
        }
    }
}
