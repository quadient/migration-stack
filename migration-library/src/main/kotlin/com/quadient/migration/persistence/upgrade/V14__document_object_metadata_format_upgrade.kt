package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.sql.Connection

class V14__document_object_metadata_format_upgrade : BaseJavaMigration() {
    private val mapper = jacksonObjectMapper()

    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        connection.createStatement().use { stmt ->
            stmt.execute("ALTER TABLE attachment ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '[]'")
        }

        for (table in listOf("document_object", "image", "display_rule")) {
            migrateMetadata(connection, table)
        }
    }

    private fun migrateMetadata(connection: Connection, table: String) {
        val selectStmt = connection.prepareStatement("SELECT id, metadata FROM $table")
        val updateStmt = connection.prepareStatement("UPDATE $table SET metadata = ?::jsonb WHERE id = ?")

        selectStmt.use { sel ->
            val rs = sel.executeQuery()
            while (rs.next()) {
                val id = rs.getObject("id")
                val metadataJson = rs.getString("metadata")

                try {
                    val oldMetadata = mapper.readTree(metadataJson)
                    if (oldMetadata.isObject) {
                        val newMetadata = convertMetadata(oldMetadata)
                        val newJson = mapper.writeValueAsString(newMetadata)
                        updateStmt.setString(1, newJson)
                        updateStmt.setObject(2, id)
                        updateStmt.addBatch()
                    }
                } catch (ex: Exception) {
                    System.err.println("Error migrating $table metadata id=$id: ${ex.message}")
                    throw ex
                }
            }
            rs.close()
            updateStmt.executeBatch()
        }
        updateStmt.close()
    }

    private fun convertMetadata(oldMetadata: JsonNode): ArrayNode {
        val newArray = mapper.createArrayNode()
        for (entry in oldMetadata.properties()) {
            val obj = mapper.createObjectNode()
            obj.put("type", "IcmMetadata")
            obj.put("key", entry.key)
            obj.set("value", entry.value)
            newArray.add(obj)
        }
        return newArray
    }
}


