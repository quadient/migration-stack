package com.quadient.migration.persistence.upgrade

import com.quadient.migration.Postgres.Companion.POSTGRES_CONTAINER
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager

/**
 * Integration test for V16 migration that introduces the base_template table and converts the
 * document_object/display_rule base_template columns (and their mapping counterparts) from a plain
 * path string into a discriminated jsonb reference (path or id).
 */
class V16MigrationTest {

    @Test
    fun `V16 migration converts base template references to jsonb`() {
        PostgreSQLContainer(POSTGRES_CONTAINER).use { postgres ->
            postgres.start()

            Database.connect(
                url = postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgres.username,
                password = postgres.password
            )

            Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:com/quadient/migration/persistence/upgrade")
                .target("15")
                .load()
                .migrate()

            connection(postgres).use { conn ->
                insertDocumentObject(conn, "doc-with-base-template", "templates/base.xml")
                insertDocumentObject(conn, "doc-without-base-template", null)
                insertDisplayRule(conn, "rule-with-base-template", "templates/base-rule.xml")
                insertDisplayRule(conn, "rule-without-base-template", null)

                insertMapping(
                    conn, "map-doc", "DocumentObject",
                    """{"name":"mapped","baseTemplate":"templates/mapped.xml"}"""
                )
                insertMapping(
                    conn, "map-doc-null", "DocumentObject",
                    """{"name":"mapped","baseTemplate":null}"""
                )
                insertMapping(
                    conn, "map-rule", "DisplayRule",
                    """{"name":"mapped","baseTemplate":"templates/mapped-rule.xml"}"""
                )
                insertMapping(
                    conn, "map-area", "Area",
                    """{"name":"mapped","areas":{}}"""
                )
            }

            Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:com/quadient/migration/persistence/upgrade")
                .target("16")
                .load()
                .migrate()

            connection(postgres).use { conn ->
                assertJsonEquals(
                    """{"type":"BaseTemplatePath","path":"templates/base.xml"}""",
                    queryColumn(conn, "document_object", "base_template", "doc-with-base-template")!!
                )
                assertNull(queryColumn(conn, "document_object", "base_template", "doc-without-base-template"))

                assertJsonEquals(
                    """{"type":"BaseTemplatePath","path":"templates/base-rule.xml"}""",
                    queryColumn(conn, "display_rule", "base_template", "rule-with-base-template")!!
                )
                assertNull(queryColumn(conn, "display_rule", "base_template", "rule-without-base-template"))

                assertJsonEquals(
                    """{"name":"mapped","baseTemplate":{"type":"BaseTemplatePath","path":"templates/mapped.xml"}}""",
                    queryMapping(conn, "map-doc", "DocumentObject")
                )
                assertJsonEquals(
                    """{"name":"mapped","baseTemplate":null}""",
                    queryMapping(conn, "map-doc-null", "DocumentObject")
                )
                assertJsonEquals(
                    """{"name":"mapped","baseTemplate":{"type":"BaseTemplatePath","path":"templates/mapped-rule.xml"}}""",
                    queryMapping(conn, "map-rule", "DisplayRule")
                )
                assertJsonEquals(
                    """{"name":"mapped","areas":{}}""",
                    queryMapping(conn, "map-area", "Area")
                )
            }
        }
    }

    private fun insertDocumentObject(conn: Connection, id: String, baseTemplate: String?) {
        conn.prepareStatement(
            """
            INSERT INTO document_object (id, project_name, name, origin_locations, custom_fields, type, internal, metadata, skip, base_template, last_updated, created)
            VALUES (?, 'test', ?, '{}', '{}', 'Block', true, '[]', '{"skipped":false,"reason":null,"placeholder":null}'::jsonb, ?, NOW(), NOW())
            """.trimIndent()
        ).apply {
            setString(1, id)
            setString(2, id)
            setString(3, baseTemplate)
        }.executeUpdate()
    }

    private fun insertDisplayRule(conn: Connection, id: String, baseTemplate: String?) {
        conn.prepareStatement(
            """
            INSERT INTO display_rule (id, project_name, name, origin_locations, custom_fields, internal, metadata, base_template, last_updated, created)
            VALUES (?, 'test', ?, '{}', '{}', true, '{}', ?, NOW(), NOW())
            """.trimIndent()
        ).apply {
            setString(1, id)
            setString(2, id)
            setString(3, baseTemplate)
        }.executeUpdate()
    }

    private fun insertMapping(conn: Connection, id: String, type: String, mappings: String) {
        conn.prepareStatement(
            """
            INSERT INTO mapping (id, type, project_name, mappings)
            VALUES (?, ?, 'test', ?::jsonb)
            """.trimIndent()
        ).apply {
            setString(1, id)
            setString(2, type)
            setString(3, mappings)
        }.executeUpdate()
    }

    private fun queryColumn(conn: Connection, table: String, column: String, id: String): String? {
        val rs = conn.prepareStatement("SELECT $column::text AS value FROM $table WHERE id = ?")
            .apply { setString(1, id) }
            .executeQuery()
        assert(rs.next()) { "Expected row '$id' in '$table' to exist" }
        return rs.getString("value")
    }

    private fun queryMapping(conn: Connection, id: String, type: String): String {
        val rs = conn.prepareStatement("SELECT mappings::text FROM mapping WHERE id = ? AND type = ?")
            .apply {
                setString(1, id)
                setString(2, type)
            }
            .executeQuery()
        assert(rs.next()) { "Expected mapping row '$id' of type '$type' to exist" }
        return rs.getString("mappings")
    }

    private fun connection(postgres: PostgreSQLContainer): Connection =
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)

    private fun assertJsonEquals(expected: String, actual: String) =
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual))
}
