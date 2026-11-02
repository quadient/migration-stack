#!/usr/bin/env node
// Builds sample JSON data for a migration variable structure.
//
// Usage:
//   node sample-data.mjs <input.json|input.csv> [--values values.json] [--array-size 3] [--top-array-size N] [--out out.json]
//
// --array-size      records per nested array (default 3)
// --top-array-size  records per outermost array, i.e. not nested in another array (default = --array-size)
//
// <input>  JSON produced by variable-structure.sql, or the CSV produced by the VariablesExport gradle task.
// Without --values: prints the resolved field list (one line per leaf) so values can be chosen.
// With --values:    writes the sample data JSON. values.json maps variable id -> value or list of values;
//                   a list is consumed in order each time the field is emitted (cycling), so fields
//                   inside arrays get distinct values. Missing ids fall back to a per-type default.

import fs from "node:fs";

const args = process.argv.slice(2);
const opt = (name, dflt) => {
    const i = args.indexOf(name);
    if (i < 0) return dflt;
    const v = args[i + 1];
    args.splice(i, 2);
    return v;
};
const valuesFile = opt("--values");
const arraySize = Number(opt("--array-size", "3"));
const topArraySize = Number(opt("--top-array-size", String(arraySize)));
const outFile = opt("--out");
const inputFile = args[0];
if (!inputFile) {
    console.error("Usage: node sample-data.mjs <input.json|input.csv> [--values values.json] [--array-size 3] [--top-array-size N] [--out out.json]");
    process.exit(2);
}

const warnings = [];

// ---- input -------------------------------------------------------------------------------------

function splitCsvLine(line) {
    const cols = [];
    let cur = "", inQuotes = false, quoted = false;
    for (let i = 0; i < line.length; i++) {
        const c = line[i];
        if (inQuotes) {
            if (c === '"' && line[i + 1] === '"') { cur += '"'; i++; }
            else if (c === '"') inQuotes = false;
            else cur += c;
        } else if (c === ",") { cols.push(cur); cur = ""; quoted = false; }
        else if (cur.length === 0 && c === '"' && !quoted) { inQuotes = true; quoted = true; }
        else cur += c;
    }
    cols.push(cur);
    return cols;
}

// VariablesExport CSV: id,name,data_type,inspire_path,inspire_name,language_variable,originLocations
// Variables without inspire_path are not part of the structure.
function readCsv(text) {
    const lines = text.split(/\r?\n/).filter(l => l.trim() !== "");
    const header = splitCsvLine(lines[0]).map(h => h.trim());
    const col = n => header.indexOf(n);
    const variables = [];
    let languageVariable = null;
    for (const line of lines.slice(1)) {
        const c = splitCsvLine(line);
        const id = c[col("id")], path = c[col("inspire_path")];
        if (!path) continue;
        if ((c[col("language_variable")] || "").trim().toLowerCase() === "true") languageVariable = id;
        variables.push({
            id,
            name: c[col("inspire_name")] || c[col("name")] || id,
            dataType: c[col("data_type")] || null,
            literalPath: path.startsWith("$") ? null : path,
            refId: path.startsWith("$") ? path.slice(1) : null,
        });
    }
    return { structureId: null, languageVariable, variables };
}

const raw = fs.readFileSync(inputFile, "utf8").replace(/^﻿/, "");
const input = inputFile.toLowerCase().endsWith(".csv") ? readCsv(raw) : JSON.parse(raw);
const byId = new Map(input.variables.map(v => [v.id, v]));

// ---- path resolution (mirrors VariablePath.resolve in InspireDocumentObjectBuilder.kt) -----------

function parentPath(v, seen = new Set()) {
    if (v.literalPath != null) return v.literalPath;
    if (seen.has(v.id)) return null;
    seen.add(v.id);
    const parent = byId.get(v.refId);
    if (!parent) return null;
    const pp = parentPath(parent, seen);
    if (!pp) return null;
    if (parent.dataType === "Array") return `${pp}.${parent.name}.Value`;
    if (parent.dataType === "SubTree") return `${pp}.${parent.name}`;
    warnings.push(`'${v.id}' references '${parent.id}' of type ${parent.dataType}; only Array/SubTree can be referenced`);
    return null;
}

const resolved = [];
for (const v of input.variables) {
    const pp = parentPath(v);
    if (!pp) { warnings.push(`'${v.id}' has an unresolvable path - skipped`); continue; }
    if (!v.dataType) warnings.push(`'${v.id}' has no variable record - treated as String`);
    // segments without "Data" root and "Value" markers; isArray marks segments followed by ".Value"
    const parts = `${pp}.${v.name}`.split(".");
    const segs = [];
    parts.forEach((p, i) => {
        if (p.toLowerCase() === "value") return;
        if (i === 0 && p === "Data") return;
        segs.push({ name: p, isArray: parts[i + 1]?.toLowerCase() === "value" });
    });
    resolved.push({ ...v, dataType: v.dataType || "String", segs });
}

// ---- tree (mirrors buildVariableTree in InspireBuilderUtils.kt) ---------------------------------

const root = { kind: "SubTree", children: new Map() };
const explicitType = new Map(resolved.map(v => [v.segs.map(s => s.name).join("."), v]));
for (const v of resolved) {
    let node = root;
    v.segs.forEach((s, i) => {
        const key = v.segs.slice(0, i + 1).map(x => x.name).join(".");
        let child = node.children.get(s.name);
        if (!child) {
            const ex = explicitType.get(key);
            const kind = ex ? ex.dataType : (s.isArray ? "Array" : "SubTree");
            child = { kind, id: ex?.id, path: key, children: new Map() };
            node.children.set(s.name, child);
        }
        if (child.kind !== "Array" && child.kind !== "SubTree" && i < v.segs.length - 1) {
            throw new Error(`Leaf variable '${key}' cannot contain child variables (via '${v.id}')`);
        }
        node = child;
    });
}

// ---- output -------------------------------------------------------------------------------------

if (!valuesFile) {
    const lines = [];
    const walk = (node, depth) => {
        for (const [name, c] of node.children) {
            const container = c.kind === "Array" ? "[]" : c.kind === "SubTree" ? "{}" : "";
            lines.push(`${"  ".repeat(depth)}${name}${container}  ${c.kind}${c.id ? `  id=${c.id}` : ""}`);
            walk(c, depth + 1);
        }
    };
    walk(root, 0);
    console.log(`structure: ${input.structureId ?? "(csv)"}  languageVariable: ${input.languageVariable ?? "-"}`);
    console.log(lines.join("\n"));
} else {
    const values = JSON.parse(fs.readFileSync(valuesFile, "utf8"));
    const counters = new Map();
    const today = new Date().toISOString().slice(0, 10);
    const defaults = { String: "string", Integer: 0, Currency: 0.0, Boolean: true, DateTime: today };
    const leafValue = node => {
        const n = counters.get(node.path) ?? 0;
        counters.set(node.path, n + 1);
        const v = node.id != null ? values[node.id] : undefined;
        if (Array.isArray(v)) return v.length ? v[n % v.length] : defaults[node.kind];
        if (v !== undefined) return v;
        return defaults[node.kind] ?? "string";
    };
    // inArray: false until the first (outermost) array, which gets topArraySize records
    const build = (node, inArray = false) => {
        const obj = {};
        for (const [name, c] of node.children) {
            if (c.kind === "Array") obj[name] = Array.from({ length: inArray ? arraySize : topArraySize }, () => build(c, true));
            else if (c.kind === "SubTree") obj[name] = build(c, inArray);
            else obj[name] = leafValue(c);
        }
        return obj;
    };
    const json = JSON.stringify(build(root), null, 2) + "\n";
    if (outFile) { fs.writeFileSync(outFile, json); console.log(`written ${outFile}`); }
    else process.stdout.write(json);
}

for (const w of [...new Set(warnings)]) console.error(`WARN ${w}`);
