package com.quadient.migration.example.azureAI.categorizeAndTranslate

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.DocumentObjectRef

static ArrayList translateSectionsTemplatesGroups(DocumentObject documentObject, Migration migration) {
    if (documentObject.customFields.get("usedObjects")) {
        List translateObjects = new ArrayList()
        documentObject.customFields.get("usedObjects")?.toString()?.tokenize(",")?.collect {
            translateObjects.add(new DocumentObjectRef(it))
        }
        return translateObjects
    }
    return []
}

