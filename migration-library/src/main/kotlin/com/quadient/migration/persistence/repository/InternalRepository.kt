package com.quadient.migration.persistence.repository

import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.data.MigrationObjectModel
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
import org.slf4j.LoggerFactory

abstract class InternalRepository<T : MigrationObjectModel>(
    val table: MigrationObjectTable, val projectName: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)!!
    private val cache = mutableMapOf<String, T>()
    private var allCached = false

    abstract fun toModel(row: ResultRow): T

    fun count(): Long {
        return transaction {
            table.selectAll().where(filter()).count()
        }
    }

    fun upsert(block: JdbcTransaction.() -> ResultRow): T {
        return transaction {
            val result = toModel(block())
            cache[result.id] = result
            result
        }
    }

    fun <D: MigrationObject> upsertBatch(dtos: Collection<D>, block: (java.sql.Connection) -> Unit) {
        transaction {
            block(connection.connection as java.sql.Connection)
        }
        cacheObjects(dtos)
    }

    fun createSql(columns: List<String>, dtoCount: Int): String {
        val placeholders = (1..columns.size).joinToString(",", prefix = "(", postfix = ")") { "?" }
        val values = (1..dtoCount).joinToString(",") { placeholders }
        val setOnConflict = columns.filter { it != "created" }.joinToString(", ") { "$it = EXCLUDED.$it" }

        val sql = """
        INSERT INTO ${table.tableName} (${columns.joinToString(", ")})
        VALUES $values
        ON CONFLICT (id, project_name) DO UPDATE SET $setOnConflict
        """.trimIndent()

        return sql
    }

    fun <D: MigrationObject> cacheObjects(dto: Collection<D>) {
        val ids = dto.map { it.id }.toSet()
        transaction {
            for (obj in table.selectAll().where(table.projectName eq projectName and (table.id inList ids))
                .map(::toModel)) {
                cache[obj.id] = obj
            }
        }
    }

    fun listAllModelBatched(limit: Int, offset: Long): List<T> {
        return transaction {
            val result = table.selectAll().orderBy(table.id).limit(limit).offset(offset).where(filter()).map {
                val result = toModel(it)
                cache[result.id] = result
                result
            }
            result
        }
    }

    fun listAllModel(): List<T> {
        if (allCached) {
            return cache.values.toList()
        }
        return transaction {
            val result = table.selectAll().where(filter()).map {
                val result = toModel(it)
                cache[result.id] = result
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
                table.selectAll().where(filter(id)).firstOrNull()?.let(::toModel)
            }
        }
    }

    fun findModelByName(name: String): T? {
        val found = cache.values.find { it.name == name }
        return found ?: transaction {
            val result = table.selectAll().where(filter(name = name)).firstOrNull()?.let(::toModel)
            if (result != null) {
                cache[result.id] = result
            }

            result
        }
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
}