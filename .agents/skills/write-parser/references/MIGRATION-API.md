# Migration Library API Reference

## Overview

This document describes the Migration Library API used in parsing scripts. Your parser transforms source format data (
XML, JSON, CSV, etc.) into the **Migration Model** - a common intermediate representation for Quadient Inspire content.

All objects are created using **Builder classes** with a fluent API and stored in repositories.

## Getting Started

Every parser script starts with:

```groovy
import static com.quadient.migration.example.common.util.InitMigration.initMigration

Migration migration = initMigration(this.binding)
```

This initializes the migration context with pre-configured settings from `migration-config.toml` and
`project-config.toml` (user-managed files).

**Do not generate or modify configuration files** - they contain environment-specific settings managed by the user.

## Core Principles

### 1. Builder Pattern

All model objects are created using builder classes that must be instantiated and built:

```groovy
// Create builder, configure it, then call .build()
def myTemplate = new DocumentObjectBuilder("my-template", DocumentObjectType.Template)
        .name("My Template")
        .targetFolder("templates/subfolder")
        .originLocations(["source/template.xml"])
        .addCustomField("sourceType", "template")
        .addCustomField("version", "1.0")
        .build()
```

**Key Rules:**

- Always use `new BuilderName(id)` to instantiate builders
- Use existing IDs from source if available, otherwise generate deterministic IDs based on parent ids, prefixes/suffixes
  and counters.
- Chain methods using `.method()` syntax
- Closures receive an `it` parameter representing the nested builder
- Always call `.build()` at the end to create the object
- Use `.originLocations([fileName])` to track source files (takes `List<String>`)
- Use `.addCustomField(key, value)` to store additional metadata (both key and value must be `String`)
- **Check for null before calling builder methods** - Most builder methods expect non-null values

### 2. Identity and References

- Every top-level object has a unique `id` (required)
- Top-level objects reference each other by ID using `Ref` classes
- Optional `name` property (defaults to id if not set) - applies only to top-level objects

### 3. Common Properties

All **top-level migration objects** inherit from `MigrationObject`:

- `id: String` - Unique identifier (required)
- `name: String?` - Display name (optional, defaults to id)
- `originLocations: List<String>` - Source file paths for traceability
- `customFields: CustomFieldMap` - Additional custom metadata (String key-value pairs only)

**Top-level objects** are those stored in repositories: DocumentObject, Variable, DisplayRule, Image, TextStyle,
ParagraphStyle, VariableStructure, Attachment.

**Content elements** (Paragraph, Area, Table, FirstMatch, SelectByLanguage) are NOT top-level objects and do **NOT**
have `customFields`, `originLocations`, or `name` properties.

**Important Constraints**:

- **customFields values must be String** - For complex structures (lists, maps), serialize to JSON or flatten into
  multiple fields with indexed keys
- **Style definitions required** - TextStyle and ParagraphStyle builders need `.definition()` with lambda (can use
  empty: `.definition { }`)
- **Name uniqueness** - While not required by model, names become file names in Inspire; duplicates will shadow each
  other

## Best Practices

- **Use Meaningful IDs** - Unique, descriptive identifiers with consistent naming (avoid random UUIDs)
- **Define Styles Separately** - Create TextStyle and ParagraphStyle as top-level objects, reference them by ID
- **Organize with Target Folders** - Use logical folder structure: `.targetFolder("blocks/invoices/headers")`
- **Use Skip for Non-Migratable Content** - Mark complex/unsupported content: `.skip("manual-name", "reason...")`
- **Check for Null** - Verify data exists before calling builder methods that expect non-null values
- **Test Iteratively** - Run your script frequently to catch errors early

## Repository API

The `Migration` object provides repositories for storing and retrieving model objects:

```groovy
// Available repositories
migration.variableRepository
migration.variableStructureRepository
migration.documentObjectRepository
migration.paragraphStyleRepository
migration.textStyleRepository
migration.displayRuleRepository
migration.imageRepository
migration.attachmentRepository
```

Store and retrieve migration objects using repository methods:

```groovy
// Save or update object
migration.variableRepository.upsert(variable)
migration.documentObjectRepository.upsert(docObject)

// Find object by ID (returns object or null)
def variable = migration.variableRepository.find(variableId)
if (variable != null) {
    // Update and save
    variable.customFields.put("key", "value")
    migration.variableRepository.upsert(variable)
}

// List all objects
def variables = migration.variableRepository.listAll()
```

---

# Migration Model Objects

## Top-Level Objects

### DocumentObject

Represents templates, pages, blocks, and sections.

**Builder:** `DocumentObjectBuilder(id: String, type: DocumentObjectType)`

**Four DocumentObject Types:**

| Type         | Purpose                                                 |
|--------------|---------------------------------------------------------|
| **Template** | Top-level entry point / parent document composition     |
| **Page**     | Page with dimensions and layout                         |
| **Block**    | Reusable content part (text, tables, images, etc.)      |
| **Section**  | Container for blocks/other sections (no direct content) |

**The `internal` Flag:**

- `internal: false` (default) - Standalone, reusable objects deployed as separate files
- `internal: true` - Inline content rendered only at point of use, not reused elsewhere

**Choosing the Right Type:**

- Top-level entry point → **Template**
- Has page dimensions → **Page**
- Contains content (text/tables/images) → **Block**
- Container for blocks/sections → **Section**
- Reused in multiple places → `internal: false`
- Used once, inline → `internal: true`

**Properties:**

- `type: DocumentObjectType` - One of: `Template`, `Page`, `Block`, `Section`
- `content: List<DocumentContent>` - Content elements (paragraphs, areas, tables, etc.)
- `internal: Boolean` - If true, not exported as separate file (default: false)
- `targetFolder: String?` - Deployment folder path (supports nesting: "folder1/folder2")
- `displayRuleRef: DisplayRuleRef?` - Conditional display rule
- `variableStructureRef: VariableStructureRef?` - Associated variable structure
- `baseTemplate: String?` - Override default base template path
- `options: DocumentObjectOptions?` - Type-specific options (e.g., PageOptions for page dimensions)
- `metadata: Map<String, List<MetadataPrimitive>>` - Additional metadata for source system
- `skip: SkipOptions` - Mark object to be skipped with reason

**Example:**

```groovy
def template = new DocumentObjectBuilder("template-1", DocumentObjectType.Template)
        .name("Invoice Template")
        .targetFolder("templates/invoices")
        .displayRuleRef("display-rule-1")
        .paragraph { it.text { it.string("Hello") } }
        .documentObjectRef("inline-1")
        .build()

// Inline block example
def inlineBlock = new DocumentObjectBuilder("inline-1", DocumentObjectType.Block)
        .internal(true)
        .string("Inner text content")
        .addCustomField("sourceType", "textBlock")
        .build()
```

Common methods: `.name()`, `.targetFolder()`, `.internal()`, `.displayRuleRef()`, `.variableStructureRef()`,
`.metadata()`, `.skip()`

### Variable

**Builder:** `VariableBuilder(id: String)` | **Key Properties:** `dataType` (required), `defaultValue`

```groovy
def customerName = new VariableBuilder("customer-name")
        .dataType(DataType.String)  // DateTime, Integer, Integer64, Double, Boolean, Currency, Array, SubTree
        .defaultValue("Unknown")
        .originLocations(["source/variables.xml"])
        .build()
```

**Array and SubTree variable types** represent data collection nodes used for repeated content:

- `DataType.Array` — A repeatable group of child variables (e.g., a list of transactions). Used as the iteration source for `repeatedContent` and `addRepeatedRow`.
- `DataType.SubTree` — A nested object grouping related child variables under a common path (e.g., an address sub-object).

```groovy
// Array variable — iterates over a collection
def transactionsArrayVariable = new VariableBuilder("transactionsArray").name("Transactions")
        .dataType(DataType.Array).build()

// SubTree variable — groups related fields under a nested path
def addressSubtreeVariable = new VariableBuilder("addressSubtree").name("Address")
        .dataType(DataType.SubTree).build()

// Scalar child variables belonging to the array
def transactionAmountVariable = new VariableBuilder("transactionAmount").name("amount")
        .dataType(DataType.Currency).build()
```

### DisplayRule

**Builder:** `DisplayRuleBuilder(id: String)` | Conditional logic with comparison operators and groups (And/Or)

```groovy
// Simple comparison
def rule = new DisplayRuleBuilder("rule-1")
        .comparison { it.variable("status").equals().value("active") }
        .build()

// Complex: Group with Or operator
def rule2 = new DisplayRuleBuilder("rule-2")
        .group {
            it.operator(GroupOp.Or)
            it.comparison { it.variable("age").greaterOrEqualThan().value(18) }
            it.comparison { it.variable("country").equals().value("US") }
        }
        .build()
```

**Operators:** `equals`, `notEquals`, `greaterThan`, `greaterOrEqualThan`, `lessThan`, `lessOrEqualThan` (case-sensitive
and case-insensitive variants available)

**Note:** All comparisons use `.value(...)` which accepts String, Number, Boolean, etc.

**Referencing an existing display rule (aliasing):** Use `.targetId()` to create a display rule that delegates to
another already-defined rule. This is useful when deploying shared/external rules.

```groovy
// Define the actual rule
def actualRule = new DisplayRuleBuilder("actual-rule")
        .comparison { it.value(true).equals().variable(someVariable) }
        .build()

// Create an alias that points to the actual rule
def aliasRule = new DisplayRuleBuilder("alias-rule")
        .targetId(actualRule)  // or .targetId("actual-rule") by ID string
        .build()
```

**External display rules:** Mark a display rule as non-internal (reusable/shared) with `.internal(false)` and
optionally add a human-readable `.subject()` for identification:

```groovy
def externalRule = new DisplayRuleBuilder("shared-visibility-rule")
        .internal(false)
        .subject("Shows address block when any address field is non-empty")
        .variableStructureRef(variableStructure)
        .group {
            it.operator(GroupOp.Or)
            it.comparison { it.variable(nameVariable).notEquals().value("") }
            it.comparison { it.variable(cityVariable).notEquals().value("") }
        }.build()
```

### Image

**Builder:** `ImageBuilder(id: String)` | **Key Properties:** `sourcePath`, `imageType`, `options`, `targetFolder`

```groovy
def logo = new ImageBuilder("logo-image")
        .sourcePath("images/logo.png")
        .imageType(ImageType.Png)  // Bmp, Gif, Jpeg, Png, Tga, Tiff, Svg, Unknown
        .options(new ImageOptions(Size.ofMillimeters(50), Size.ofMillimeters(30)))
        .targetFolder("images/logos")
        .subject("Company logo")           // Human-readable label
        .alternateText("Company logo image") // PDF accessibility alternate text
        .build()
```

### Attachment

**Builder:** `AttachmentBuilder(id: String)` | **Key Properties:** `sourcePath`, `attachmentType`, `targetFolder`

```groovy
def pdfAttachment = new AttachmentBuilder("terms-and-conditions")
        .sourcePath("attachments/terms.pdf")
        .attachmentType(AttachmentType.Pdf)  // Pdf, Xml, Other
        .targetFolder("attachments/legal")
        .build()
```

**Note:** Attachments represent files that should be included with the output (e.g., PDF attachments in emails).

### TextStyle

**Builder:** `TextStyleBuilder(id: String)` | **Properties:** `fontFamily`, `size`, `bold`, `italic`, `underline`,
`foregroundColor`, `backgroundColor`

```groovy
def heading1 = new TextStyleBuilder("heading1")
        .definition {
            it.fontFamily("Arial")
            it.size(Size.ofPoints(18))
            it.bold(true)
            it.foregroundColor("#000000")
        }
        .build()
```

**Important**: Style builders require `.definition()`. If to be defined later, create styles with empty definition using
lambda:

```groovy
new TextStyleBuilder("style-id")
        .definition {}  // Empty lambda
        .build()
```

### ParagraphStyle

**Builder:** `ParagraphStyleBuilder(id: String)` | **Properties:** `leftIndent`, `rightIndent`, `firstLineIndent`,
`spaceBefore`, `spaceAfter`, `alignment` (Left/Right/Center/Justify), `lineSpacing`, `keepWithNextParagraph`, `tabs`,
`pdfTaggingRule`

```groovy
def bodyStyle = new ParagraphStyleBuilder("body-style")
        .definition {
            it.leftIndent(Size.ofMillimeters(10))
            it.alignment(Alignment.Justify)
            it.lineSpacing(new LineSpacing.MultipleOf(1.5))
        }
        .build()

// With PDF accessibility tagging (useful for heading structure in PDF output)
def headingStyle = new ParagraphStyleBuilder("heading-style")
        .definition {
            it.spaceAfter(Size.ofMillimeters(3))
            it.pdfTaggingRule(ParagraphPdfTaggingRule.Heading1)  // Heading, Heading1-Heading6, Paragraph
        }
        .build()
```

**Important**: Style builders require `.definition()`. If to be defined later, create styles with empty definition using
lambda:

```groovy
new ParagraphStyleBuilder("style-id")
        .definition {} // Empty lambda
        .build()
```

### VariableStructure

Maps variable paths for data binding.

**Builder:** `VariableStructureBuilder(id: String)`

**Properties:**

- `structure: Map<String, VariablePathData>` - Variable ID to path mapping
- `languageVariable: VariableRef?` - Variable used for language selection

`addVariable` supports two path styles — a literal dot-notation string path, or a reference to an Array/SubTree
variable object. Both are equivalent; the variable reference style is preferred when the variable is already defined.

```groovy
def varStructure = new VariableStructureBuilder("main-structure")
        .name("Main Data Structure")
        // Literal path (dot-notation into the data tree)
        .addVariable("customer-name", "Data.Clients.Value")
        .addVariable("invoice-date", "Data.invoice.date")
        // Reference to an Array variable (equivalent to the literal path above)
        .addVariable(cityVariable.id, clientsArrayVariable)
        // Reference to an Array variable with an explicit name override
        .addVariable(jobsArrayVariable.id, clientsArrayVariable, "Jobs")
        // Root-level literal path for an Array variable itself
        .addVariable(clientsArrayVariable.id, "Data")
        .languageVariable("lang-var")
        .build()
```

## Content Elements (DocumentContent)

**Important**: Content elements are **not** top-level migration objects. They do not have `customFields`,
`originLocations`, or `name` properties. Only use these properties on top-level objects (DocumentObject, Variable,
DisplayRule, Image, TextStyle, ParagraphStyle, VariableStructure).

### Paragraph

Text content with inline formatting.

**Builder:** `ParagraphBuilder`

**Structure:**

- Contains `List<Text>` where each Text has:
    - `content: List<TextContent>` - String values, variables, images, document refs, tables, firstMatch
    - `styleRef: TextStyleRef?` - Text style reference
    - `displayRuleRef: DisplayRuleRef?` - Conditional display

**Example:**

```groovy
// Paragraph is typically used within DocumentObjectBuilder
def block = new DocumentObjectBuilder("my-block", DocumentObjectType.Block)
        .paragraph {
            it.styleRef("body-para-style")
            it.displayRuleRef("show-if-active")

            // Simple string content - use addText().string()
            it.addText().string("Simple text content")

            // Complex text with styling - use .text { } lambda
            it.text {
                it.styleRef("bold-text-style")
                it.string("Customer: ")
            }
            it.text {
                it.variableRef("customer-name")
            }
        }
        .build()

// Or create standalone for nested use
def standalone = new ParagraphBuilder()
        .styleRef("body-para-style")
        .addText().string("Simple text")  // Simple text
        .text {                            // Complex text with styling
            it.styleRef("bold-text-style")
            it.string("Styled content")
        }
        .build()
```

**TextBuilder Methods:**

- `.string("text")` - Add plain string content (most common for simple text)
- `.variableRef("variable-id")` - Add variable reference
- `.imageRef("image-id")` - Add inline image
- `.attachmentRef("attachment-id")` - Add attachment reference
- `.hyperlink("url", "display text")` - Add clickable hyperlink
- `.documentObjectRef("block-id")` - Add nested block reference
- `.table { }` - Add inline table
- `.firstMatch { }` - Add inline conditional content
- `.qrCode { }` - Add a QR code (see [Barcode / QR Code](#barcode--qr-code))
- `.code39Barcode { }` - Add a Code 39 barcode (see [Barcode / QR Code](#barcode--qr-code))

**Note:** For simple text paragraphs, use `addText().string("text")`. For complex styled/formatted text, use `.text { }`
lambda builder.

### Area

Positioned or flow content container on a page object.

**Builder:** `AreaBuilder`

**Properties:**

- `content: List<DocumentContent>` - Nested content
- `position: Position?` - Absolute position (x, y, width, height)
- `interactiveFlowName: String?` - Flow name in base template
- `flowToNextPage: Boolean` - Whether content can flow to next page (default: false)

**Example:**

```groovy
// Area is typically used within DocumentObjectBuilder
def page = new DocumentObjectBuilder("my-page", DocumentObjectType.Page)
        .area {
            it.position {
                it.left(Size.ofMillimeters(10))
                it.top(Size.ofMillimeters(10))
                it.width(Size.ofMillimeters(190))
                it.height(Size.ofMillimeters(50))
            }
            it.flowToNextPage(true)
            it.paragraph { it.addText().string("Header content") }
            it.documentObjectRef("nested-block")
            it.imageRef("logo")
        }
        .build()
```

### Repeated Content

Repeats a block of content once per element in an Array variable. Use `.repeatedContent(variable) { }` on a
`DocumentObjectBuilder` (or `AreaBuilder`) to iterate over a collection and render a content per item.
Accepts a Variable object, a VariableRef, or a literal dot-notation path string.

```groovy
// Renders one paragraph per element in jobsArrayVariable
def jobListBlock = new DocumentObjectBuilder("jobList", DocumentObjectType.Block)
        .internal(true)
        .repeatedContent(jobsArrayVariable) {
            it.paragraph {
                it.text { it.string("Job: ").variableRef(jobNameVariable) }
            }
        }
        .build()

// Equivalent using a literal path
def jobListBlock2 = new DocumentObjectBuilder("jobList2", DocumentObjectType.Block)
        .internal(true)
        .repeatedContent("Data.Clients.Value.Jobs") {
            it.paragraph {
                it.text { it.string("Job: ").variableRef(jobNameVariable) }
            }
        }
        .build()
```

**Note:** For repeated table rows, use `addRepeatedRow` on `TableBuilder` instead.

### Table

Tabular data structure.

**Builder:** `TableBuilder`

**Structure:**

- `rows: List<Row>` - Static table rows; use `addRow { }`
- `repeatedRows: List<RepeatedRow>` - Dynamic rows driven by an Array variable; use `addRepeatedRow(variable) { }`
- `columnWidths: List<ColumnWidth>` - Column sizing
- Row contains `cells: List<Cell>` (added via `addCell { }`) and optional `displayRuleRef`
- Cell contains content, merge flags, border, alignment, height, and overflow options

**Table-level options:**

| Method | Purpose                                                                   |
|---|---------------------------------------------------------------------------|
| `addColumnWidth(size, percent)` | Define a column                                                           |
| `percentWidth(pct)` | Set table width as % of available space                                   |
| `minWidth(size)` / `maxWidth(size)` | Width bounds                                                              |
| `alignment(TableAlignment.xxx)` | Left / Center / Right / Inherit                                           |
| `border { }` | Table-level border (see BorderOptionsBuilder below)                       |
| `tableStyleName("name")` | Reference an existing Designer table style by name                        |
| `pdfTaggingRule(TablePdfTaggingRule.xxx)` | PDF accessibility role: None / Default / Table / Artifact                 |
| `pdfAlternateText("text")` | PDF alternate text for the table                                          |
| `action(TableAction.Flatten)` | Mark table for flattening during deployment (default: `TableAction.Keep`) |
| `name("name")` | Table display name                                                        |

**Row types:**

- `addFirstHeaderRow { }` — Header row shown only on the **first** page when the table spans pages
- `addHeaderRow { }` — Header row repeated on **every** page when the table spans pages
- `addRow { }` — Regular body row
- `addFooterRow { }` — Footer row repeated on every page when the table spans pages
- `addLastFooterRow { }` — Footer row shown only on the **last** page when the table spans pages
- `addRepeatedRow(arrayVariable) { }` — Dynamic rows iterated over an Array variable (see below)

**Cell-level options** (inside `addCell { }`):

| Method | Purpose |
|---|---|
| `border { }` | Cell-specific border and padding |
| `alignment(CellAlignment.xxx)` | Top / Center / Bottom |
| `heightFixed(size)` | Fixed row height |
| `overflow(CellOverflow.xxx)` | `OverflowContentToNextPage` (default) or `MoveCellToNextPage` |
| `mergeLeft = true` | Merge this cell with the one to its left |
| `mergeUp = true` | Merge this cell with the one above it |
| `displayRuleRef("rule-id")` | Conditional display for this cell |

**BorderOptionsBuilder** (used for both table and cell `.border { }` closures):

```groovy
it.border {
    it.allBorders(Color.fromHex("#000000"), Size.ofMillimeters(0.3))  // set all four sides at once
    it.leftLine(color, width)    // or set sides individually
    it.rightLine(color, width)
    it.topLine(color, width)
    it.bottomLine(color, width)
    it.padding(Size.ofMillimeters(2))   // uniform padding
    it.paddingLeft(size)                // or per-side padding
    it.paddingRight(size)
    it.paddingTop(size)
    it.paddingBottom(size)
    it.fill(Color.fromHex("#F0F0F0"))   // background fill color
}
```

**Example — static table with borders and PDF tagging:**

```groovy
def borderColor = Color.fromHex("#000000")
def borderWidth = Size.ofMillimeters(0.3)

def myTable = new TableBuilder()
        .pdfTaggingRule(TablePdfTaggingRule.Table)
        .pdfAlternateText("Invoice line items")
        .addColumnWidth(Size.ofMillimeters(60), 33)
        .addColumnWidth(Size.ofMillimeters(60), 33)
        .addColumnWidth(Size.ofMillimeters(60), 34)
        .percentWidth(100)
        .border { it.allBorders(borderColor, borderWidth) }

        .addFirstHeaderRow {
            it.addCell {
                it.border { it.allBorders(borderColor, borderWidth).padding(Size.ofMillimeters(2)) }
                it.paragraph { it.string("Description") }
            }
            it.addCell {
                it.border { it.allBorders(borderColor, borderWidth).padding(Size.ofMillimeters(2)) }
                it.paragraph { it.string("Qty") }
            }
            it.addCell {
                it.border { it.allBorders(borderColor, borderWidth).padding(Size.ofMillimeters(2)) }
                it.paragraph { it.string("Amount") }
            }
        }

        .addRow {
            it.addCell {
                it.border { it.allBorders(borderColor, borderWidth) }
                it.paragraph { it.string("Item 1") }
                it.heightFixed(Size.ofMillimeters(10))
            }
            it.addCell {
                it.mergeLeft = true  // Merge with cell to the left
                it.border { it.allBorders(borderColor, borderWidth) }
                it.paragraph { it.string("1") }
            }
            it.addCell {
                it.border { it.allBorders(borderColor, borderWidth) }
                it.paragraph { it.string("$100") }
            }
        }
        .build()
```

**Repeated rows — dynamic rows driven by an Array variable:**

Use `addRepeatedRow(variable) { }` to create a group of rows that repeat once per element of an Array variable.
Inside the closure, add one or more `addRow { }` calls (even nested `addRepeatedRow` for nested arrays).
Accepts a Variable object, a VariableRef, or a literal dot-notation path string.

```groovy
def transactionsTable = new TableBuilder()
        .addColumnWidth(Size.ofMillimeters(80), 50)
        .addColumnWidth(Size.ofMillimeters(80), 50)
        .border { it.allBorders(Color.fromHex("#000000"), Size.ofMillimeters(0.3)) }

        .addFirstHeaderRow {
            it.addCell { it.paragraph { it.string("Account") } }
            it.addCell { it.paragraph { it.string("Amount") } }
        }

        .addRepeatedRow(transactionsArrayVariable) {
            it.displayRuleRef("show-if-transaction")  // optional display rule on the repeated group
            it.addRow {
                it.addCell { it.paragraph { it.text { it.variableRef(transactionAccountVariable) } } }
                it.addCell { it.paragraph { it.text { it.variableRef(transactionAmountVariable) } } }
            }
        }
        .build()
```

**Table style reference** — use an existing named style defined in the existing style definition:

```groovy
def styledTable = new TableBuilder()
        .tableStyleName("MyExistingTableStyle")
        // add columns and rows as usual; style provides default formatting
        .addRow { it.addCell { it.paragraph { it.string("Value") } } }
        .build()
```

**Table flattening** — mark a table to be converted to a flat (non-table) layout during deployment:

```groovy
def flattenedTable = new TableBuilder()
        .action(TableAction.Flatten)
        .addRow { it.addCell { it.paragraph { it.string("Content") } } }
        .build()
```

### Column Layout

Applies a multi-column layout to content within a block. When added to a `DocumentObjectBuilder`, the paragraphs that
follow it are rendered in the specified number of columns. Depending on `applyTo`, it may affect only the current block
or the entire template.

**Builder:** `ColumnLayoutBuilder` (used via `.columnLayout { }` on a DocumentObjectBuilder)

**Properties:**

| Method | Purpose |
|---|---|
| `numberOfColumns(n)` | Number of columns |
| `gutterWidth(size)` | Space between columns |
| `balancingType(ColumnBalancingType.xxx)` | `Balanced` (equal height) or other balancing mode |
| `applyTo(ColumnApplyTo.xxx)` | Whether the layout applies to the current block or the whole template |

```groovy
def columnBlock = new DocumentObjectBuilder("two-column-block", DocumentObjectType.Block)
        .internal(true)
        .columnLayout {
            it.numberOfColumns(2)
            it.gutterWidth(Size.ofMillimeters(5))
            it.balancingType(ColumnBalancingType.Balanced)
        }
        .paragraph { it.text { it.string("Column content here...") } }
        .build()
```

### Shape

A vector drawing element (lines, polygons, ellipses) that can be placed directly in a `DocumentObjectBuilder` content.
Shapes are positioned absolutely via their `position` and defined by drawing commands.

**Builder:** `ShapeBuilder` (import from `com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.ShapeBuilder`)

**Properties:**

| Method | Purpose |
|---|---|
| `position { }` | Absolute position and bounding box (uses `PositionBuilder`) |
| `fill(color)` | Shape fill color |
| `lineFill(color)` | Stroke/outline color |
| `lineWidth(size)` | Stroke width |
| `moveTo(x, y)` | Move drawing cursor |
| `lineTo(x, y)` | Draw a straight line |
| `bezierTo(...)` | Draw a cubic Bézier curve |
| `square(position)` | Draw a rectangle |
| `ellipse(position)` | Draw an ellipse |
| `triangle(position)` | Draw a triangle |
| `name("name")` | Human-readable label |

Shapes are added directly to `DocumentObjectBuilder` (or `AreaBuilder`) using `.shape(shapeInstance)`:

```groovy
import com.quadient.migration.api.dto.migrationmodel.builder.documentcontent.ShapeBuilder

// Horizontal rule separator line
def separator = new ShapeBuilder()
        .name("separator")
        .position {
            it.left(Size.ofMillimeters(10))
            it.top(Size.ofMillimeters(32))
            it.width(Size.ofMillimeters(190))
            it.height(Size.ofMillimeters(0.2))
        }
        .lineFill(Color.fromHex("#000000"))
        .moveTo(Size.ofMillimeters(0), Size.ofMillimeters(0))
        .lineTo(Size.ofMillimeters(190), Size.ofMillimeters(0))
        .build()

def page = new DocumentObjectBuilder("page", DocumentObjectType.Page)
        .options(new PageOptions(pageWidth, pageHeight))
        .shape(separator)       // Shape added at document object level
        .area { /* ... */ }
        .build()
```

### Barcode / QR Code

Barcodes and QR codes can be placed in two ways:

- **Inline** — inside a `TextBuilder` (within a paragraph text run), flows with the text
- **Direct** — added directly to a `DocumentObjectBuilder` or `AreaBuilder` with an absolute position, like a shape

Both `QrCodeBuilder` and `Code39BarcodeBuilder` support `.position { }` for absolute placement and `.variableRef(variable)` to source the encoded data from a variable instead of a literal string.

**QR Code — inline (inside text):**

```groovy
it.text {
    it.qrCode {
        it.data("https://example.com")                      // literal data
        it.errorCorrection(QrCodeErrorCorrectionLevel.M)    // L, M, Q, H
        it.size(QrCodeSize.Auto)
        it.moduleWidth(Size.ofMillimeters(0.58))
        it.quietZone(Size.ofMillimeters(2))
        it.barcodeFill(Color.fromHex("#000000"))            // optional
        it.backgroundFill(Color.fromHex("#FFFFFF"))         // optional
    }
}
```

**QR Code — absolutely positioned on a page/block:**

```groovy
def page = new DocumentObjectBuilder("page", DocumentObjectType.Page)
        .options(new PageOptions(pageWidth, pageHeight))
        .qrCode {
            it.position {
                it.left(Size.ofMillimeters(150))
                it.top(Size.ofMillimeters(10))
                it.width(Size.ofMillimeters(30))
                it.height(Size.ofMillimeters(30))
            }
            it.variableRef(qrDataVariable)    // data from a variable
            it.errorCorrection(QrCodeErrorCorrectionLevel.M)
            it.moduleWidth(Size.ofMillimeters(0.58))
        }
        .area { /* ... */ }
        .build()
```

**Code 39 barcode — inline (inside text):**

```groovy
it.text {
    it.code39Barcode {
        it.data("ABC-123")
        it.height(Size.ofMillimeters(15))
        it.moduleWidth(Size.ofMillimeters(0.5))
        it.useControlSum(true)
        it.barcodeFill(Color.fromHex("#000000"))
    }
}
```

**Code 39 barcode — absolutely positioned:**

```groovy
def page = new DocumentObjectBuilder("page", DocumentObjectType.Page)
        .code39Barcode {
            it.position {
                it.left(Size.ofMillimeters(10))
                it.top(Size.ofMillimeters(250))
                it.width(Size.ofMillimeters(80))
                it.height(Size.ofMillimeters(15))
            }
            it.variableRef(barcodeDataVariable)
            it.useControlSum(true)
        }
        .build()
```

**Required imports:**

```groovy
import com.quadient.migration.shared.QrCodeErrorCorrectionLevel
import com.quadient.migration.shared.QrCodeSize
```

### FirstMatch

Conditional content block (if-then-else logic).

**Builder:** `FirstMatchBuilder`

**Structure:**

- `cases: List<Case>` - Ordered list of condition-content pairs
- `default: List<DocumentContent>` - Fallback content
- Each Case has: `displayRuleRef: DisplayRuleRef`, `content: List<DocumentContent>`, `name: String?`

**Example:**

```groovy
// FirstMatch is typically used within DocumentObjectBuilder
def block = new DocumentObjectBuilder("conditional-block", DocumentObjectType.Block)
        .firstMatch {
            it.case {
                it.name("Premium Customer")
                it.displayRule("is-premium")
                it.paragraph { it.string("Premium benefits...") }
            }
            it.case {
                it.name("Standard Customer")
                it.displayRule("is-standard")
                it.paragraph { it.string("Standard benefits...") }
            }
            it.defaultParagraph { it.string("Default content") }
        }
        .build()
```

### SelectByLanguage

Language-specific content selection.

**Builder:** `SelectByLanguageBuilder`

**Structure:**

- `cases: List<Case>` - Language variants
- Each Case has: `content: List<DocumentContent>`, `language: String` (ISO code)

**Example:**

```groovy
// SelectByLanguage is typically used within DocumentObjectBuilder
def block = new DocumentObjectBuilder("multilang-block", DocumentObjectType.Block)
        .selectByLanguage {
            it.case {
                it.language("en")
                it.string("Hello")
            }
            it.case {
                it.language("fr")
                it.paragraph { it.text { it.string("Bonjour") } }
            }
            it.case {
                it.language("de")
                it.paragraph { it.string("Guten Tag") }
            }
        }
        .build()
```

### Snippets

Snippets are lightweight reusable content fragments. They are stored like other document objects (in
`documentObjectRepository`) and referenced via `documentObjectRef("snippet-id")` wherever a document object reference
is valid — directly in a block, an area, or inline inside a text run.

**Builder:** `SnippetBuilder(id: String)`

Two snippet modes:

**1. Simple snippet** — combines static strings and variable references in sequence:

```groovy
def snippet = new SnippetBuilder("greeting-snippet")
        .simple()
        .string("Hello, ")
        .variableRef(nameVariable)
        .variableStructureRef(variableStructure)
        .build()

migration.documentObjectRepository.upsert(snippet)

// Referenced at block/area level (standalone, between other content)
def block = new DocumentObjectBuilder("my-block", DocumentObjectType.Block)
        .documentObjectRef(snippet)
        .paragraph { it.string("More content below.") }
        .build()

// Or referenced inline inside a text run
it.text {
    it.styleRef(normalStyle)
    it.documentObjectRef(snippet)
    it.string(" — additional text")
}
```

**2. FirstMatch snippet** — conditional inline text selection (variable strings only, not full DocumentContent):

```groovy
def fmSnippet = new SnippetBuilder("salutation-snippet")
        .firstMatch { fb ->
            fb.case { cb ->
                cb.displayRuleRef(displayRuleFrance)
                cb.string("Bonjour, ")
            }
            .case { cb ->
                cb.displayRuleRef(displayRuleCzechia)
                cb.string("Dobrý den, ")
            }
            .defaultString("Hello, ")
        }
        .variableStructureRef(variableStructure)
        .build()
```

**Note:** FirstMatch snippets only support variable-string content in cases and defaults (i.e., `.string()` and
`.variableRef()`) — not full paragraphs or tables. For those, use a regular `DocumentObjectBuilder` with `.firstMatch { }`.

## Reference Types

All references point to other objects by ID:

- `DocumentObjectRef(id)` or `DocumentObjectRef(id, displayRuleRef)` - Reference to templates/pages/blocks/sections
- `VariableRef(id)` - Reference to variables
- `ImageRef(id)` - Reference to images
- `AttachmentRef(id)` - Reference to attachments
- `TextStyleRef(id)` - Reference to text styles
- `ParagraphStyleRef(id)` - Reference to paragraph styles
- `DisplayRuleRef(id)` - Reference to display rules
- `VariableStructureRef(id)` - Reference to variable structures
- `StringValue(text)` - Plain text content
- `Hyperlink(url, displayText?, alternateText?)` - Clickable hyperlink with optional display and accessibility text

## Supporting Types

### Size

Represents measurements with automatic unit conversion.

**Creation methods:**

```groovy
Size.ofPoints(12.0)
Size.ofMillimeters(10)
Size.ofCentimeters(1.0)
Size.ofInches(0.5)
Size.fromString("10mm")
Size.fromString("12 pt")
```

### Color

RGB color representation.

**Creation methods:**

```groovy
Color.fromHex("#FF0000")
new Color(255, 0, 0)
new Color(1.0, 0.0, 0.0) // normalized 0-1
```

### Position

Rectangular position and size. Usually built using PositionBuilder within area closures:

```groovy
// Within an area builder
it.position {
    it.left(Size.ofMillimeters(10))
    it.top(Size.ofMillimeters(20))
    it.width(Size.ofMillimeters(100))
    it.height(Size.ofMillimeters(50))
}

// Or construct directly if needed
new Position(
        Size.ofMillimeters(10), // x
        Size.ofMillimeters(20), // y
        Size.ofMillimeters(100), // width
        Size.ofMillimeters(50)  // height
)
```

### IcmPath

Path type used in `projectConfig` properties and other places. Call `.toString()` to convert to String:

```groovy
def inputPath = migration.projectConfig.inputDataPath.toString()
```

## Metadata Support

Objects support metadata for source system tracking, organized in named groups:

```groovy
.metadata("DocumentInfo") { mb ->
    mb.string("Document type: Technical Example")
    mb.integer(42L)
    mb.dateTime(Instant.parse("2024-01-15T10:30:00Z"))
}
```

**Types:** `.string()`, `.integer()`, `.float()`, `.boolean()`, `.dateTime()`

## Skip Options

Mark objects that cannot be automatically migrated:

```groovy
.skip("placeholder-name", "reason for skipping")
```

---

# Additional Information

## Validation Rules

The library validates: non-empty `id`, required `dataType` for Variables, consistent table cell counts, valid Size/Color
values, and existence of referenced objects. Validation errors will fail at runtime.

## Working Example

See `migration-examples/src/main/groovy/com/quadient/migration/example/example/Import.groovy` for a complete example
demonstrating all major object types, conditional logic, language-specific content, tables (with borders, PDF tagging,
and repeated rows), shapes, barcodes, column layout, snippets, and document assembly.
