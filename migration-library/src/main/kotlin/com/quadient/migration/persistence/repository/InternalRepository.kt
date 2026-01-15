package com.quadient.migration.persistence.repository

import com.quadient.migration.data.MigrationObjectModel
import com.quadient.migration.persistence.table.MigrationObjectTable
import com.quadient.migration.tools.getOrPutOrNull
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

abstract class InternalRepository<T : MigrationObjectModel>(
    val table: MigrationObjectTable, val projectName: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)!!
    private val cache = mutableMapOf<String, T>()
    private var allCached = false

    abstract fun toModel(row: ResultRow): T

    fun upsert(id: String, block: JdbcTransaction.() -> Unit) {
        transaction {
            block()
            when (val model = findModelInternal(id)) {
                null -> cache.remove(id)
                else -> cache.put(id, model)
            }
        }
    }

    fun listAllModel(): List<T> {
        if (allCached) {
            return cache.values.toList()
        }
        return transaction {
            val result = table.selectAll().where(filter()).map {
                val result = toModel(it)
                cache.put(result.id, result)
                result
            }
            allCached = true
            result
        }
    }

    fun list(customFilter: Op<Boolean>): List<T> {
        return transaction {
            table.selectAll().where(customFilter and filter()).map {
                val result = toModel(it)
                cache[result.id] = result
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
                findModelInternal(id)
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
        cache.clear()
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

    private fun findModelInternal(id: String): T? {
        return table.selectAll().where(filter(id)).firstOrNull()?.let(::toModel)
    }

}