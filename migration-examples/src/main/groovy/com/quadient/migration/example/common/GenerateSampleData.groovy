//! ---
//! displayName: Generate Sample Data
//! category: Utils
//! description: Prints the field tree of a variable structure and optionally generates sample JSON data matching it. Arguments: --variable-structure-id <id> (defaults to the project's default or only structure), --values <values.json> mapping variable ids to a value or a list of values, --out <file> (defaults to 'sample-data/<project-name>_<structure-id>.json'), --array-size <n> for nested arrays and --top-array-size <n> for the outermost arrays (both default to 3). The JSON is written when --values or --out is given.
//! ---
package com.quadient.migration.example.common

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.Variable
import com.quadient.migration.api.dto.migrationmodel.VariableStructure
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.LiteralPath
import com.quadient.migration.shared.VariablePath
import com.quadient.migration.shared.VariableRefPath
import com.quadient.migration.example.common.util.PathUtil
import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.LocalDate

import static com.quadient.migration.example.common.util.InitMigration.initMigration
import static com.quadient.migration.example.common.util.ScriptArgs.getValueOfArg

@Field static Logger log = LoggerFactory.getLogger(this.class.name)

def migration = initMigration(this.binding)
def args = this.binding.variables["args"] as List<String>

def structure = selectStructure(migration, getValueOfArg("--variable-structure-id", args).orElse(null))
def fields = resolveFields(migration, structure)

log.info "Variable structure: ${structure.id}, language variable: ${structure.languageVariable?.id ?: "-"}\n${describe(fields.tree)}"
fields.warnings.each { log.warn it }

def valuesArg = getValueOfArg("--values", args).orElse(null)
def outArg = getValueOfArg("--out", args).orElse(null)
if (valuesArg || outArg) {
    def values = valuesArg ? new JsonSlurper().parse(new File(valuesArg)) as Map<String, Object> : [:]
    def arraySize = getValueOfArg("--array-size", args).map { it.toInteger() }.orElse(3)
    def topArraySize = getValueOfArg("--top-array-size", args).map { it.toInteger() }.orElse(arraySize)

    def data = generate(fields.tree, values, arraySize, topArraySize)

    def outFile = outArg
            ? new File(outArg)
            : PathUtil.dataDirPath(binding, "sample-data", "${migration.projectConfig.name}_${structure.id}.json").toFile()
    outFile.createParentDirectories()
    outFile.setText(toJson(data) + "\n", "UTF-8")
    log.info "Sample data written to ${outFile.absolutePath}"
}

class SampleDataNode {
    String name
    String path
    DataType dataType
    String variableId
    Map<String, SampleDataNode> children = [:]

    boolean isContainer() { dataType == DataType.Array || dataType == DataType.SubTree }
}

static VariableStructure selectStructure(Migration migration, String structureId) {
    def id = structureId ?: migration.projectConfig.defaultVariableStructure
    if (id) {
        def structure = migration.variableStructureRepository.find(id)
        if (structure == null) {
            throw new IllegalArgumentException("Variable structure '$id' not found in project '${migration.projectConfig.name}'.")
        }
        return structure
    }

    def all = migration.variableStructureRepository.listAll()
    if (all.size() == 1) {
        return all.first()
    }
    if (all.isEmpty()) {
        throw new IllegalArgumentException("Project '${migration.projectConfig.name}' has no variable structures.")
    }
    throw new IllegalArgumentException("Project '${migration.projectConfig.name}' has ${all.size()} variable structures, " +
            "specify one with --variable-structure-id: ${all.collect { it.id }.join(", ")}")
}

static Map resolveFields(Migration migration, VariableStructure structure) {
    def variables = [:] as Map<String, Variable>
    def findVariable = { String id -> variables.computeIfAbsent(id) { migration.variableRepository.find(id) } }

    def warnings = []
    def paths = [:] as LinkedHashMap<String, Map>

    structure.structure.each { id, pathData ->
        def parentPath = resolvePath(pathData.path, structure, findVariable, warnings)
        if (!parentPath) {
            warnings << "Variable '$id' has no resolvable path - skipped"
            return
        }

        def variable = findVariable(id)
        if (variable == null) {
            warnings << "Variable '$id' has no variable record - treated as String"
        }

        def name = pathData.name ?: variable?.nameOrId() ?: id
        def path = removeDataFromPath("${parentPath}.${name}")
        if (!path || path == "SystemVariable" || path.startsWith("SystemVariable.")) {
            return
        }

        paths[path] = [id: id, dataType: variable?.dataType ?: DataType.String]
    }

    return [tree: buildTree(paths), warnings: warnings*.toString()]
}

static String resolvePath(VariablePath path, VariableStructure structure, Closure<Variable> findVariable,
                          List<String> warnings, Set<String> visited = [] as Set) {
    if (path instanceof LiteralPath) {
        return path.path
    }

    def refId = (path as VariableRefPath).variableId
    def parentPathData = structure.structure[refId]
    if (parentPathData == null || !visited.add(refId)) {
        return null
    }

    def parentPath = resolvePath(parentPathData.path, structure, findVariable, warnings, visited)
    def parent = findVariable(refId)
    if (!parentPath || parent == null) {
        return null
    }

    def parentName = parentPathData.name ?: parent.nameOrId()
    switch (parent.dataType) {
        case DataType.Array: return "${parentPath}.${parentName}.Value"
        case DataType.SubTree: return "${parentPath}.${parentName}"
        default:
            warnings << "Variable '$refId' of type ${parent.dataType} is used in a path, only Array and SubTree can be referenced"
            return null
    }
}

static String removeDataFromPath(String path) {
    return path == "Data" ? "" : path.startsWith("Data.") ? path.substring("Data.".length()) : path
}

static String removeValueFromPath(String path) {
    return path.split("\\.").findAll { it.toLowerCase() != "value" }.join(".")
}

static Map<String, SampleDataNode> buildTree(Map<String, Map> paths) {
    def explicit = paths.collectEntries { path, info -> [(removeValueFromPath(path)): info] } as Map<String, Map>
    def root = [:] as LinkedHashMap<String, SampleDataNode>

    paths.keySet().each { path ->
        def parts = path.split("\\.")
        def current = root
        def currentPath = []

        parts.eachWithIndex { part, i ->
            if (part.toLowerCase() == "value") {
                return
            }

            currentPath << part
            def nodePath = currentPath.join(".")
            def isArray = i + 1 < parts.length && parts[i + 1].toLowerCase() == "value"
            def node = current.computeIfAbsent(part) {
                def info = explicit[nodePath]
                def dataType = info?.dataType ?: (isArray ? DataType.Array : DataType.SubTree)
                new SampleDataNode(name: part, path: nodePath, dataType: dataType, variableId: info?.id)
            }

            if (!node.container && i != parts.length - 1) {
                throw new IllegalStateException("Leaf variable '${node.name}' cannot contain child variables")
            }
            current = node.children
        }
    }

    return root
}

static String describe(Map<String, SampleDataNode> tree) {
    def lines = []
    def walk
    walk = { Map<String, SampleDataNode> nodes, int depth ->
        nodes.values().each { node ->
            def suffix = node.dataType == DataType.Array ? "[]" : node.dataType == DataType.SubTree ? "{}" : ""
            lines << "${"  " * depth}${node.name}$suffix  ${node.dataType}${node.variableId ? "  id=${node.variableId}" : ""}"
            walk(node.children, depth + 1)
        }
    }
    walk(tree, 0)
    return lines.join("\n")
}

static Map generate(Map<String, SampleDataNode> tree, Map<String, Object> values, int arraySize, int topArraySize) {
    def counters = [:] as Map<String, Integer>

    def leafValue = { SampleDataNode node ->
        def n = counters.merge(node.path, 1, Integer::sum) - 1
        def value = node.variableId != null ? values[node.variableId] : null
        if (value instanceof List) {
            return value ? value[n % value.size()] : defaultValue(node.dataType)
        }
        return value != null ? value : defaultValue(node.dataType)
    }

    def build
    build = { Map<String, SampleDataNode> nodes, boolean inArray ->
        def obj = [:] as LinkedHashMap<String, Object>
        nodes.values().each { node ->
            if (node.dataType == DataType.Array) {
                obj[node.name] = (0..<(inArray ? arraySize : topArraySize)).collect { build(node.children, true) }
            } else if (node.dataType == DataType.SubTree) {
                obj[node.name] = build(node.children, inArray)
            } else {
                obj[node.name] = leafValue(node)
            }
        }
        return obj
    }

    return build(tree, false)
}

static Object defaultValue(DataType dataType) {
    switch (dataType) {
        case DataType.Integer:
        case DataType.Integer64: return 0
        case DataType.Double:
        case DataType.Currency: return 0.0
        case DataType.Boolean: return true
        case DataType.DateTime: return LocalDate.now().toString()
        default: return "string"
    }
}

static String toJson(Object data) {
    def generator = new JsonGenerator.Options().disableUnicodeEscaping().build()
    return JsonOutput.prettyPrint(generator.toJson(data), true)
}
