//! ---
//! category: Parser
//! description: Import docbook example
//! sourceFormat: DocBook
//! ---
package com.quadient.migration.example.docbook

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType
import groovy.transform.Field
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

import java.util.regex.Matcher
import java.util.regex.Pattern

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding)

List<File> xmlFiles = new File(migration.projectConfig.inputDataPath).listFiles().findAll {
    it.isFile() && it.name.endsWith('.xml')
}

println(xmlFiles.size())
imageId = 1

xmlFiles.each {
    println(it)
    parseTemplate(it)
}

def parseTemplate(File xmlFile) {
    List<DocumentObject> blocks = new ArrayList<DocumentObject>()

    println "Parsing XML file: ${xmlFile.name}"

    String templateId = xmlFile.name[0..xmlFile.name.lastIndexOf('.') - 1]
    String source = xmlFile.name
    XmlSlurper parser = new XmlSlurper()

    parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
    parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

    GPathResult xml = parser.parse(xmlFile)

    xml.children().each { item ->
        GPathResult nodeItem = (GPathResult) item
        if (nodeItem.name() == "sect1") {
            if (nodeItem.children().size() > 0) {
                ArrayList<Paragraph> paragraphs = new ArrayList<>()
                nodeItem.children().each { childItem ->
                    GPathResult childNodeItem = (GPathResult) childItem
                    if (childNodeItem.name() == "para") {
                        def paraBuilder = new ParagraphBuilder()
                        paraBuilder.addText().content(tokenizeTextAndVars(childNodeItem.text(), source))
                        paragraphs.add(paraBuilder.build())
                    }
                }
                String id = nodeItem.@id
                blocks.add(upsertBlock(id, paragraphs, null, [source]))
            }
        }
        if (nodeItem.name() == "para" || nodeItem.name() == "title") {
            if (nodeItem.children().size() > 0 && nodeItem.inlinegraphic.@fileref) {
                String id = "image_" + imageId.toString()
                imageId++
                Map<String, String> customFields = ["image": nodeItem.inlinegraphic.@fileref.toString()]
                blocks.add(upsertBlock(id, [], customFields, [source], DocumentObjectType.Unsupported))
            } else {
                String id = "emptyLine"
                blocks.add(upsertBlock(id, [new Paragraph("")], null, [source]))
            }
        }
    }
    upsertTemplate(templateId, blocks.collect { it.id }, [:], [source])
}


DocumentObject upsertBlock(String id,
                           List<DocumentContent> paragraphs,
                           Map<String, String> customFields,
                           List<String> originLocations,
                           DocumentObjectType type = DocumentObjectType.Block) {
    def blockBuilder = new DocumentObjectBuilder(id, type)
            .content(paragraphs)
            .originLocations(originLocations)
            .internal(true)

    if (customFields != null) {
        blockBuilder.customFields(customFields)
    }

    def block = blockBuilder.build()

    migration.documentObjectRepository.upsert(block)
    return block
}

DocumentObject upsertTemplate(String id,
                                 List<String> blocks,
                                 Map<String, String> customFields,
                                 List<String> originLocations) {
    def templateBuilder = new DocumentObjectBuilder(id, DocumentObjectType.Template)
            .content(blocks.collect { new DocumentObjectRef(it) })
            .originLocations(originLocations)

    if (customFields != null) {
        templateBuilder.customFields(customFields)
    }

    def template = templateBuilder.build()

    migration.documentObjectRepository.upsert(template)
    return template
}

List<TextContent> tokenizeTextAndVars(String inText, String source) {
    def content = new ArrayList<TextContent>()
    String regex = /\$(\w+)\$/

    Pattern pattern = Pattern.compile(regex)
    Matcher matcher = pattern.matcher(inText)
    int readingIndex = 0
    //Search for next match
    while (matcher.find()) {
        String matchingText = matcher.group(0)
        String variableId = matcher.group(1)
        //Find position of the found string in the original text
        int indexFound = inText.indexOf(matchingText, readingIndex)
        //Save to new string leading text + the found string
        String textBeforeFound = inText.substring(readingIndex, indexFound)
        if (textBeforeFound) {
            content.add(new StringValue(textBeforeFound))
        }
        if (!variableId.isEmpty()) {
            Variable variable = new VariableBuilder(variableId).dataType(DataType.String).originLocations([source]).build()
            content.add(new VariableRef(variableId))
            migration.variableRepository.upsert(variable)
        }
        //Mark the start position of the search for next match
        readingIndex = indexFound + matchingText.size()
    }
    //Save any trailing text after last found
    String trailingText = inText.substring(readingIndex)
    if (trailingText) {
        content.add(new StringValue(trailingText))
    }
    return content
}
