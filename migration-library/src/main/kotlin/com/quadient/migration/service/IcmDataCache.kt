package com.quadient.migration.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.quadient.migration.service.inspirebuilder.FontKey
import com.quadient.migration.service.inspirebuilder.fontDataStringToMap
import com.quadient.migration.service.ipsclient.IpsService
import com.quadient.migration.shared.IcmPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.collections.toList

abstract class IcmDataCache(
    private val ipsService: IpsService,
    private val resourcePathProvider: ResourcePathProvider
) {
    private val logger = LoggerFactory.getLogger(this::class.java)!!
    private val lenientJson = Json { ignoreUnknownKeys = true }
    private val xmlMapper by lazy { XmlMapper().registerKotlinModule() }
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
            val name = node["Name"]?.textValue() ?: return@forEach
            val raw = node["CustomProperty"]?.textValue() ?: return@forEach
            val displayName = lenientJson.decodeFromString<StyleCustomProperty>(raw).displayName ?: return@forEach
            result.putIfAbsent(displayName, name)
        }
        return result
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
        return BaseTemplateData(parseInteractiveFlowNamesToIds(layoutXmlTree))
    }

    private fun parseInteractiveFlowNamesToIds(layoutXmlTree: JsonNode): Map<String, String> {
        val pagesInteractiveFlowNode = layoutXmlTree["Pages"]?.get("InteractiveFlow") ?: return emptyMap()
        val flowNodes = layoutXmlTree["Flow"] ?: return emptyMap()

        val interactiveFlowIds = if (pagesInteractiveFlowNode is ArrayNode) {
            pagesInteractiveFlowNode.map { it["FlowId"].textValue() }
        } else {
            listOf(pagesInteractiveFlowNode["FlowId"].textValue())
        }

        val result = mutableMapOf<String, String>()
        interactiveFlowIds.forEachIndexed { i, id ->
            val flowData = flowNodes.first { flow -> flow["Id"].textValue() == id }
            flowData["Name"]?.textValue()?.let { result[it] = "Def.InteractiveFlow$i" }

            flowData["CustomProperty"]?.textValue()?.let { raw ->
                lenientJson.decodeFromString<FlowCustomProperty>(raw).customName?.let {
                    result[it] = "Def.InteractiveFlow$i"
                }
            }
        }
        return result
    }

    data class StyleDefinitionData(
        val textStyleDisplayNamesToNames: Map<String, String>,
        val paragraphStyleDisplayNamesToNames: Map<String, String>,
        val tableStyleDisplayNamesToName: Map<String, String>,
    )

    data class BaseTemplateData(
        val interactiveFlowNamesToIds: Map<String, String>,
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
