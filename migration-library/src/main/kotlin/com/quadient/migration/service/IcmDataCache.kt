package com.quadient.migration.service

import com.quadient.migration.service.inspirebuilder.FontKey
import com.quadient.migration.service.inspirebuilder.fontDataStringToMap
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.IcmPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.quadient.migration.tools.logger
import kotlin.collections.toList
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule

abstract class IcmDataCache(
    private val ipsService: IpsService,
    private val resourcePathProvider: ResourcePathProvider
) {
    private val logger by logger()
    private val lenientJson = Json { ignoreUnknownKeys = true }
    private val xmlMapper by lazy { XmlMapper.builder().addModule(KotlinModule.Builder().build()).build() }
    private val baseTemplateCache = mutableMapOf<IcmPath, BaseTemplateData?>()
    private val wfd2XmlCache = mutableMapOf<IcmPath, String>()

    val font: Map<FontKey, String> by lazy {
        fontDataStringToMap(ipsService.gatherFontData(resourcePathProvider.getFontRootFolder()))
    }

    fun wfd2Xml(path: IcmPath): String {
        return wfd2XmlCache.getOrPut(path) {
            ipsService.wfd2xml(path)
        }
    }

    private val fileExistence: MutableMap<IcmPath, Boolean> = mutableMapOf()
    fun fileExists(path: IcmPath): Boolean {
        return fileExistence.getOrPut(path) {
            ipsService.fileExists(path)
        }
    }

    val styleDefinitionData: StyleDefinitionData? by lazy {
        val path = resourcePathProvider.getStyleDefinitionPath()
        if (!fileExists(path)) {
            logger.warn("Style definition '$path' does not exist. Style display name resolution will be skipped.")
            return@lazy null
        }

        try {
            val data = ipsService.wfd2xml(path)
            val result = parseStyleDefinitionData(data)

            result
        } catch (e: Exception) {
            logger.warn("Failed to load style definition data from '$path'.", e)
            null
        }
    }

    private fun parseStyleDefinitionData(xml: String): StyleDefinitionData {
        val layoutXmlTree = xmlMapper.readTree(xml.trimIndent())["Layout"]["Layout"]
        return StyleDefinitionData(
            parseStyleDisplayNamesToNames(layoutXmlTree, "TextStyle"),
            parseStyleDisplayNamesToNames(layoutXmlTree, "ParaStyle"),
            parseStyleDisplayNamesToNames(layoutXmlTree, "TableStyle"),
        )
    }

    private fun parseStyleDisplayNamesToNames(layoutXmlTree: JsonNode, nodeTag: String): Map<String, String> {
        val styleNode = layoutXmlTree[nodeTag] ?: return emptyMap()
        val styleNodeList = if (styleNode is ArrayNode) styleNode.toList() else listOf(styleNode)
        val result = mutableMapOf<String, String>()
        styleNodeList.forEach { node ->
            val name = node["Name"]?.xmlText() ?: return@forEach
            val raw = node["CustomProperty"]?.xmlText() ?: return@forEach
            val displayName = lenientJson.decodeFromString<StyleCustomProperty>(raw).displayName ?: return@forEach
            result.putIfAbsent(displayName, name)
        }
        return result
    }

    private fun JsonNode.xmlText(): String? = when {
        isString -> asString()
        isObject -> get("")?.takeIf { it.isString }?.asString()
        else -> null
    }

    fun getOrLoadBaseTemplateData(path: IcmPath): BaseTemplateData? {
        if (baseTemplateCache.containsKey(path)) return baseTemplateCache[path]

        if (!fileExists(path)) {
            baseTemplateCache[path] = null
            return null
        }

        return try {
            val xml = ipsService.wfd2xml(path)
            parseBaseTemplateData(xml).also { baseTemplateCache[path] = it }
        } catch (e: Exception) {
            logger.warn("Failed to load base template data from '$path'.", e)
            baseTemplateCache[path] = null
            null
        }
    }

    private fun parseBaseTemplateData(xml: String): BaseTemplateData {
        val layoutXmlTree = xmlMapper.readTree(xml.trimIndent())["Layout"]["Layout"]
        val pagesInteractiveFlowNode = layoutXmlTree["Pages"]?.get("InteractiveFlow")
            ?: return BaseTemplateData(emptyMap(), null, null)
        val flowNodes = layoutXmlTree["Flow"]
            ?: return BaseTemplateData(emptyMap(), null, null)

        val interactiveFlowIds: List<String> = if (pagesInteractiveFlowNode is ArrayNode) {
            pagesInteractiveFlowNode.toList().map { it["FlowId"].asString() }
        } else {
            listOf(pagesInteractiveFlowNode["FlowId"].asString())
        }
        val emailBodyRootFlowId =
            layoutXmlTree["ECPlaceHolder"]?.find { it["Id"][""].stringValue() == "Def.EmailsBody" }?.get("ContentId")
                ?.stringValue()
        val smsRootFlowId = layoutXmlTree["SMSRoot"]?.get("FlowId")?.stringValue()

        val interactiveFlowNamesToIds = mutableMapOf<String, String>()
        var smsFlowId: String? = null
        var emailFlowId: String? = null

        interactiveFlowIds.forEachIndexed { i, id ->
            when {
                emailBodyRootFlowId != null && id == emailBodyRootFlowId -> emailFlowId = "Def.InteractiveFlow$i"
                smsRootFlowId != null && id == smsRootFlowId -> smsFlowId = "Def.InteractiveFlow$i"
            }
            val flowData = flowNodes.first { flow -> flow["Id"].asString() == id }
            flowData["Name"]?.stringValue()?.let { interactiveFlowNamesToIds[it] = "Def.InteractiveFlow$i" }

            flowData["CustomProperty"]?.stringValue()?.let { raw ->
                lenientJson.decodeFromString<FlowCustomProperty>(raw).customName?.let {
                    interactiveFlowNamesToIds[it] = "Def.InteractiveFlow$i"
                }
            }
        }

        return BaseTemplateData(
            interactiveFlowNamesToIds = interactiveFlowNamesToIds,
            emailFlowId = emailFlowId,
            smsFlowId = smsFlowId,
        )
    }

    data class StyleDefinitionData(
        val textStyleDisplayNamesToNames: Map<String, String>,
        val paragraphStyleDisplayNamesToNames: Map<String, String>,
        val tableStyleDisplayNamesToName: Map<String, String>,
    )

    data class BaseTemplateData(
        val interactiveFlowNamesToIds: Map<String, String>,
        val emailFlowId: String?,
        val smsFlowId: String?,
    )

    @Serializable
    private data class StyleCustomProperty(
        @SerialName("DisplayName") val displayName: String? = null,
    )

    @Serializable
    private data class FlowCustomProperty(
        val customName: String? = null,
    )
}

class DesignerIcmDataCache(ipsService: IpsService, resourcePathProvider: DesignerResourcePathProvider) :
    IcmDataCache(ipsService, resourcePathProvider)

class InteractiveIcmDataCache(ipsService: IpsService, resourcePathProvider: InteractiveResourcePathProvider) :
    IcmDataCache(ipsService, resourcePathProvider)

class EvolveIcmDataCache(ipsService: IpsService, resourcePathProvider: EvolveResourcePathProvider) :
    IcmDataCache(ipsService, resourcePathProvider)
