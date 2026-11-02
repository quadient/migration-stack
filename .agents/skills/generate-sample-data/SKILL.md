---
name: generate-sample-data
description: Generate sample/test JSON data matching a migration variable structure (the data definition deployed to Inspire). Use when the user asks for sample data, test data, input data or example JSON for a variable structure, data definition or the current migration project.
compatibility: Requires Node.js; DB access via psql, Docker or the Gradle VariablesExport task
---

# Generate sample data for a variable structure

Optional inputs from the request: variable structure id, array sizes, output path, project config file
(overrides `active-project-config`). Array sizes: `--top-array-size` = records of the outermost arrays (not
nested in another array, typically `Clients`), `--array-size` = records of nested arrays. Both default to 3;
a single unqualified size ("arrays of 5") sets both, "one client" / "one top-level record" sets only the top one.

Produces a JSON file whose shape matches the variable structure of the **current migration project**, filled
with plausible values. Two helpers live next to this file:

- `variable-structure.sql` – reads one structure from the working DB (raw entries, no resolution).
- `sample-data.mjs` – resolves paths, builds the tree, prints the field list or writes the final JSON
  (accepts the SQL output **or** the `VariablesExport` CSV, so both sources share one resolution logic).

`SKILL_DIR` below = the directory of this file (`.agents/skills/generate-sample-data`). Write intermediate
files to the scratchpad directory, not into the repo.

## 1. Resolve the current project

All config lives in `migration-examples/src/main/resources/`.

1. Project config file: `--project-config` argument if given, else the first non-empty line not starting with
   `#` in `active-project-config` (no extension), else `project-config.toml`.
2. Project name = `name` in that file. Every DB query **must** filter `project_name = <name>`.
3. Structure id: argument if given → `defaultVariableStructure` in the project config (if not commented out)
   → the only structure of the project → otherwise list the project's structures and ask the user.

## 2. Load the structure – fallback order

DB settings are `[dbConfig]` in `migration-config.toml` (host, port, dbName, user, password). Never echo the
password; pass it via the `PGPASSWORD` env var. Commands below use POSIX shell syntax – adapt them to the
shell in use (e.g. `$env:PGPASSWORD`, `gradlew.bat` in PowerShell/cmd). Use the first option that works:

1. **`psql` on PATH** (native install, Windows service, remote host):
   ```bash
   PGPASSWORD='<pw>' psql -h <host> -p <port> -U <user> -d <dbName> -X -At -v ON_ERROR_STOP=1 \
     -v proj='<project>' -v vs='<structureId>' -f "$SKILL_DIR/variable-structure.sql" > "$SCRATCH/structure.json"
   ```
2. **`psql` of a local PostgreSQL install that is not on PATH** – look in the platform's usual install
   location (newest version) and run the same command with it, e.g. `C:/Program Files/PostgreSQL/*/bin`
   (Windows), `/usr/lib/postgresql/*/bin` or `/usr/pgsql-*/bin` (Linux),
   `/opt/homebrew/opt/postgresql*/bin` or `/Applications/Postgres.app/Contents/Versions/latest/bin` (macOS).
3. **Docker container** publishing the configured port – find it with
   `docker ps --filter publish=<port> --format '{{.Names}}'` (fallback: any running container whose image is
   `postgres*`), then pipe the SQL to the psql inside it:
   ```bash
   docker exec -i -e PGPASSWORD='<pw>' <container> psql -U <user> -d <dbName> -X -At -v ON_ERROR_STOP=1 \
     -v proj='<project>' -v vs='<structureId>' < "$SKILL_DIR/variable-structure.sql" > "$SCRATCH/structure.json"
   ```
4. **Gradle CSV export** (uses the project's own JDBC connection – works whenever the migration scripts work,
   no psql needed). From the repo root:
   ```bash
   ./gradlew :migration-examples:VariablesExport -PscriptArgs="--variable-structure-id <structureId>"
   ```
   The CSV lands in `migration-examples/mapping/` and its name ends with `<structureId>.csv` (take the newest
   match). Rows without `inspire_path` are not in the structure and are ignored by the script. Caveat: if the
   project config selects specific document objects, the export may contain only their variables.
5. **User-provided CSV** in the same `VariablesExport` format – ask for it if everything above fails, and
   report what failed for each option.

To list structures (step 1.3) with options 1–3, run
`select id, name from variable_structure where project_name = '<project>'` the same way.

## 3. Check the field list

```bash
node "$SKILL_DIR/sample-data.mjs" "$SCRATCH/structure.json"     # or the .csv
```

Prints the tree (`Name[]` = Array, `Name{}` = SubTree, then type and variable id) plus `WARN` lines on stderr
(unresolvable references, missing variable rows). An empty structure or warnings are worth telling the user.

How the structure works (the script implements it; know it to interpret the output):
- Each structure entry's `path` is the **parent** path; the field name is appended. Field name =
  structure entry `name` → variable `name` → variable id.
- A path is a literal (`Data.Clients.Value`) or a reference to another variable (`$clientsArray` /
  `{"variableId": …}`) meaning "inside that variable" – Array parents add `.Value`; references chain.
- `Data` is the root and is dropped. `X.Value` = X is an array; another segment = subtree. An explicit
  `Array`/`SubTree` variable at that path wins.

## 4. Choose values and build the JSON

Write `$SCRATCH/values.json` mapping **variable id** → value, or a list of values. Each time a field is emitted
the next list item is used (cycling), so for a field inside arrays give as many distinct values as it will be
emitted (product of the enclosing array sizes, e.g. with defaults 3 for `Clients[]`, 9 for `Clients[].Jobs[]`;
with `--top-array-size 1` 1 and 3)
– records should differ from each other, and related fields in the same record should fit together
(first name / last name / email, city / state / zip). Unlisted ids get the defaults below.

Values by type:
- `String` – realistic for the name (`DRV_RecipientFirstName` → "John", `DRV_City` → "New York",
  `*Email*` → "john.smith@example.com", `*Zip*` → "10001", `*_Indicator`, `*Flag*` → "Y"). If unsure: `"string"`.
- `Integer` – whole number fitting the name (counts small, ids/numbers longer). Unsure: `0`.
- `Currency` – JSON number with up to two decimals, e.g. `1250.75`. Unsure: `0`.
- `DateTime` – ISO string; `"2026-03-15"` for dates, `"2026-03-15T14:30:00"` when the name implies time.
- `Boolean` – `true` unless the name clearly implies otherwise.
- Language variable (printed in the header) – a language code like `"en"`.

Unless the user says otherwise every field gets a value and every array 3 records. Then:

```bash
node "$SKILL_DIR/sample-data.mjs" "$SCRATCH/structure.json" --values "$SCRATCH/values.json" \
  --top-array-size 3 --array-size 3 --out "<output>"
```

Default output: `migration-examples/sample-data/<project>_<structureId>.json` (git-ignored) unless the user
named a path. Show the user the path, a short excerpt, and any warnings.
