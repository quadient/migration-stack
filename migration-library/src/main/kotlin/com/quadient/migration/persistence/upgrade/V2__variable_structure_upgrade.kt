package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.quadient.migration.shared.VariablePathData

class V2__variable_structure_upgrade : BaseJavaMigration() {
    private val mapper = jacksonObjectMapper()

    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        migrateVariableStructureModel(connection)
        migrateVariableStructureMapping(connection)
    }

    private fun migrateVariableStructureModel(connection: Connection) {
        val selectStmt = connection.prepareStatement("SELECT id, structure FROM variable_structure")
        val updateStmt = connection.prepareStatement("UPDATE variable_structure SET structure = ? WHERE id = ?")

        selectStmt.use { sel ->
            val rs = sel.executeQuery()
            while (rs.next()) {
                val id = rs.getObject("id")
                val structureJson = rs.getString("structure")

                try {
                    val originalStructure: Map<String, String> = mapper.readValue(structureJson)
                    val newMap = originalStructure.mapValues { (_, value) ->
                        VariablePathData(value, null)
                    }
                    val newJson = mapper.writeValueAsString(newMap)

                    updateStmt.setString(1, newJson)
                    updateStmt.setObject(2, id)
                    updateStmt.addBatch()
                } catch (ex: Exception) {
                    System.err.println("Error migrating variable_structure id=$id: ${ex.message}")
                }
            }
            rs.close()
            updateStmt.executeBatch()
        }
        updateStmt.close()
    }

    private fun migrateVariableStructureMapping(connection: Connection) {
        val selectStmt =
            connection.prepareStatement("SELECT id, mappings FROM mapping WHERE type = 'VariableStructure'")
        val updateStmt = connection.prepareStatement("UPDATE mapping SET mappings = ? WHERE id = ?")

        selectStmt.use { sel ->
            val rs = sel.executeQuery()
            while (rs.next()) {
                val id = rs.getString("id")
                val mappingsJson = rs.getString("mappings")
                if (mappingsJson.isNullOrBlank()) {
                    System.err.println("Skipping mapping id=$id: mappings is null/blank")
                    continue
                }

                try {
                    val mappings: Map<String, String> = mapper.readValue(mappingsJson)
                    val newMappings = mappings.mapValues { (_, path) ->
                        VariablePathData(path, null)
                    }
                    val newJson = mapper.writeValueAsString(newMappings)

                    updateStmt.setString(1, newJson)
                    updateStmt.setString(2, id)
                    updateStmt.addBatch()
                } catch (ex: Exception) {
                    System.err.println("Error migrating mapping id=$id: ${ex.message}")
                }
            }
            rs.close()
            updateStmt.executeBatch()
        }
        updateStmt.close()
    }
}