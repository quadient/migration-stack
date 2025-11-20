package com.quadient.migration.persistence.upgrade

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.quadient.migration.shared.SkipOptions
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection

class V6__skip_upgrade : BaseJavaMigration() {
    private val mapper = jacksonObjectMapper()

    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        migrateDocumentObjects(connection)
        migrateImages(connection)
    }

    fun migrateDocumentObjects(connection: Connection) {
        // Add 'skip' column with default value
        val addColumnStmt = connection.prepareStatement("""ALTER TABLE document_object ADD COLUMN IF NOT EXISTS skip jsonb NOT NULL DEFAULT '{"skipped":false, "placeholder": null, "reason": null}'""")
        addColumnStmt.executeUpdate()
        addColumnStmt.close()

        // Remove default constraint
        val dropDefaultStmt = connection.prepareStatement("ALTER TABLE document_object ALTER COLUMN skip DROP DEFAULT")
        dropDefaultStmt.executeUpdate()
        dropDefaultStmt.close()

        // Update existing 'Unsupported' document objects if needed
        val selectStmt = connection.prepareStatement("SELECT id, project_name FROM document_object WHERE type = 'Unsupported'")
        val updateStmt = connection.prepareStatement("UPDATE document_object SET type = ?, skip = ?::jsonb WHERE id = ?")
        selectStmt.use { sel ->
            val rs = sel.executeQuery()
            while (rs.next()) {
                val id = rs.getObject("id")
                val project = rs.getString("project_name")
                val skipJson = """{"skipped":true,"placeholder":null,"reason":"Unsupported document object type"}"""

                updateStmt.setString(1, "Block")
                updateStmt.setString(2, skipJson)
                updateStmt.setObject(3, id)
                updateStmt.addBatch()

                // Update mapping
                val mappingSelectStmt = connection.prepareStatement("SELECT mappings FROM mapping WHERE id = ? AND type = 'DocumentObject'")
                mappingSelectStmt.setObject(1, id)
                val mapping = mappingSelectStmt.use {
                    val qry = it.executeQuery()
                    if (qry.next()) {
                        qry.getString("mappings")
                    } else {
                        null
                    }
                }

                val originalDef = mapper.readTree(mapping ?: "{}") as ObjectNode
                originalDef.putPOJO("skip", SkipOptions(skipped = true, placeholder = null, reason = "Unsupported document object type"))
                val mappingJson = mapper.writeValueAsString(originalDef)

                if (mapping != null) {
                    val updateMappingStmt = connection.prepareStatement("UPDATE mapping SET mappings = ?::jsonb WHERE id = ? AND type = 'DocumentObject'")
                    updateMappingStmt.setString(1, mappingJson)
                    updateMappingStmt.setObject(2, id)
                    updateMappingStmt.executeUpdate()
                    updateMappingStmt.close()
                } else {
                    val insertMappingStmt = connection.prepareStatement("INSERT INTO mapping (id, project_name, type, mappings) VALUES (?, ?, 'DocumentObject', ?::jsonb)")
                    insertMappingStmt.setObject(1, id)
                    insertMappingStmt.setString(2, project)
                    insertMappingStmt.setString(3, mappingJson)
                    insertMappingStmt.executeUpdate()
                    insertMappingStmt.close()
                }
            }
            updateStmt.executeBatch()
            rs.close()
        }

        updateStmt.close()
        addColumnStmt.close()
        dropDefaultStmt.close()
    }

    fun migrateImages(connection: Connection) {
        // Add 'skip' column with default value
        val addColumnStmt = connection.prepareStatement("""ALTER TABLE image ADD COLUMN IF NOT EXISTS skip jsonb NOT NULL DEFAULT '{"skipped":false, "placeholder": null, "reason": null}'""")
        addColumnStmt.executeUpdate()
        addColumnStmt.close()

        // Remove default constraint
        val dropDefaultStmt = connection.prepareStatement("ALTER TABLE image ALTER COLUMN skip DROP DEFAULT")
        dropDefaultStmt.executeUpdate()
        dropDefaultStmt.close()

        // Update existing 'Unsupported' document objects if needed
        val selectStmt = connection.prepareStatement("SELECT id, image_type, project_name FROM image WHERE image_type = 'Unknown' OR (source_path IS NULL OR source_path = '')")
        val updateStmt = connection.prepareStatement("UPDATE image SET skip = ?::jsonb WHERE id = ?")

        selectStmt.use { sel ->
            val rs = sel.executeQuery()
            while (rs.next()) {
                val id = rs.getObject("id")
                val type = rs.getString("image_type")
                val project = rs.getString("project_name")
                val reason = if (type == "Unknown") {
                    "Unknown image type $id"
                } else {
                    "Image without source path $id"
                }

                val skipJson = """{"skipped":true,"placeholder":null,"reason":"$reason"}"""

                updateStmt.setString(1, skipJson)
                updateStmt.setObject(2, id)
                updateStmt.addBatch()

                // Update mapping
                val mappingSelectStmt = connection.prepareStatement("SELECT mappings FROM mapping WHERE id = ? AND type = 'Image'")
                mappingSelectStmt.setObject(1, id)
                val mapping = mappingSelectStmt.use {
                    val qry = it.executeQuery()
                    if (qry.next()) {
                        qry.getString("mappings")
                    } else {
                        null
                    }
                }

                val originalDef = mapper.readTree(mapping ?: "{}") as ObjectNode
                originalDef.putPOJO("skip", SkipOptions(skipped = true, placeholder = null, reason = reason))
                val mappingJson = mapper.writeValueAsString(originalDef)

                if (mapping != null) {
                    val updateMappingStmt = connection.prepareStatement("UPDATE mapping SET mappings = ?::jsonb WHERE id = ? AND type = 'Image'")
                    updateMappingStmt.setString(1, mappingJson)
                    updateMappingStmt.setObject(2, id)
                    updateMappingStmt.executeUpdate()
                    updateMappingStmt.close()
                } else {
                    val insertMappingStmt = connection.prepareStatement("INSERT INTO mapping (id, project_name, type, mappings) VALUES (?, ?, 'Image', ?::jsonb)")
                    insertMappingStmt.setObject(1, id)
                    insertMappingStmt.setString(2, project)
                    insertMappingStmt.setString(3, mappingJson)
                    insertMappingStmt.executeUpdate()
                    insertMappingStmt.close()
                }
            }
            updateStmt.executeBatch()
            rs.close()
        }

        updateStmt.close()
        addColumnStmt.close()
        dropDefaultStmt.close()
    }
}