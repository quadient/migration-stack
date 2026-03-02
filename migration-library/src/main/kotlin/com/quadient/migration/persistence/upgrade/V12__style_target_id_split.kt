package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V12__style_target_id_split : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection

        // Split style table representation into explicit definition + target_id columns.
        connection.prepareStatement("ALTER TABLE text_style ADD COLUMN IF NOT EXISTS target_id varchar(255)").execute()
        connection.prepareStatement("ALTER TABLE paragraph_style ADD COLUMN IF NOT EXISTS target_id varchar(255)")
            .execute()

        connection.prepareStatement(
            """
            UPDATE text_style
            SET target_id = definition->>'id'
            WHERE definition->>'type' = 'com.quadient.migration.persistence.migrationmodel.TextStyleEntityRef'
            """.trimIndent()
        ).executeUpdate()

        connection.prepareStatement(
            """
            UPDATE paragraph_style
            SET target_id = definition->>'id'
            WHERE definition->>'type' = 'com.quadient.migration.persistence.migrationmodel.ParagraphStyleEntityRef'
            """.trimIndent()
        ).executeUpdate()

        connection.prepareStatement(
            """
            UPDATE text_style
            SET definition = definition - 'type'
            WHERE definition->>'type' = 'com.quadient.migration.persistence.migrationmodel.TextStyleDefinitionEntity'
            """.trimIndent()
        ).executeUpdate()

        connection.prepareStatement(
            """
            UPDATE paragraph_style
            SET definition = definition - 'type'
            WHERE definition->>'type' = 'com.quadient.migration.persistence.migrationmodel.ParagraphStyleDefinitionEntity'
            """.trimIndent()
        ).executeUpdate()

        connection.prepareStatement(
            """
            UPDATE text_style
            SET definition = '{}'::jsonb
            WHERE definition->>'type' = 'com.quadient.migration.persistence.migrationmodel.TextStyleEntityRef'
            """.trimIndent()
        ).executeUpdate()

        connection.prepareStatement(
            """
            UPDATE paragraph_style
            SET definition = '{}'::jsonb
            WHERE definition->>'type' = 'com.quadient.migration.persistence.migrationmodel.ParagraphStyleEntityRef'
            """.trimIndent()
        ).executeUpdate()

        // Split style mapping representation in mapping table to explicit targetId + definition fields.
        migrateStyleMappings(connection, "TextStyle")
        migrateStyleMappings(connection, "ParagraphStyle")
    }

    // Uses createStatement with string interpolation instead of prepareStatement
    // to avoid JDBC ? parameter placeholder conflicting with PostgreSQL JSONB ? operator.
    private fun migrateStyleMappings(connection: java.sql.Connection, type: String) {
        val refPredicate = """
            mappings ? 'definition'
            AND jsonb_typeof(mappings->'definition') = 'object'
            AND (mappings->'definition' ? 'targetId')
            AND (((mappings->'definition')::jsonb - 'type'::text - 'targetId'::text) = '{}'::jsonb)
        """.trimIndent()

        connection.createStatement().apply {
            executeUpdate(
                """
                UPDATE mapping
                SET mappings =
                    jsonb_set(
                        jsonb_set(mappings, '{targetId}', to_jsonb(mappings->'definition'->>'targetId'), true),
                        '{definition}', 'null'::jsonb, true
                    )
                WHERE type = '$type'
                  AND $refPredicate
                """.trimIndent()
            )
            close()
        }

        connection.createStatement().apply {
            executeUpdate(
                """
                UPDATE mapping
                SET mappings =
                    jsonb_set(
                        CASE
                            WHEN mappings ? 'targetId' THEN mappings
                            ELSE jsonb_set(mappings, '{targetId}', 'null'::jsonb, true)
                        END,
                        '{definition}', (mappings->'definition')::jsonb - 'type'::text, true
                    )
                WHERE type = '$type'
                  AND mappings ? 'definition'
                  AND jsonb_typeof(mappings->'definition') = 'object'
                  AND NOT ($refPredicate)
                  AND (mappings->'definition' ? 'type')
                """.trimIndent()
            )
            close()
        }
    }
}
