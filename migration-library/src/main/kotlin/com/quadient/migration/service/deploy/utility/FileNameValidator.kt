package com.quadient.migration.service.deploy.utility

interface FileNameValidator {
    val forbiddenChars: Set<Char>

    fun validate(fileName: String): Set<Char> {
        if (forbiddenChars.isEmpty()) return emptySet()

        return fileName.filter { it in forbiddenChars }.toSet()
    }
}

open class InteractiveFileNameValidator : FileNameValidator {
    override val forbiddenChars = setOf('/', ',', ':', '\\', '?', '%', ';', '*', '|', '"', '<', '>')
}

class EvolveFileNameValidator : InteractiveFileNameValidator()

class DesignerFileNameValidator : FileNameValidator {
    override val forbiddenChars = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*',)
}
