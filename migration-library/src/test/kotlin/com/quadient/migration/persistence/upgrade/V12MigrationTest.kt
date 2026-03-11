package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager

/**
 * Integration test for V12 migration that splits style definition/ref into separate definition + target_id columns.
 * Uses a real PostgreSQL container to verify the Flyway migration transforms old-format data correctly.
 */
class V12MigrationTest {

    @Test
    fun `V12 migration correctly splits style definitions and mappings`() {
        PostgreSQLContainer("postgres:16-alpine").use { postgres ->
            // given
            postgres.start()

            Database.connect(
                url = postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgres.username,
                password = postgres.password
            )

            Flyway.configure().dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:com/quadient/migration/persistence/upgrade").target("11").load().migrate()

            connection(postgres).use { insertOldFormatData(it) }

            // when
            Flyway.configure().dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:com/quadient/migration/persistence/upgrade").target("12").load().migrate()

            // then
            connection(postgres).use { conn ->
                verifyTextStyleRef(conn)
                verifyTextStyleDefinition(conn)
                verifyParagraphStyleRef(conn)
                verifyParagraphStyleDefinition(conn)
                verifyTextStyleRefMapping(conn)
                verifyTextStyleDefMapping(conn)
                verifyParagraphStyleRefMapping(conn)
                verifyParagraphStyleDefMapping(conn)
            }
        }
    }

    private fun insertOldFormatData(conn: Connection) {
        val now = "NOW()"

        conn.prepareStatement(
            """
            INSERT INTO text_style (id, project_name, name, origin_locations, custom_fields, definition, last_updated, created)
            VALUES ('ts-ref', 'test', 'Text Ref', '{}', '{}',
                '{"type":"com.quadient.migration.persistence.migrationmodel.TextStyleEntityRef","id":"target-text"}'::jsonb,
                $now, $now)
            """.trimIndent()
        ).executeUpdate()

        conn.prepareStatement(
            """
            INSERT INTO text_style (id, project_name, name, origin_locations, custom_fields, definition, last_updated, created)
            VALUES ('ts-def', 'test', 'Text Def', '{}', '{}',
                '{"type":"com.quadient.migration.persistence.migrationmodel.TextStyleDefinitionEntity","fontFamily":"Arial","bold":true,"size":"3.5278mm"}'::jsonb,
                $now, $now)
            """.trimIndent()
        ).executeUpdate()

        conn.prepareStatement(
            """
            INSERT INTO paragraph_style (id, project_name, name, origin_locations, custom_fields, definition, last_updated, created)
            VALUES ('ps-ref', 'test', 'Para Ref', '{}', '{}',
                '{"type":"com.quadient.migration.persistence.migrationmodel.ParagraphStyleEntityRef","id":"target-para"}'::jsonb,
                $now, $now)
            """.trimIndent()
        ).executeUpdate()

        conn.prepareStatement(
            """
            INSERT INTO paragraph_style (id, project_name, name, origin_locations, custom_fields, definition, last_updated, created)
            VALUES ('ps-def', 'test', 'Para Def', '{}', '{}',
                '{"type":"com.quadient.migration.persistence.migrationmodel.ParagraphStyleDefinitionEntity","alignment":"Left","leftIndent":"3.5278mm"}'::jsonb,
                $now, $now)
            """.trimIndent()
        ).executeUpdate()

        conn.prepareStatement(
            """
            INSERT INTO mapping (id, type, project_name, mappings)
            VALUES ('ts-ref', 'TextStyle', 'test',
                '{"name":"mapped","definition":{"type":"TextStyleRef","targetId":"mapped-target"}}'::jsonb)
            """.trimIndent()
        ).executeUpdate()

        conn.prepareStatement(
            """
            INSERT INTO mapping (id, type, project_name, mappings)
            VALUES ('ts-def', 'TextStyle', 'test',
                '{"name":"mapped","definition":{"type":"TextStyleDef","fontFamily":"Verdana","bold":false,"size":"3.5278mm"}}'::jsonb)
            """.trimIndent()
        ).executeUpdate()

        conn.prepareStatement(
            """
            INSERT INTO mapping (id, type, project_name, mappings)
            VALUES ('ps-ref', 'ParagraphStyle', 'test',
                '{"name":"mapped","definition":{"type":"ParagraphStyleRef","targetId":"mapped-para-target"}}'::jsonb)
            """.trimIndent()
        ).executeUpdate()

        conn.prepareStatement(
            """
            INSERT INTO mapping (id, type, project_name, mappings)
            VALUES ('ps-def', 'ParagraphStyle', 'test',
                '{"name":"mapped","definition":{"type":"ParagraphStyleDef","alignment":"Center"}}'::jsonb)
            """.trimIndent()
        ).executeUpdate()
    }

    private fun verifyTextStyleRef(conn: Connection) {
        val rs = conn.prepareStatement(
            "SELECT target_id, definition::text FROM text_style WHERE id = 'ts-ref'"
        ).executeQuery()
        rs.next()
        assertEquals("target-text", rs.getString("target_id"))
        assertEquals("{}", rs.getString("definition"))
    }

    private fun verifyTextStyleDefinition(conn: Connection) {
        val rs = conn.prepareStatement(
            "SELECT target_id, definition::text FROM text_style WHERE id = 'ts-def'"
        ).executeQuery()
        rs.next()
        assertNull(rs.getString("target_id"), "text style def: target_id should be null")
        assertJsonEquals(
            """{"fontFamily":"Arial","bold":true,"size":"10.00pt"}""", rs.getString("definition")
        )
    }

    private fun verifyParagraphStyleRef(conn: Connection) {
        val rs = conn.prepareStatement(
            "SELECT target_id, definition::text FROM paragraph_style WHERE id = 'ps-ref'"
        ).executeQuery()
        rs.next()
        assertEquals("target-para", rs.getString("target_id"))
        assertEquals("{}", rs.getString("definition"))
    }

    private fun verifyParagraphStyleDefinition(conn: Connection) {
        val rs = conn.prepareStatement(
            "SELECT target_id, definition::text FROM paragraph_style WHERE id = 'ps-def'"
        ).executeQuery()
        rs.next()
        assertNull(rs.getString("target_id"), "paragraph style def: target_id should be null")
        assertJsonEquals(
            """{"alignment":"Left","leftIndent":"3.5278mm"}""", rs.getString("definition")
        )
    }

    private fun verifyTextStyleRefMapping(conn: Connection) {
        val rs = conn.prepareStatement(
            "SELECT mappings::text FROM mapping WHERE id = 'ts-ref' AND type = 'TextStyle'"
        ).executeQuery()
        rs.next()
        assertJsonEquals(
            """{"name":"mapped","targetId":"mapped-target","definition":null}""", rs.getString("mappings")
        )
    }

    private fun verifyTextStyleDefMapping(conn: Connection) {
        val rs = conn.prepareStatement(
            "SELECT mappings::text FROM mapping WHERE id = 'ts-def' AND type = 'TextStyle'"
        ).executeQuery()
        rs.next()
        assertJsonEquals(
            """{"name":"mapped","targetId":null,"definition":{"fontFamily":"Verdana","bold":false,"size":"10.00pt"}}""",
            rs.getString("mappings")
        )
    }

    private fun verifyParagraphStyleRefMapping(conn: Connection) {
        val rs = conn.prepareStatement(
            "SELECT mappings::text FROM mapping WHERE id = 'ps-ref' AND type = 'ParagraphStyle'"
        ).executeQuery()
        rs.next()
        assertJsonEquals(
            """{"name":"mapped","targetId":"mapped-para-target","definition":null}""", rs.getString("mappings")
        )
    }

    private fun verifyParagraphStyleDefMapping(conn: Connection) {
        val rs = conn.prepareStatement(
            "SELECT mappings::text FROM mapping WHERE id = 'ps-def' AND type = 'ParagraphStyle'"
        ).executeQuery()
        rs.next()
        assertJsonEquals(
            """{"name":"mapped","targetId":null,"definition":{"alignment":"Center"}}""", rs.getString("mappings")
        )
    }

    private fun connection(postgres: PostgreSQLContainer<*>) =
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)

    private fun assertJsonEquals(expected: String, actual: String, message: String = "") =
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual), message)
}
