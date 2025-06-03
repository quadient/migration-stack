package com.quadient.migration.tools

import kotlin.reflect.full.primaryConstructor

fun flow(init: Flow.() -> Unit) = Flow().apply(init)

class Paragraph : VffNode("p")
class Default : VffNode()
class Condition : VffNode() {
    var inline: String? by attributes

    fun flow(init: Flow.() -> Unit) = addChild(init)
    fun p(init: Paragraph.() -> Unit) = addChild(init)
}

class Flow : VffNode() {
    var type: Type? by attributes
    var location: String? by attributes

    fun condition(init: Condition.() -> Unit) = addChild(init)
    fun default(init: Default.() -> Unit) = addChild(init)
    fun p(init: Paragraph.() -> Unit) = addChild(init)

    enum class Type {
        SelectByCondition, Simple, Section, DirectExternal;

        override fun toString() = super.toString().lowercase()
    }
}

@VffDsl
abstract class VffNode(private val nameOverride: String? = null) {
    private val children = mutableListOf<VffNode>()
    protected val attributes = mutableMapOf<String, Any?>()
    private var content = ""

    private fun renderChildren(): String = children.joinToString(separator = "")
    internal inline fun <reified T : VffNode> addChild(init: T.() -> Unit) =
        (T::class.primaryConstructor ?: throw RuntimeException("VffNode does not have primary constructor")).call()
            .apply(init).apply(children::add)


    override fun toString(): String {
        val cls = nameOverride ?: (this::class.simpleName
            ?: throw RuntimeException("VffNode does not have simple class name")).lowercase()
        val attrsBody =
            attributes.entries.filter { !it.value?.toString().isNullOrEmpty() }.joinToString(separator = " ") { (key, value) ->
                """$key="$value""""
            }.let {
                if (it.isEmpty()) {
                    ""
                } else {
                    " $it"
                }
            }

        return """<$cls$attrsBody>${renderChildren()}$content</$cls>"""
    }

    operator fun String.unaryPlus() {
        content += this
    }
}

@DslMarker
annotation class VffDsl
