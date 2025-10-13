package com.quadient.migration.persistence.upgrade

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class V3__display_rule_function_support_upgrade : BaseJavaMigration() {
    private val mapper = jacksonObjectMapper()

    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        val selectStmt = connection.prepareStatement("SELECT id, definition FROM display_rule")
        val updateStmt = connection.prepareStatement("UPDATE display_rule SET definition = ?::jsonb WHERE id = ?")
        selectStmt.use { sel ->
            val rs = sel.executeQuery()
            while (rs.next()) {
                val id = rs.getObject("id")
                val defJson = rs.getString("definition")

                try {
                    val originalDef = mapper.readTree(defJson)
                    transformGroup(originalDef.get("group"))
                    val newJson = mapper.writeValueAsString(originalDef)
                    updateStmt.setString(1, newJson)
                    updateStmt.setObject(2, id)
                    updateStmt.addBatch()
                } catch (ex: Exception) {
                    System.err.println("Error migrating display_rule id=$id: ${ex.message}")
                }
            }
            rs.close()
            updateStmt.executeBatch()
        }
        updateStmt.close()
    }

    private fun transformGroup(def: JsonNode) {
        for (item in def.get("items")) {
            val type = item.get("type").asText()
            when (type) {
                "com.quadient.migration.shared.Binary" -> {
                    val left = item.get("left") as ObjectNode
                    val right = item.get("right") as ObjectNode
                    left.put("type", "com.quadient.migration.shared.Literal")
                    right.put("type", "com.quadient.migration.shared.Literal")
                }
                "com.quadient.migration.shared.Group" -> {
                    transformGroup(item)
                }
            }
        }
    }
}