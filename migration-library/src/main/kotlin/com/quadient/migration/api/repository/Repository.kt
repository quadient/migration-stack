package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.api.dto.migrationmodel.RefValidatable
import com.quadient.migration.persistence.table.MigrationObjectTable
import com.quadient.migration.tools.getOrPutOrNull
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.statements.BatchUpsertStatement
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

abstract class Repository<T : MigrationObject>(
    protected val table: MigrationObjectTable,
    protected val projectName: String
) {
    private val cache = mutableMapOf<String, T>()
    private var allCached = false

    abstract fun fromDb(row: ResultRow): T

    fun listAll(): List<T> {
        if (allCached) {
            return cache.values.toList()
        }
        return transaction {
            val result = table.selectAll().where(filter()).map {
                val result = fromDb(it)
                cache[result.id] = result
                result
            }
            allCached = true
            result
        }
    }

    open fun list(customFilter: Op<Boolean>): List<T> {
        return transaction {
            table.selectAll().where(customFilter and filter()).map {
                val result = fromDb(it)
                cache[result.id] = result
                result
            }
        }
    }

    fun find(id: String): T? {
        return cache.getOrPutOrNull(id) {
            transaction {
                table.selectAll().where(filter(id)).firstOrNull()?.let(::fromDb)
            }
        }
    }

    fun findOrFail(id: String): T {
        val model = find(id)
        return if (model != null) {
            model
        } else {
            val errorMessage = "Record '$id' not found in '${table::class.simpleName}'."
            error(errorMessage)
        }
    }

    fun findByName(name: String): T? {
        val found = cache.values.find { it.name == name }
        return found ?: transaction {
            val result = table.selectAll().where(filter(name = name)).firstOrNull()?.let(::fromDb)
            if (result != null) {
                cache[result.id] = result
            }
            result
        }
    }

    fun findAndUpdate(id: String, block: (T) -> Unit): T? {
        return find(id)?.let {
            block(it)
            upsert(it)
            it
        }
    }

    fun findRefs(id: String): List<Ref> {
        val model = find(id)
        if (model != null && model is RefValidatable) {
            return model.collectRefs()
        }
        return emptyList()
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

    protected fun filter(id: String? = null, name: String? = null): Op<Boolean> {
        var result = table.projectName eq projectName
        if (id != null) {
            result = result and (table.id eq id)
        }
        if (name != null) {
            result = result and (table.name eq name)
        }
        return result
    }

    protected fun upsertInternal(block: JdbcTransaction.() -> ResultRow): T {
        return transaction {
            val result = fromDb(block())
            cache[result.id] = result
            result
        }
    }

    protected fun <D : MigrationObject> upsertBatchInternal(
        dto: Collection<D>,
        block: BatchUpsertStatement.(D) -> Unit
    ) {
        val ids = dto.map { it.id }.toSet()
        return transaction {
            table.batchUpsert(dto) {
                block(it)
            }

            for (obj in table.selectAll().where(table.projectName eq projectName and (table.id inList ids))
                .map(::fromDb)) {
                cache[obj.id] = obj
            }
        }
    }

    protected fun updateCache(id: String, obj: T) {
        cache[id] = obj
    }

    abstract fun findUsages(id: String): List<MigrationObject>

    abstract fun upsert(dto: T)

    abstract fun upsertBatch(dtos: Collection<T>)
}