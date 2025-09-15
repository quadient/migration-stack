package com.quadient.migration.persistence.upgrade

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class V2__variable_structure_upgrade : BaseJavaMigration() {
    private val mapper = jacksonObjectMapper()

    override fun migrate(context: Context) {
        val connection: Connection = context.connection

        val selectStmt = connection.prepareStatement("SELECT id, structure FROM variable_structure")
        val updateStmt = connection.prepareStatement("UPDATE variable_structure SET structure = ? WHERE id = ?")

        selectStmt.use { sel ->
            val rs = sel.executeQuery()
            while (rs.next()) {
                val id = rs.getObject("id")
                val structureJson = rs.getString("structure")

                val originalStructure: Map<String, String> = mapper.readValue(structureJson)

                val newMap = originalStructure.mapValues { (_, value) ->
                    NameAndPath(name = null, path = value)
                }

                val newJson = mapper.writeValueAsString(newMap)

                updateStmt.setString(1, newJson)
                updateStmt.setObject(2, id)
                updateStmt.addBatch()
            }
            updateStmt.executeBatch()
        }
    }

    data class NameAndPath(val name: String?, val path: String?)
}