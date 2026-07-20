package com.quadient.migration.service

import com.quadient.migration.api.dto.migrationmodel.AttachmentRef
import com.quadient.migration.api.dto.migrationmodel.BaseTemplateRef
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.MigrationObject
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.api.dto.migrationmodel.RefValidatable
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.api.dto.migrationmodel.VariableStructureRef
import com.quadient.migration.api.repository.AttachmentRepository
import com.quadient.migration.api.repository.BaseTemplateRepository
import com.quadient.migration.api.repository.DisplayRuleRepository
import com.quadient.migration.api.repository.DocumentObjectRepository
import com.quadient.migration.api.repository.ImageRepository
import com.quadient.migration.api.repository.ParagraphStyleRepository
import com.quadient.migration.api.repository.TextStyleRepository
import com.quadient.migration.api.repository.VariableRepository
import com.quadient.migration.api.repository.VariableStructureRepository

class RefCollector(
    private val documentObjectRepository: DocumentObjectRepository,
    private val imageRepository: ImageRepository,
    private val attachmentRepository: AttachmentRepository,
    private val textStyleRepository: TextStyleRepository,
    private val paragraphStyleRepository: ParagraphStyleRepository,
    private val displayRuleRepository: DisplayRuleRepository,
    private val variableRepository: VariableRepository,
    private val variableStructureRepository: VariableStructureRepository,
    private val baseTemplateRepository: BaseTemplateRepository,
) {
    fun <T: RefValidatable> collectAllRefs(obj: T, breakFn: (MigrationObject) -> Boolean = { true }): Set<Ref> {
        return obj.collectAllRefs(breakFn)
    }

    fun <T: RefValidatable> collectAllObjects(obj: T, breakFn: (MigrationObject) -> Boolean = { true }): Set<MigrationObject> {
        return obj.collectAllObjects(breakFn)
    }

    @JvmName("#collectAllObjectsExt")
    fun <T : RefValidatable> T.collectAllObjects(breakFn: (MigrationObject) -> Boolean = { true }): Set<MigrationObject> {
        val result = mutableSetOf<MigrationObject>()
        traverse(breakFn) { _, migObject -> result.add(migObject) }
        return result
    }

    @JvmName("#collectAllRefsExt")
    fun <T : RefValidatable> T.collectAllRefs(breakFn: (MigrationObject) -> Boolean = { true }): Set<Ref> {
        val result = mutableSetOf<Ref>()
        traverse(breakFn) { ref, _ -> result.add(ref) }
        return result
    }

    private fun resolve(ref: Ref): MigrationObject = when (ref) {
        is DisplayRuleRef -> displayRuleRepository.findOrFail(ref.id)
        is DocumentObjectRef -> documentObjectRepository.findOrFail(ref.id)
        is ParagraphStyleRef -> paragraphStyleRepository.findOrFail(ref.id)
        is AttachmentRef -> attachmentRepository.findOrFail(ref.id)
        is ImageRef -> imageRepository.findOrFail(ref.id)
        is TextStyleRef -> textStyleRepository.findOrFail(ref.id)
        is VariableRef -> variableRepository.findOrFail(ref.id)
        is VariableStructureRef -> variableStructureRepository.findOrFail(ref.id)
        is BaseTemplateRef -> baseTemplateRepository.findOrFail(ref.id)
    }

    private fun <T : RefValidatable> T.traverse(
        breakFn: (MigrationObject) -> Boolean,
        onVisit: (Ref, MigrationObject) -> Boolean,
    ) {
        val queue = this.collectRefs().toMutableList()
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val migObject = resolve(current)
            if (!onVisit(current, migObject)) continue
            if (!breakFn(migObject)) continue
            if (migObject is RefValidatable) queue.addAll(migObject.collectRefs())
        }
    }
}