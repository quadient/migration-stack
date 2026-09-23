package com.quadient.migration.service.deploy.utility

import com.quadient.migration.tools.shouldBeEmpty
import com.quadient.migration.tools.shouldBeEqualTo
import org.junit.jupiter.api.Test

class FileNameValidatorTest {
    private val interactive = InteractiveFileNameValidator()
    private val designer = DesignerFileNameValidator()

    @Test
    fun `interactive file name without forbidden characters is valid`() {
        interactive.validate("block.jsd").shouldBeEmpty()
    }

    @Test
    fun `forbidden character in the file name is reported`() {
        interactive.validate("na%me.jld").shouldBeEqualTo(setOf('%'))
    }

    @Test
    fun `dots in the file name are allowed`() {
        interactive.validate("my.block.jsd").shouldBeEmpty()
    }

    @Test
    fun `all forbidden characters in the file name are reported`() {
        interactive.validate("a,b:c?d%e;f*g|h\"i<j>k.jsd")
            .shouldBeEqualTo(setOf(',', ':', '?', '%', ';', '*', '|', '"', '<', '>'))
    }

    @Test
    fun `path separators in the file name are reported`() {
        interactive.validate("folder/sub\\block.jld").shouldBeEqualTo(setOf('/', '\\'))
    }

    @Test
    fun `designer file name without forbidden characters is valid`() {
        designer.validate("my.template.wfd").shouldBeEmpty()
    }

    @Test
    fun `designer reports all its forbidden characters`() {
        designer.validate("a<b>c:d\"e|f?g*h.wfd").shouldBeEqualTo(setOf('<', '>', ':', '"', '|', '?', '*'))
    }

    @Test
    fun `designer reports path separators in the file name`() {
        designer.validate("folder/sub\\template.wfd").shouldBeEqualTo(setOf('/', '\\'))
    }

    @Test
    fun `designer allows characters forbidden only in interactive`() {
        designer.validate("a,b%c;d.wfd").shouldBeEmpty()
    }

    @Test
    fun `evolve uses the same forbidden characters as interactive`() {
        EvolveFileNameValidator().forbiddenChars.shouldBeEqualTo(interactive.forbiddenChars)
    }
}
