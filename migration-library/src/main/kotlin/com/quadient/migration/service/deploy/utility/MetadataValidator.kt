package com.quadient.migration.service.deploy.utility

import com.quadient.migration.api.dto.migrationmodel.DisplayRule
import com.quadient.migration.api.dto.migrationmodel.DocumentObject
import com.quadient.migration.api.dto.migrationmodel.Image
import com.quadient.migration.tools.caseInsensitiveSetOf

interface MetadataValidator {
    companion object {
        val DISALLOWED_METADATA = caseInsensitiveSetOf(
            "Type",
            "Dependencies",
            "Tags",
            "Subject",
            "PublicTemplate",
            "StateId",
            "BusinessProcess",
            "TicketTitle",
            "TicketDescription",
            "TicketIcon",
            "UserAttachment",
            "GlobalStorageAttachment",
            "Languages",
            "MailMergeModule",
            "MailMergePath",
            "Guid",
            "IM_Path",
            "IM_PM_Name",
            "IM_PM_Location",
            "IM_Context",
            "ModuleNames",
            "EM_Paths",
            "ResultType",
            "ParamTypes",
            "Channels",
            "Master Template",
            "ProductionActions",
            "Html Content",
            "Responsive Html Content",
            "Brand",
            "WFDType",
            "OutputType",
            "PreviewTypes",
            "SupportedChannels",
            "InteractiveFlowsNames",
            "InteractiveFlowsTypes ",
        )
        val IMAGE_DISALLOWED_METADATA = DISALLOWED_METADATA - "Subject"
    }

    fun DocumentObject.getInvalidMetadataKeys(): Set<String> {
        return this.metadata.keys.asSequence().filter { key -> DISALLOWED_METADATA.contains(key) }.toSet()
    }

    fun Image.getInvalidMetadataKeys(): Set<String> {
        return this.metadata.keys.asSequence().filter { key -> IMAGE_DISALLOWED_METADATA.contains(key) }.toSet()
    }

    fun DisplayRule.getInvalidMetadataKeys(): Set<String> {
        return this.metadata.keys.asSequence().filter { key -> DISALLOWED_METADATA.contains(key) }.toSet()
    }
}

class MetadataValidatorImpl : MetadataValidator { }