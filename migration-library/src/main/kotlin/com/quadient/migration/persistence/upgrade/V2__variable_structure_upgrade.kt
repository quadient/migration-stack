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
        val updateStmt = connection.prepareStatement("UPDATE variable_structure SET structure = ?::jsonb WHERE id = ?")

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
        val updateStmt = connection.prepareStatement("UPDATE mapping SET mappings = ?::jsonb WHERE id = ?")

        selectStmt.use { sel ->
            val rs = sel.executeQuery()
            while (rs.next()) {
                val variableStructureId = rs.getString("id")
                val outerMappingsJson = rs.getString("mappings")

                try {
                    val outerMappings: MutableMap<String, Any?> = mapper.readValue(outerMappingsJson)

                    @Suppress("UNCHECKED_CAST") val innerMappings = outerMappings["mappings"] as? MutableMap<String, String>
                    if (innerMappings != null) {
                        val newInnerMappings = innerMappings.mapValues { (_, path) ->
                            VariablePathData(path, null)
                        }
                        outerMappings["mappings"] = newInnerMappings
                        val newJson = mapper.writeValueAsString(outerMappings)

                        updateStmt.setString(1, newJson)
                        updateStmt.setString(2, variableStructureId)
                        updateStmt.addBatch()
                    }
                } catch (ex: Exception) {
                    System.err.println("Error migrating mapping id=$variableStructureId: ${ex.message}")
                }
            }
            rs.close()
            updateStmt.executeBatch()
        }
        updateStmt.close()
    }
}