package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.sql.Connection

class V15__document_object_options_entity_upgrade : BaseJavaMigration() {
    private val mapper = jacksonObjectMapper()

    override fun migrate(context: Context) {
        migrateDocumentObjectOptions(context.connection)
    }

    private fun migrateDocumentObjectOptions(connection: Connection) {
        val selectStmt = connection.prepareStatement("SELECT id, options FROM document_object WHERE options IS NOT NULL")
        val updateStmt = connection.prepareStatement("UPDATE document_object SET options = ?::jsonb WHERE id = ?")

        selectStmt.use { sel ->
            val rs = sel.executeQuery()
            while (rs.next()) {
                val id = rs.getObject("id")
                val optionsJson = rs.getString("options")

                try {
                    val node = mapper.readTree(optionsJson) as? ObjectNode ?: continue
                    val migratedNode = migrateOptionsNode(node) ?: continue
                    updateStmt.setString(1, mapper.writeValueAsString(migratedNode))
                    updateStmt.setObject(2, id)
                    updateStmt.addBatch()
                } catch (ex: Exception) {
                    System.err.println("Error migrating document_object options id=$id: ${ex.message}")
                    throw ex
                }
            }
            rs.close()
            updateStmt.executeBatch()
        }
        updateStmt.close()
    }

    private fun migrateOptionsNode(node: ObjectNode): ObjectNode? {
        val oldType = node.get("type")?.asString() ?: return null
        if (oldType != "com.quadient.migration.shared.PageOptions") {
            throw IllegalArgumentException("Unexpected type in options: $oldType")
        }

        node.put("type", "PageOptionsEntity")
        return node
    }
}
