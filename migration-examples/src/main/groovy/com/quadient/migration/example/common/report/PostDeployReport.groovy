package com.quadient.migration.example.common.report

import com.fasterxml.jackson.databind.ObjectMapper
import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.DisplayRuleRef
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.api.dto.migrationmodel.ImageRef
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyle
import com.quadient.migration.api.dto.migrationmodel.ParagraphStyleRef
import com.quadient.migration.api.dto.migrationmodel.Ref
import com.quadient.migration.api.dto.migrationmodel.StatusTracking
import com.quadient.migration.api.dto.migrationmodel.TextStyle
import com.quadient.migration.api.dto.migrationmodel.TextStyleRef
import com.quadient.migration.api.dto.migrationmodel.VariableRef
import com.quadient.migration.data.Active
import com.quadient.migration.data.Deployed
import com.quadient.migration.data.StatusEvent
import com.quadient.migration.example.common.util.Csv
import com.quadient.migration.service.deploy.ResourceType
import groovy.transform.Field

import java.nio.file.Paths

import static com.quadient.migration.example.common.util.InitMigration.initMigration

@Field Migration migration = initMigration(this.binding.variables["args"])

def allObjects = migration.statusTrackingRepository
    .listAll()
def allEvents = allObjects
    .collect { it.statusEvents }
    .flatten()

def nonActiveEvents = allEvents
    .findAll { it !instanceof Active }
    .sort { it.timestamp }
if (nonActiveEvents.isEmpty()) {
    println "No deployment events found."
    return
}
def output = nonActiveEvents.last().output
def lastDeploymentId = nonActiveEvents
    .last()
    .deploymentId

def deployedObjects = new ArrayList<StatusTrackingWithEvent>()
for (obj in allObjects) {
    def deployedEvent
    def isOverwrite = false
    for (event in obj.statusEvents) {
        if ((event instanceof Deployed || event instanceof com.quadient.migration.data.Error)
            && event.deploymentId == lastDeploymentId) {
            deployedEvent = event
        }

        if ((event instanceof Deployed || event instanceof com.quadient.migration.data.Error)
            && event.deploymentId != lastDeploymentId && event.output == output) {
            isOverwrite = true
        }
    }

    if (deployedEvent != null) {
        deployedObjects.add(new StatusTrackingWithEvent(statusTracking: obj, event: deployedEvent, isOverwrite: isOverwrite))
    }
}

def deployedDocumentObjects = new ArrayList<ObjectWithStatus<DocumentObject>>()
def deployedImages = new ArrayList<ObjectWithStatus<Image>>()
def deployedTextStyles = new ArrayList<ObjectWithStatus<TextStyle>>()
def deployedParagraphStyles = new ArrayList<ObjectWithStatus<ParagraphStyle>>()

for (obj in deployedObjects) {
    switch (obj.statusTracking.resourceType) {
        case ResourceType.DocumentObject:
            def o = migration.documentObjectRepository.find(obj.statusTracking.id)
            deployedDocumentObjects.add(new ObjectWithStatus(statusTracking: obj.statusTracking, event: obj.event, object: o, isOverwrite: obj.isOverwrite))
            break
        case ResourceType.Image:
            def o = migration.imageRepository.find(obj.statusTracking.id)
            deployedImages.add(new ObjectWithStatus(statusTracking: obj.statusTracking, event: obj.event, object: o, isOverwrite: obj.isOverwrite))
            break
        case ResourceType.TextStyle:
            def o = migration.textStyleRepository.find(obj.statusTracking.id)
            deployedTextStyles.add(new ObjectWithStatus(statusTracking: obj.statusTracking, event: obj.event, object: o, isOverwrite: obj.isOverwrite))
            break
        case ResourceType.ParagraphStyle:
            def o = migration.paragraphStyleRepository.find(obj.statusTracking.id)
            deployedParagraphStyles.add(new ObjectWithStatus(statusTracking: obj.statusTracking, event: obj.event, object: o, isOverwrite: obj.isOverwrite))
            break
    }
}

def documentObjectDependencies = new ArrayList<DocumentObject>()
def imageDependencies = new ArrayList<Image>()
def textStyleDependencies = new ArrayList<TextStyle>()
def paragraphStyleDependencies = new ArrayList<ParagraphStyle>()
def alreadyFoundRefs = new HashSet<Ref>()
def queue = new ArrayList<Ref>(deployedDocumentObjects.collect { new DocumentObjectRef(it.object.id) })
while (!queue.isEmpty()) {
    def ref = queue.pop()
    if (alreadyFoundRefs.contains(ref)) {
        continue
    }

    switch (ref) {
        case DocumentObjectRef:
            def refs = migration.documentObjectRepository.findRefs(ref.id)
            queue.addAll(refs)

            if (!deployedDocumentObjects.any { it.object.id == ref.id }) {
                documentObjectDependencies.add(migration.documentObjectRepository.find(ref.id))
            }
            break
        case VariableRef:
            break
        case DisplayRuleRef:
            break
        case ImageRef:
            imageDependencies.add(migration.imageRepository.find(ref.id))
            break
        case TextStyleRef:
            textStyleDependencies.add(migration.textStyleRepository.find(ref.id))
            break
        case ParagraphStyleRef:
            paragraphStyleDependencies.add(migration.paragraphStyleRepository.find(ref.id))
            break
        default: break
    }

    alreadyFoundRefs.add(ref)
}

def dstFile = Paths.get("report", "${migration.projectConfig.name}-${lastDeploymentId}-deployment-report.csv")

def mapper = new ObjectMapper()
def file = dstFile.toFile()
file.createParentDirectories()
file.withWriter { writer ->
    writer.writeLine("id,type,status,deployedAs,internal,icmPath,errorMessage,content")

    // Directly deployed objects
    for (docObj in deployedDocumentObjects) {
        def content = Csv.escapeJson(mapper.writeValueAsString(docObj.object.content))
        def builder = new StringBuilder()
        builder.append(docObj.object.id).append(",")
        builder.append(docObj.object.type.toString()).append(",")
        builder.append(docObj.event.class.simpleName).append(",")
        builder.append(docObj.isOverwrite ? "Overwrite," : "New,")
        builder.append("${docObj.object.internal},")
        builder.append(docObj.event.icmPath).append(",")
        if (docObj.event instanceof com.quadient.migration.data.Error) {
            builder.append("\"${Csv.escapeJson(docObj.event.error)}\",")
        } else {
            builder.append(",")
        }
        builder.append("\"${content}\"")
        writer.writeLine(builder.toString())
    }
    for (img in deployedImages) {
        def builder = new StringBuilder()
        builder.append(img.object.id).append(",")
        builder.append("Image,")
        builder.append(img.event.class.simpleName).append(",")
        builder.append(img.isOverwrite ? "Overwrite," : "New,")
        builder.append(",")
        builder.append(img.event.icmPath).append(",")
        if (img.event instanceof com.quadient.migration.data.Error) {
            builder.append("\"${Csv.escapeJson(img.event.error)}\",")
        } else {
            builder.append(",")
        }
        writer.writeLine(builder.toString())
    }
    for (textStyle in deployedTextStyles) {
        def builder = new StringBuilder()
        def content = Csv.escapeJson(mapper.writeValueAsString(textStyle.object.definition))
        builder.append(textStyle.object.id).append(",")
        builder.append("TextStyle,")
        builder.append(textStyle.event.class.simpleName).append(",")
        builder.append(textStyle.isOverwrite ? "Overwrite," : "New,")
        builder.append(",")
        builder.append(textStyle.event.icmPath).append(",")
        if (textStyle.event instanceof com.quadient.migration.data.Error) {
            builder.append("\"${Csv.escapeJson(textStyle.event.error)}\",")
        } else {
            builder.append(",")
        }
        builder.append("\"${content}\"")
        writer.writeLine(builder.toString())
    }
    for (paragraphStyle in deployedParagraphStyles) {
        def builder = new StringBuilder()
        def content = Csv.escapeJson(mapper.writeValueAsString(paragraphStyle.object.definition))
        builder.append(paragraphStyle.object.id).append(",")
        builder.append("ParagraphStyle,")
        builder.append(paragraphStyle.event.class.simpleName).append(",")
        builder.append(paragraphStyle.isOverwrite ? "Overwrite," : "New,")
        builder.append(",")
        builder.append(paragraphStyle.event.icmPath).append(",")
        if (paragraphStyle.event instanceof com.quadient.migration.data.Error) {
            builder.append("\"${Csv.escapeJson(paragraphStyle.event.error)}\",")
        } else {
            builder.append(",")
        }
        builder.append("\"${content}\"")
        writer.writeLine(builder.toString())
    }

    // Objects deployed as dependency
    for (docObj in documentObjectDependencies) {
        def builder = new StringBuilder()
        def content = Csv.escapeJson(mapper.writeValueAsString(docObj.content))
        builder.append(docObj.id).append(",")
        builder.append(docObj.type.toString()).append(",")
        builder.append(",")
        builder.append(docObj.internal ? "Inline," : "Reused,")
        builder.append("${docObj.internal},,,")
        builder.append("\"${content}\"")
        writer.writeLine(builder.toString())
    }
    for (img in imageDependencies) {
        def builder = new StringBuilder()
        builder.append(img.id).append(",")
        builder.append("Image,")
        builder.append(",")
        builder.append("Reused,,,,")
        writer.writeLine(builder.toString())
    }
    for (textStyle in textStyleDependencies) {
        def builder = new StringBuilder()
        def content = Csv.escapeJson(mapper.writeValueAsString(textStyle.definition))
        builder.append(textStyle.id).append(",")
        builder.append("TextStyle,")
        builder.append(",")
        builder.append("Reused,,,,")
        builder.append("\"${content}\"")
        writer.writeLine(builder.toString())
    }
    for (paragraphStyle in paragraphStyleDependencies) {
        def builder = new StringBuilder()
        def content = Csv.escapeJson(mapper.writeValueAsString(paragraphStyle.definition))
        builder.append(paragraphStyle.id).append(",")
        builder.append("ParagraphStyle,")
        builder.append(",")
        builder.append("Reused,,,,")
        builder.append("\"${content}\"")
        writer.writeLine(builder.toString())
    }
}

class StatusTrackingWithEvent {
    StatusTracking statusTracking
    StatusEvent event
    Boolean isOverwrite
}

class ObjectWithStatus<T> {
    StatusTracking statusTracking
    StatusEvent event
    T object
    Boolean isOverwrite
}
