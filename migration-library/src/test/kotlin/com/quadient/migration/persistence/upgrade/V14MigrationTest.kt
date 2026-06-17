package com.quadient.migration.persistence.upgrade

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager

class V14MigrationTest {

    @Test
    fun `V14 migration converts metadata from map to list format`() {
        PostgreSQLContainer("postgres:16-alpine").use { postgres ->
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
                .target("13")
                .load()
                .migrate()

            connection(postgres).use { conn ->
                insertDocumentObject(conn, "doc-single-key", """{"docMeta":[{"type":"string","value":"v1"}]}""")
                insertDocumentObject(conn, "doc-multi-key", """{"key1":[{"type":"string","value":"a"}],"key2":[{"type":"bool","value":true}]}""")
                insertDocumentObject(conn, "doc-empty-meta", """{}""")
                insertDocumentObject(conn, "doc-multi-value", """{"tags":[{"type":"string","value":"a"},{"type":"string","value":"b"}]}""")
                insertDocumentObject(conn, "doc-already-array", """[]""")
                insertImage(conn, "img1", """{"imgMeta":[{"type":"string","value":"landscape"}]}""")
                insertDisplayRule(conn, "rule1", """{"ruleMeta":[{"type":"string","value":"v2"}]}""")
                insertAttachment(conn, "att1")
            }

            Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:com/quadient/migration/persistence/upgrade")
                .target("14")
                .load()
                .migrate()

            connection(postgres).use { conn ->
                assertJsonEquals(
                    """[{"type":"IcmMetadata","key":"docMeta","value":[{"type":"string","value":"v1"}]}]""",
                    queryMetadata(conn, "document_object", "doc-single-key")
                )

                val multiKeyArray = Json.parseToJsonElement(queryMetadata(conn, "document_object", "doc-multi-key")).jsonArray
                assertEquals(2, multiKeyArray.size)
                assertEquals(setOf("key1", "key2"), multiKeyArray.map { it.jsonObject["key"]!!.jsonPrimitive.content }.toSet())

                assertJsonEquals("[]", queryMetadata(conn, "document_object", "doc-empty-meta"))

                assertJsonEquals(
                    """[{"type":"IcmMetadata","key":"tags","value":[{"type":"string","value":"a"},{"type":"string","value":"b"}]}]""",
                    queryMetadata(conn, "document_object", "doc-multi-value")
                )

                assertJsonEquals("[]", queryMetadata(conn, "document_object", "doc-already-array"))

                assertJsonEquals(
                    """[{"type":"IcmMetadata","key":"imgMeta","value":[{"type":"string","value":"landscape"}]}]""",
                    queryMetadata(conn, "image", "img1")
                )

                assertJsonEquals(
                    """[{"type":"IcmMetadata","key":"ruleMeta","value":[{"type":"string","value":"v2"}]}]""",
                    queryMetadata(conn, "display_rule", "rule1")
                )

                assertJsonEquals("[]", queryMetadata(conn, "attachment", "att1"))
            }
        }
    }

    private fun insertDocumentObject(conn: Connection, id: String, metadata: String) {
        conn.prepareStatement(
            """
            INSERT INTO document_object (id, project_name, name, origin_locations, custom_fields, type, internal, metadata, skip, last_updated, created)
            VALUES (?, 'test', ?, '{}', '{}', 'Block', true, ?::jsonb, '{"skipped":false,"reason":null,"placeholder":null}'::jsonb, NOW(), NOW())
            """.trimIndent()
        ).apply {
            setString(1, id)
            setString(2, id)
            setString(3, metadata)
        }.executeUpdate()
    }

    private fun insertImage(conn: Connection, id: String, metadata: String) {
        conn.prepareStatement(
            """
            INSERT INTO image (id, project_name, name, origin_locations, custom_fields, image_type, metadata, skip, last_updated, created)
            VALUES (?, 'test', ?, '{}', '{}', 'Jpeg', ?::jsonb, '{"skipped":false,"reason":null,"placeholder":null}'::jsonb, NOW(), NOW())
            """.trimIndent()
        ).apply {
            setString(1, id)
            setString(2, id)
            setString(3, metadata)
        }.executeUpdate()
    }

    private fun insertDisplayRule(conn: Connection, id: String, metadata: String) {
        conn.prepareStatement(
            """
            INSERT INTO display_rule (id, project_name, name, origin_locations, custom_fields, internal, metadata, last_updated, created)
            VALUES (?, 'test', ?, '{}', '{}', true, ?::json, NOW(), NOW())
            """.trimIndent()
        ).apply {
            setString(1, id)
            setString(2, id)
            setString(3, metadata)
        }.executeUpdate()
    }

    private fun insertAttachment(conn: Connection, id: String) {
        conn.prepareStatement(
            """
            INSERT INTO attachment (id, project_name, name, origin_locations, custom_fields, attachment_type, skip, last_updated, created)
            VALUES (?, 'test', ?, '{}', '{}', 'Attachment', '{"skipped":false,"reason":null,"placeholder":null}'::jsonb, NOW(), NOW())
            """.trimIndent()
        ).apply {
            setString(1, id)
            setString(2, id)
        }.executeUpdate()
    }

    private fun queryMetadata(conn: Connection, table: String, id: String): String {
        val rs = conn.prepareStatement("SELECT metadata::text FROM $table WHERE id = ?")
            .apply { setString(1, id) }
            .executeQuery()
        assert(rs.next()) { "Expected row '$id' in '$table' to exist" }
        return rs.getString("metadata")
    }

    private fun connection(postgres: PostgreSQLContainer<*>): Connection =
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)

    private fun assertJsonEquals(expected: String, actual: String) =
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual))
}

