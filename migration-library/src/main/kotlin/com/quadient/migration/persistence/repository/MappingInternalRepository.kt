package com.quadient.migration.persistence.repository

import com.quadient.migration.persistence.migrationmodel.MappingEntity
import com.quadient.migration.persistence.migrationmodel.MappingItemEntity
import com.quadient.migration.persistence.table.MappingTable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Types
import kotlin.reflect.KClass

class MappingInternalRepository(val projectName: String) {
    fun listAll(): List<MappingEntity> {
        return transaction {
            MappingEntity.find { MappingTable.projectName eq projectName }.toList()
        }
    }

    fun listByType(type: KClass<out MappingItemEntity>): List<MappingEntity> {
        return transaction {
            MappingEntity.find {
                (MappingTable.projectName eq projectName) and (MappingTable.type eq type.simpleName)
            }.toList()
        }
    }

    inline fun <reified T : MappingItemEntity> find(id: String): T? {
        return transaction {
            val id = CompositeID {
                it[MappingTable.projectName] = projectName
                it[MappingTable.type] =
                    T::class.simpleName ?: throw IllegalArgumentException("Type must have a simple name")
                it[MappingTable.resourceId] = id
            }

            MappingEntity.findById(id)?.let {
                require(it.mapping::class == T::class) {
                    "Mapping type mismatch: expected ${T::class.simpleName}, found ${it.mapping::class.simpleName}"
                }
                it.mapping as T
            }
        }
    }

    fun upsertBatch(entries: Collection<Pair<String, MappingItemEntity>>) {
        if (entries.isEmpty()) return

        val columns = listOf("id", "type", "project_name", "mappings")
        val placeholders = columns.joinToString(",", prefix = "(", postfix = ")") { "?" }
        val values = (1..entries.size).joinToString(",") { placeholders }
        val setOnConflict = columns.filter { it != "id" && it != "type" && it != "project_name" }
            .joinToString(", ") { "$it = EXCLUDED.$it" }

        val sql = """
            INSERT INTO ${MappingTable.nameInDatabaseCase()} (${columns.joinToString(", ")})
            VALUES $values
            ON CONFLICT (id, type, project_name) DO UPDATE SET $setOnConflict
        """.trimIndent()

        transaction {
            val stmt = (connection.connection as java.sql.Connection).prepareStatement(sql)
            var index = 1
            entries.forEach { (id, mapping) ->
                stmt.setString(index++, id)
                stmt.setString(index++, requireNotNull(mapping::class.simpleName))
                stmt.setString(index++, projectName)
                stmt.setObject(index++, Json.encodeToString(mapping), Types.OTHER)
            }
            stmt.executeUpdate()
        }
    }

    fun upsert(id: String, mapping: MappingItemEntity): MappingEntity {
        return transaction {
            val id = CompositeID {
                it[MappingTable.projectName] = projectName
                it[MappingTable.type] = requireNotNull(mapping::class.simpleName)
                it[MappingTable.resourceId] = id
            }

            MappingEntity.findByIdAndUpdate(id) {
                it.mapping = mapping
            } ?: MappingEntity.new(id) {
                this.mapping = mapping
            }
        }
    }

    fun deleteAll() {
        transaction {
            MappingEntity.find { MappingTable.projectName eq projectName }.forEach { it.delete() }
        }
    }
}