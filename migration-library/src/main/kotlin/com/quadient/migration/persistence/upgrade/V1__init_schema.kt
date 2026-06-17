package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V1__init_schema : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val connection = context.connection
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS variable (
                    id VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    name VARCHAR(255),
                    origin_locations TEXT[] NOT NULL,
                    custom_fields JSONB NOT NULL,
                    last_updated TIMESTAMP NOT NULL,
                    created TIMESTAMP NOT NULL,
                    data_type VARCHAR(50) NOT NULL,
                    default_value VARCHAR(255),
                    PRIMARY KEY (id, project_name)
                )
                """.trimIndent()
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS document_object (
                    id VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    name VARCHAR(255),
                    origin_locations TEXT[] NOT NULL,
                    custom_fields JSONB NOT NULL,
                    last_updated TIMESTAMP NOT NULL,
                    created TIMESTAMP NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    content JSONB,
                    internal BOOLEAN NOT NULL,
                    target_folder VARCHAR(255),
                    display_rule_ref VARCHAR(255),
                    variable_structure_ref VARCHAR(255),
                    base_template VARCHAR(255),
                    options JSONB,
                    PRIMARY KEY (id, project_name)
                )
                """.trimIndent()
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS text_style (
                    id VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    name VARCHAR(255),
                    origin_locations TEXT[] NOT NULL,
                    custom_fields JSONB NOT NULL,
                    last_updated TIMESTAMP NOT NULL,
                    created TIMESTAMP NOT NULL,
                    definition JSONB NOT NULL,
                    PRIMARY KEY (id, project_name)
                )
                """.trimIndent()
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS paragraph_style (
                    id VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    name VARCHAR(255),
                    origin_locations TEXT[] NOT NULL,
                    custom_fields JSONB NOT NULL,
                    last_updated TIMESTAMP NOT NULL,
                    created TIMESTAMP NOT NULL,
                    definition JSONB NOT NULL,
                    PRIMARY KEY (id, project_name)
                )
                """.trimIndent()
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS variable_structure (
                    id VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    name VARCHAR(255),
                    origin_locations TEXT[] NOT NULL,
                    custom_fields JSONB NOT NULL,
                    last_updated TIMESTAMP NOT NULL,
                    created TIMESTAMP NOT NULL,
                    structure JSONB NOT NULL,
                    PRIMARY KEY (id, project_name)
                )
                """.trimIndent()
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS display_rule (
                    id VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    name VARCHAR(255),
                    origin_locations TEXT[] NOT NULL,
                    custom_fields JSONB NOT NULL,
                    last_updated TIMESTAMP NOT NULL,
                    created TIMESTAMP NOT NULL,
                    definition JSONB,
                    PRIMARY KEY (id, project_name)
                )
                """.trimIndent()
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS image (
                    id VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    name VARCHAR(255),
                    origin_locations TEXT[] NOT NULL,
                    custom_fields JSONB NOT NULL,
                    last_updated TIMESTAMP NOT NULL,
                    created TIMESTAMP NOT NULL,
                    source_path VARCHAR(255),
                    image_type VARCHAR(50) NOT NULL,
                    options JSONB,
                    target_folder VARCHAR(255),
                    PRIMARY KEY (id, project_name)
                )
                """.trimIndent()
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS status_tracking (
                    id VARCHAR(255) NOT NULL,
                    resource_type VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    status_events JSONB NOT NULL,
                    PRIMARY KEY (id, resource_type, project_name)
                )
                """.trimIndent()
            )
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS mapping (
                    id VARCHAR(255) NOT NULL,
                    type VARCHAR(255) NOT NULL,
                    project_name VARCHAR(50) NOT NULL,
                    mappings JSONB NOT NULL,
                    PRIMARY KEY (id, type, project_name)
                )
                """.trimIndent()
            )
        }
    }
}