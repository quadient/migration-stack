package com.quadient.migration.api.repository

import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.data.MigrationObjectModel
import com.quadient.migration.persistence.repository.InternalRepository
import com.quadient.migration.service.RefValidatable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

abstract class Repository<T : MigrationObject, K : MigrationObjectModel>(
    protected val internalRepository: InternalRepository<K>
) {
    abstract fun toDto(model: K): T

    fun count(): Long = internalRepository.count()
    fun listAll(): List<T> = internalRepository.listAllModel().map(::toDto)
    fun listAllBatched(limit: Int, offset: Long): List<T> = internalRepository.listAllModelBatched(limit, offset).map(::toDto)
    fun find(id: String): T? = internalRepository.findModel(id)?.let(::toDto)
    fun findByName(name: String): T? = internalRepository.findModelByName(name)?.let(::toDto)

    fun findAndUpdate(id: String, block: (T) -> Unit): T? {
        return internalRepository.findModel(id)?.let {
            val dto = toDto(it)
            block(dto)
            upsert(dto)
            dto
        }
    }

    fun findRefs(id: String): List<Ref> {
        val model = internalRepository.findModel(id)
        if (model != null && model is RefValidatable) {
            return model.collectRefs().map { Ref.fromModel(it) }
        }
        return emptyList()
    }

    fun delete(id: String) = internalRepository.delete(id)

    fun deleteAll() = internalRepository.deleteAll()

    fun destroy() = internalRepository.destroy()

    abstract fun findUsages(id: String): List<MigrationObject>

    abstract fun upsert(dto: T)

    abstract fun upsertBatch(dtos: Collection<T>)
}