---
name: generate-sample-data
description: Generate sample/test JSON data matching a migration variable structure (the data definition deployed to Inspire). Use when the user asks for sample data, test data, input data or example JSON for a variable structure, data definition or the current migration project.
compatibility: Requires the migration examples project (JDK + Gradle) with a reachable migration database
---

# Generate sample data for a variable structure

Produces a JSON file whose shape matches a variable structure of the **current migration project**, filled with
plausible values. The structure is read and assembled by the `GenerateSampleData` Gradle task (script
`src/main/groovy/com/quadient/migration/example/common/GenerateSampleData.groovy` of the migration examples
project); your job is choosing the values.

Optional inputs from the request: variable structure id, array sizes, output path, project config file.

## 1. Run the task

Run the `GenerateSampleData` task of the migration examples project with the nearest Gradle wrapper – the project
is the working directory itself or its `migration-examples/` subdirectory, e.g.:

```bash
./gradlew GenerateSampleData -q -PscriptArgs="<args>"
# or, as a subproject of a parent build:
./gradlew :migration-examples:GenerateSampleData -q -PscriptArgs="<args>"
```

Like every migration script, the task works on the current project (its configuration and database). Script
arguments (space separated – use paths without spaces, absolute paths for files outside the project):

- `--variable-structure-id <id>` – defaults to `defaultVariableStructure` of the project config, then to the
  project's only structure; otherwise the task fails and lists the available ids (ask the user which one).
- `--values <file.json>`, `--out <file.json>` – see step 3.
- `--top-array-size <n>` – records of the outermost arrays (not nested in another array, typically `Clients`).
- `--array-size <n>` – records of nested arrays. Both default to 3; `--top-array-size` defaults to
  `--array-size`. A single unqualified size ("arrays of 5") sets both, "one client" / "one top-level record"
  sets only the top one.
- Common overrides of the migration scripts also work, e.g. `--project-name <name>`,
  `--active-project-config <file.toml>`.

## 2. Read the field tree

Without `--values`/`--out` the task only prints the tree (`Name[]` = Array, `Name{}` = SubTree, then data type and
variable id) and `level=WARN` log lines (unresolvable references, missing variable records). The tree is logged
as one multi-line message starting with `Variable structure:`, mixed with other Gradle and log output; it also
lands in the project's `logs/script.log`. Tell the user about warnings or an empty structure.

The names are the final JSON keys. Only nodes with an `id` take values; arrays and subtrees without one are
implied by other variables' paths.

## 3. Choose values and generate

Write a values file (in the scratchpad, not the repo) mapping **variable id** → value, or a list of values. Each
time a field is emitted the next list item is used (cycling), so give a field as many distinct values as it is
emitted – the product of the enclosing array sizes (defaults: 3 for `Clients[]`, 9 for `Clients[].Jobs[]`;
with `--top-array-size 1`: 1 and 3). Records should differ, and fields of one record should fit together (first
name / last name / email, city / state / zip). Ids without a value get a default by type.

Values by type:
- `String` – realistic for the name (`Name` → "John", `City` → "New York",
  `Email` → "john.smith@example.com", `Zip` → "10001", `Indicator`, `Flag` → "Y"). Unsure: `"string"`.
- `Integer`, `Integer64` – whole number fitting the name (counts small, ids/numbers longer). Unsure: `0`.
- `Currency`, `Double` – JSON number, e.g. `1250.75` (trailing zeros like `200.00` are kept). Unsure: `0.0`.
- `DateTime` – ISO string; `"2026-03-15"` for dates, `"2026-03-15T14:30:00"` when the name implies time.
- `Boolean` – `true` unless the name clearly implies otherwise.
- Language variable (printed in the first line) – a language code like `"en"`.

Then run the task again with the same arguments plus `--values <file>` (and `--out <file>` if the user named
one). Default output: `sample-data/<project>_<structureId>.json` in the migration examples project (git-ignored);
the task prints the absolute path. Show the user the path, a short excerpt and any warnings.
