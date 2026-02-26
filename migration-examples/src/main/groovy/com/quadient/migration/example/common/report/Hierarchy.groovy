//! ---
//! displayName: Hierarchy
//! category: Report
//! description: Generate input data json for hierarchy visualization.
//! ---
package com.quadient.migration.example.common.report

import com.fasterxml.jackson.databind.ObjectMapper
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.*
import com.quadient.migration.example.common.util.PathUtil
import groovy.transform.EqualsAndHashCode
import groovy.transform.Field

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding)

def dstFile = PathUtil.dataDirPath(binding, "hierarchy", "${migration.projectConfig.name}-hierarchy.json").toFile()
dstFile.parentFile.mkdirs()

def documentObjects = migration.documentObjectRepository.listAll()
def displayRules = migration.displayRuleRepository.listAll()
def textStyles = migration.textStyleRepository.listAll()
def paragraphStyles = migration.paragraphStyleRepository.listAll()
def attachments = migration.attachmentRepository.listAll()
def images = migration.imageRepository.listAll()
def variables = migration.variableRepository.listAll()
def variableStructures = migration.variableStructureRepository.listAll()

def root = new Root()

for (def docObj : documentObjects) {
    def node = new Child(id: docObj.id,
        name: docObj.name,
        type: ChildType.DOCUMENT_OBJECT,
        data: [type: docObj.type.toString()])
    def children = docObj.collectRefs()
    collectChildren(node, children)

    root.documentObjects[node.id] = node
}

for (displayRule in displayRules) {
    def node = new Child(id: displayRule.id, name: displayRule.name, type: ChildType.DISPLAY_RULE)
    def children = displayRule.collectRefs()
    collectChildren(node, children)

    root.displayRules[node.id] = node
}

for (textStyle in textStyles) {
    def node = new Leaf(id: textStyle.id, name: textStyle.name, type: ChildType.TEXT_STYLE)

    root.textStyles[node.id] = node
}

for (paragraphStyle in paragraphStyles) {
    def node = new Leaf(id: paragraphStyle.id, name: paragraphStyle.name, type: ChildType.PARAGRAPH_STYLE)

    root.paragraphStyles[node.id] = node
}

for (attachment in attachments) {
    def node = new Leaf(id: attachment.id, name: attachment.name, type: ChildType.ATTACHMENT)

    root.attachments[node.id] = node
}

for (image in images) {
    def node = new Leaf(id: image.id, name: image.name, type: ChildType.IMAGE)

    root.images[node.id] = node
}

for (variable in variables) {
    def node = new Leaf(id: variable.id, name: variable.name, type: ChildType.VARIABLE)

    root.variables[node.id] = node
}

for (variableStructure in variableStructures) {
    def node = new Child(id: variableStructure.id, name: variableStructure.name, type: ChildType.VARIABLE_STRUCTURE)
    def children = variableStructure.collectRefs()
    collectChildren(node, children)

    root.variableStructures[node.id] = node
}

// Collect parents
for (node in root.documentObjects.values()) {
    def parentNode = new Reference(id: node.id, type: ChildType.DOCUMENT_OBJECT)
    collectParents(root, node, parentNode)
}

for (node in root.displayRules.values()) {
    def parentNode = new Reference(id: node.id, type: ChildType.DISPLAY_RULE)
    collectParents(root, node, parentNode)
}

for (node in root.variableStructures.values()) {
    def parentNode = new Reference(id: node.id, type: ChildType.VARIABLE_STRUCTURE)
    collectParents(root, node, parentNode)
}

for (node in root.variableStructures.values()) {
    def parentNode = new Reference(id: node.id, type: ChildType.VARIABLE_STRUCTURE)
    collectParents(root, node, parentNode)
}

new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(dstFile, root)

static void collectParents(Root root, Node node, Reference parentNode) {
    for (child in node.children) {
        def c = switch (child.type) {
            case ChildType.DOCUMENT_OBJECT -> root.documentObjects
            case ChildType.DISPLAY_RULE -> root.displayRules
            case ChildType.TEXT_STYLE -> root.textStyles
            case ChildType.PARAGRAPH_STYLE -> root.paragraphStyles
            case ChildType.ATTACHMENT -> root.attachments
            case ChildType.IMAGE -> root.images
            case ChildType.VARIABLE -> root.variables
            case ChildType.VARIABLE_STRUCTURE -> root.variableStructures
            default -> throw new IllegalStateException("Unknown parent type: ${child.type}")
        }

        c[child.id].parents.add(parentNode)
    }
}

static void collectChildren(Node node, List<Ref> refs) {
    for (def child : refs) {
        def type = switch (child) {
            case DocumentObjectRef -> ChildType.DOCUMENT_OBJECT
            case DisplayRuleRef -> ChildType.DISPLAY_RULE
            case TextStyleRef -> ChildType.TEXT_STYLE
            case ParagraphStyleRef -> ChildType.PARAGRAPH_STYLE
            case AttachmentRef -> ChildType.ATTACHMENT
            case ImageRef -> ChildType.IMAGE
            case VariableRef -> ChildType.VARIABLE
            case VariableStructureRef -> ChildType.VARIABLE_STRUCTURE
            default -> throw new IllegalStateException("Unknown reference type: ${child.class}")
        }

        node.children << new Reference(id: child.id, type: type)
    }
}

class Root {
    Map<String, Child> documentObjects = [:]
    Map<String, Child> displayRules = [:]
    Map<String, Leaf> textStyles = [:]
    Map<String, Leaf> paragraphStyles = [:]
    Map<String, Leaf> attachments = [:]
    Map<String, Leaf> images = [:]
    Map<String, Leaf> variables = [:]
    Map<String, Child> variableStructures = [:]
}

class Node {
    String id
    String name
    ChildType type
    Set<Reference> parents = []
    Map<String, String> data = [:]
}

class Leaf extends Node {}

class Child extends Node {
    Set<Reference> children = []
}

@EqualsAndHashCode
class Reference {
    String id
    ChildType type
}

enum ChildType {
    DOCUMENT_OBJECT,
    DISPLAY_RULE,
    TEXT_STYLE,
    PARAGRAPH_STYLE,
    ATTACHMENT,
    IMAGE,
    VARIABLE,
    VARIABLE_STRUCTURE,
}