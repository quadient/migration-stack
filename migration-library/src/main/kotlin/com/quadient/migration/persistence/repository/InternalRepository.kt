package com.quadient.migration.persistence.repository

import com.quadient.migration.data.MigrationObjectModel
import com.quadient.migration.persistence.table.MigrationObjectTable
import com.quadient.migration.tools.getOrPutOrNull
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

abstract class InternalRepository<T : MigrationObjectModel>(
    val table: MigrationObjectTable, val projectName: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)!!
    val cache = mutableMapOf<String, T>()

    abstract fun toModel(row: ResultRow): T

    fun listAllModel(): List<T> {
        cache.clear()
        return transaction {
            table.selectAll().where(filter()).map {
                val result = toModel(it)
                cache.put(result.id, result)
                result
            }
        }
    }

    fun list(customFilter: Op<Boolean>): List<T> {
        return transaction {
            table.selectAll().where(customFilter and filter()).map {
                val result = toModel(it)
                cache.put(result.id, result)
                result
            }
        }
    }

    fun findModelOrFail(id: String): T {
        val model = findModel(id)
        return if (model != null) {
            model
        } else {
            val errorMessage = "Record '$id' not found in '${table::class.simpleName}'."
            logger.error(errorMessage)
            error(errorMessage)
        }
    }

    fun findModel(id: String): T? {
        return cache.getOrPutOrNull(id) {
            transaction {
                table.selectAll().where(filter(id)).firstOrNull()?.let(::toModel)
            }
        }
    }

    fun findModelByName(name: String): T? = transaction {
        table.selectAll().where(filter(name = name)).firstOrNull()?.let(::toModel)
    }

    fun delete(id: String) {
        cache.remove(id)
        transaction { table.deleteWhere { filter(id = id) } }
    }

    fun deleteAll(): Int {
        cache.clear()
        return transaction { table.deleteWhere { filter() } }
    }

    fun destroy() {
        transaction {
            exec("DROP TABLE ${table.tableName}")
        }
    }

    fun filter(id: String? = null, name: String? = null): Op<Boolean> {
        var result = table.projectName eq projectName
        if (id != null) {
            result = result and (table.id eq id)
        }
        if (name != null) {
            result = result and (table.name eq name)
        }

        return result
    }
}