//! ---
//! displayName: Migration Model Example
//! category: Parser
//! description: Import Migration objects of technical example
//! sourceFormat: Migration model example
//! ---
package com.quadient.migration.example.example

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.Paragraph
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.builder.DisplayRuleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.GroupOp
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.PageOptions
import com.quadient.migration.shared.Size

import java.time.Instant

import static com.quadient.migration.example.common.util.InitMigration.initMigration
import static com.quadient.migration.api.dto.migrationmodel.builder.Dsl.table

Migration migration = initMigration(this.binding)

def pageWidth = Size.ofMillimeters(148)
def pageHeight = Size.ofMillimeters(210)
def leftMargin = Size.ofCentimeters(1)
def topMargin = Size.ofCentimeters(1)
def contentWidth = Size.ofMillimeters(128)
def logoWidth = Size.ofMillimeters(30)
def logoHeight = Size.ofMillimeters(13)

// Define variables to be used in the address and table
def displayHeaderVariable = new VariableBuilder("displayHeaderVariable")
    .defaultValue("true")
    .dataType(DataType.Boolean).build()
def displayParagraphVariable = new VariableBuilder("displayParagraphVariable")
    .defaultValue("true")
    .dataType(DataType.Boolean).build()
def displayLastSentenceVariable = new VariableBuilder("displayLastSentenceVariable")
    .defaultValue("true")
    .dataType(DataType.Boolean).build()
def nameVariable = new VariableBuilder("nameVariable")
    .defaultValue("John Doe")
    .dataType(DataType.String).build()
def addressVariable = new VariableBuilder("addressVariable")
    .defaultValue("123 Main St")
    .dataType(DataType.String).build()
def cityVariable = new VariableBuilder("cityVariable")
    .defaultValue("Anytown")
    .dataType(DataType.String).build()
def stateVariable = new VariableBuilder("stateVariable")
    .defaultValue("Canada")
    .dataType(DataType.String).build()

// Display displayHeaderRule to conditionally display the address.
// Header is hidden if displayHeaderVariable is set to false
def displayAddressRule = new DisplayRuleBuilder("displayAddressRule")
    .group {
        it.operator(GroupOp.Or)
        it.comparison { it.variable(nameVariable.id).notEquals().value("") }
        it.comparison { it.variable(addressVariable.id).notEquals().value("") }
        it.comparison { it.variable(cityVariable.id).notEquals().value("") }
        it.comparison { it.variable(stateVariable.id).notEquals().value("") }
    }.build()

def displayHeaderRule = new DisplayRuleBuilder("displayHeaderRule")
    .comparison { it.value(true).equals().variable(displayHeaderVariable.id) }
    .build()

def displayParagraphRule = new DisplayRuleBuilder("displayParagraphRule")
    .comparison { it.value(true).equals().variable(displayParagraphVariable.id) }
    .build()

def displayLastSentenceRule = new DisplayRuleBuilder("displayLastSentenceRule")
    .comparison { it.value(true).equals().variable(displayLastSentenceVariable.id) }
    .build()

def displayRuleStateCzechia = new DisplayRuleBuilder("displayRuleStateCzechia")
    .comparison { it.value("Czechia").equals().variable(stateVariable.id) }
    .build()

def displayRuleStateFrance = new DisplayRuleBuilder("displayRuleStateFrance")
    .comparison { it.value("France").equals().variable(stateVariable.id) }
    .build()

// Define text and paragraph styles to be used in the document
def normalStyle = new TextStyleBuilder("normalStyle")
    .definition {
        it.size(Size.ofPoints(10))
        it.foregroundColor("#000000")
    }
    .build()

def headingStyle = new TextStyleBuilder("headingStyle")
    .definition {
        it.size(Size.ofPoints(12))
        it.bold(true)
    }
    .build()

def paragraphStyle = new ParagraphStyleBuilder("paragraphStyle")
    .definition {
        it.firstLineIndent(Size.ofMillimeters(10))
        it.spaceAfter(Size.ofMillimeters(5))
    }
    .build()

// Define image to be used as a logo, base64 encoded image is hardcoded
// here for simplicity but any valid image that is saved to the storage
// can be used.
def logoBase64 = "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"
def logoImageName = "logo.png"
migration.storage.write(logoImageName, logoBase64.decodeBase64())
def logo = new ImageBuilder("logo")
    .options(new ImageOptions(logoWidth, logoHeight))
    .sourcePath(logoImageName)
    .imageType(ImageType.Png)
    .build()

// Table containing some data with the first address row being optionally hidden
// by using displayRuleRef to the display displayHeaderRule defined above.
// The table also contains some merged cells and custom column widths.
def table = table {
    it.addColumnWidth(Size.ofMillimeters(10), 10)
    it.addColumnWidth(Size.ofMillimeters(20), 20)
    it.addColumnWidth(Size.ofMillimeters(98), 70)
    it.row {
        it.displayRuleRef = new DisplayRuleRef(displayHeaderRule.id)
        it.cell {
            it.appendContent(new Paragraph("ID"))
        }
        it.cell {
            it.appendContent(new Paragraph("key"))
        }
        it.cell {
            it.appendContent(new Paragraph("value"))
        }
    }

    it.row {
        it.cell {
            it.appendContent(new Paragraph("1"))
        }
        it.cell {
            // This cell is merged with the cell to the left on the same row
            // and contains the value of the left cell.
            it.mergeLeft = true
            it.appendContent(new Paragraph("key1"))
        }
        it.cell {
            it.appendContent(new Paragraph("value1"))
        }
    }

    it.row {
        it.cell {
            it.appendContent(new Paragraph("2"))
        }
        it.cell {
            it.appendContent(new Paragraph("key2"))
        }
        it.cell {
            it.appendContent(new Paragraph("value2"))
        }
    }

    it.row {
        it.cell {
            it.appendContent(new Paragraph("3"))
        }
        it.cell {
            it.appendContent(new Paragraph("key3"))
        }
        it.cell {
            it.appendContent(new Paragraph("value3"))
        }
    }
}

// Header of the document containing the recipient's information.
// It uses the variables defined above to dynamically insert the
// recipient's name, address, city, and state.
// Simple paragraph is used because no styling is needed.
def address = new DocumentObjectBuilder("address", DocumentObjectType.Block)
    .paragraph { it.variableRef(nameVariable.id) }
    .paragraph { it.variableRef(addressVariable.id) }
    .paragraph { it.variableRef(cityVariable.id) }
    .paragraph { it.variableRef(stateVariable.id) }
    .build()

// Footer of the document containing a signature.
def signature = new DocumentObjectBuilder("signature", DocumentObjectType.Block)
    .paragraph { it.content("Sincerely,") }
    .paragraph { it.content("John Smith") }
    .paragraph { it.content("CEO of Lorem ipsum") }
    .build()

// Sample paragraph containing a heading using headingStyle style,
// and body text with normalStyle, both defined above.
def paragraph1 = new DocumentObjectBuilder("paragraph1", DocumentObjectType.Block)
// No separate file will be created and the content will be inlined instead when block is internal.
    .internal(true)
    .paragraph {
        it.text {
            it.styleRef(headingStyle.id)
            it.content("Lorem ipsum dolor sit amet\n")
        }
    }
    .paragraph {
        it.styleRef(paragraphStyle.id)
            .text {
                it.styleRef(normalStyle.id)
                it.firstMatch {
                    it.case {
                        it.appendContent(new ParagraphBuilder().styleRef(paragraphStyle.id).text {
                            it.appendContent("Dobrý den")
                        }.build()).displayRule(displayRuleStateCzechia.id)
                    }.case {
                        it.appendContent(new ParagraphBuilder().styleRef(paragraphStyle.id).text {
                            it.appendContent("Bonjour")
                        }.build()).displayRule(displayRuleStateFrance.id)
                    }.default(new ParagraphBuilder().styleRef(paragraphStyle.id).text { it.appendContent("Good morning") }.build())
                }
                it.appendContent(", Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi vel diam ut dui vulputate lobortis ac sit amet diam. Donec malesuada eros id vulputate tincidunt. Aenean ac placerat nisi. Morbi porta orci at est interdum, mollis sollicitudin odio pulvinar. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Morbi sem mauris, porta sed erat vel, vestibulum facilisis dui. Maecenas sodales quam neque, ut consectetur ante interdum at.")
            }
    }
    .build()

// Second sample paragraph
def paragraph2 = new DocumentObjectBuilder("paragraph2", DocumentObjectType.Block)
    .internal(true)
    .paragraph {
        it.styleRef(new ParagraphStyleRef(paragraphStyle.id))
            .text {
                it.content("Donec non porttitor ipsum. Praesent et blandit nulla, quis ullamcorper enim. Curabitur nec rutrum justo. Nunc ac quam a ante consequat ullamcorper eget sit amet tortor. Donec convallis sagittis purus, a feugiat lacus tristique vitae. In a orci risus. Sed elit magna, vestibulum vitae orci sodales, consequat pharetra nisi. Vestibulum non scelerisque elit. Duis feugiat porttitor ante sit amet porta. Fusce at leo posuere, venenatis libero ut, varius dolor. Duis bibendum porta tincidunt.")
            }
            .text {
                it.displayRuleRef(new DisplayRuleRef(displayLastSentenceRule.id))
                it.content("Nulla id nulla odio.")
            }
    }
    .build()

// Paragraph that is displayed conditionally based on the value of the displayParagraphVariable.
// This also demonstrates metadata usage - various metadata types can be attached
// to document objects to provide additional information without affecting visible content.
def conditionalParagraph = new DocumentObjectBuilder("conditionalParagraph", DocumentObjectType.Block)
    .internal(false)
    .displayRuleRef(displayParagraphRule.id)
    .metadata("DocumentInfo") { mb ->
        mb.string("Document type: Technical Example")
        mb.string("Version: 1.0")
    }
    .metadata("Timestamps") { mb ->
        mb.dateTime(Instant.parse("2024-01-15T10:30:00Z"))
    }
    .metadata("Statistics") { mb ->
        mb.integer(42L)
        mb.float(98.5)
        mb.boolean(true)
    }
    .paragraph {
        it.styleRef(new ParagraphStyleRef(paragraphStyle.id))
            .text {
                it.content("Integer quis quam semper, accumsan neque at, pellentesque diam. Etiam in blandit dolor. Maecenas sit amet interdum augue, vel pellentesque erat. Suspendisse ut sem in justo rhoncus placerat vitae ut lacus. Etiam consequat bibendum justo ut posuere. Donec aliquam posuere nibh, vehicula pulvinar lectus dictum et. Nullam rhoncus ultrices ipsum et consectetur. Nam tincidunt id purus ac viverra. ")
            }
    }
    .build()

def firstMatchBlock = new DocumentObjectBuilder("firstMatch", DocumentObjectType.Block)
    .internal(true)
    .firstMatch { fb ->
        fb.case { cb ->
            cb.name("Czech Variant").appendContent(new ParagraphBuilder().styleRef(paragraphStyle.id).text {
                it.appendContent("Nashledanou.")
            }.build()).displayRule(displayRuleStateCzechia.id)
        }.case { cb ->
            cb.name("French Variant").appendContent(new ParagraphBuilder().styleRef(paragraphStyle.id).text {
                it.appendContent("Au revoir.")
            }.build()).displayRule(displayRuleStateFrance.id)
        }.default(new ParagraphBuilder().styleRef(paragraphStyle.id).text { it.appendContent("Goodbye.") }.build())
    }.build()

// SelectByLanguage demonstrates language-based content selection.
def selectByLanguageBlock = new DocumentObjectBuilder("selectByLanguage", DocumentObjectType.Block)
    .internal(true)
    .selectByLanguage { sb ->
        sb.case { cb ->
            cb.language("en_us")
            cb.appendContent(new ParagraphBuilder().styleRef(paragraphStyle.id).text {
                it.appendContent("This document was created in English.")
            }.build())
        }.case { cb ->
            cb.language("de")
            cb.appendContent(new ParagraphBuilder().styleRef(paragraphStyle.id).text {
                it.appendContent("Dieses Dokument wurde auf Deutsch erstellt.")
            }.build())
        }.case { cb ->
            cb.language("es")
            cb.appendContent(new ParagraphBuilder().styleRef(paragraphStyle.id).text {
                it.appendContent("Este documento fue creado en español.")
            }.build())
        }
    }.build()

// A page object which contains the address, paragraphs, table, and signature.
// All the content is absolutely positioned using FlowAreas
def paragraph1TopMargin = topMargin + Size.ofCentimeters(2)
def signatureTopMargin = pageHeight - Size.ofCentimeters(3)
def page = new DocumentObjectBuilder("page1", DocumentObjectType.Page)
    .options(new PageOptions(pageWidth, pageHeight))
    .area {
        it.position {
            it.left(leftMargin)
            it.top(topMargin)
            it.width(contentWidth)
            it.height(Size.ofCentimeters(2))
        }
            .documentObjectRef(address.id, displayAddressRule.id)
            .interactiveFlowName("Def.InteractiveFlow0")
    }
    .area {
        it.position {
            it.left(leftMargin + contentWidth - logoWidth)
            it.top(topMargin)
            it.width(logoWidth)
            it.height(logoHeight)
        }.imageRef(logo.id)
    }
    .area {
        it.position {
            it.left(leftMargin)
            it.top(paragraph1TopMargin)
            it.width(contentWidth)
            it.height(pageHeight - Size.ofCentimeters(4))
        }
            .documentObjectRef(paragraph1.id)
            .documentObjectRef(paragraph2.id)
            .paragraph { it.styleRef(paragraphStyle.id).text { it.content(table) } }
            .documentObjectRef(conditionalParagraph.id)
            .documentObjectRef(firstMatchBlock.id)
            .documentObjectRef(selectByLanguageBlock.id)
    }
    .area {
        it.position {
            it.left(leftMargin)
            it.top(signatureTopMargin)
            it.width(contentWidth)
            it.height(Size.ofCentimeters(2))
        }
            .documentObjectRef(signature.id)
    }
    .build()

def template = new DocumentObjectBuilder("template", DocumentObjectType.Template)
    .documentObjectRef(page.id)
    .build()

// Insert all content into the database to be used in the deploy task
for (item in [address, signature, paragraph1, paragraph2, conditionalParagraph, page, template, firstMatchBlock, selectByLanguageBlock]) {
    migration.documentObjectRepository.upsert(item)
}
for (item in [headingStyle, normalStyle]) {
    migration.textStyleRepository.upsert(item)
}
for (item in [displayHeaderVariable, displayParagraphVariable, displayLastSentenceVariable, nameVariable, addressVariable, cityVariable, stateVariable]) {
    migration.variableRepository.upsert(item)
}
for (item in [displayAddressRule, displayHeaderRule, displayParagraphRule, displayLastSentenceRule, displayRuleStateCzechia, displayRuleStateFrance]) {
    migration.displayRuleRepository.upsert(item)
}
for (item in [paragraphStyle]) {
    migration.paragraphStyleRepository.upsert(item)
}

migration.imageRepository.upsert(logo)
migration.variableStructureRepository.upsert(new VariableStructureBuilder("variableStructure")
    .addVariable(displayHeaderVariable.id, "Data.displayHeader")
    .addVariable(displayParagraphVariable.id, "Data.displayParagraph")
    .addVariable(displayLastSentenceVariable.id, "Data.displayLastSentence")
    .addVariable(nameVariable.id, "Data.name")
    .addVariable(addressVariable.id, "Data.address")
    .addVariable(cityVariable.id, "Data.city")
    .addVariable(stateVariable.id, "Data.state")
    .build())