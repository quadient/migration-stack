# API Reference

## Overview

This document describes the Migration Library API used in parsing scripts. Your parser transforms source format data (
XML, JSON, CSV, etc.) into the **Migration Model** - a common intermediate representation for Quadient Inspire content.

All objects are created using **Builder classes** with a fluent API and stored in repositories.

**Related:** See `AGENT.md` for your role and workflow guidance.

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
- **Test Iteratively** - Run your script frequently to catch errors early (see AGENT.md workflow)

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
        .dataType(DataType.String)  // DateTime, Integer, Integer64, Double, Boolean, Currency
        .defaultValue("Unknown")
        .originLocations(["source/variables.xml"])
        .build()
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

### Image

**Builder:** `ImageBuilder(id: String)` | **Key Properties:** `sourcePath`, `imageType`, `options`, `targetFolder`

```groovy
def logo = new ImageBuilder("logo-image")
        .sourcePath("images/logo.png")
        .imageType(ImageType.Png)  // Bmp, Gif, Jpeg, Png, Tga, Tiff, Svg, Unknown
        .options(new ImageOptions(Size.ofMillimeters(50), Size.ofMillimeters(30)))
        .targetFolder("images/logos")
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
`spaceBefore`, `spaceAfter`, `alignment` (Left/Right/Center/Justify), `lineSpacing`, `keepWithNextParagraph`, `tabs`

```groovy
def bodyStyle = new ParagraphStyleBuilder("body-style")
        .definition {
            it.leftIndent(Size.ofMillimeters(10))
            it.alignment(Alignment.Justify)
            it.lineSpacing(new LineSpacing.MultipleOf(1.5))
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

**Example:**

```groovy
def varStructure = new VariableStructureBuilder("main-structure")
        .name("Main Data Structure")
        .addVariable("customer-name", "Data.customer.name")
        .addVariable("invoice-date", "Data.invoice.date")
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

### Table

Tabular data structure.

**Builder:** `TableBuilder`

**Structure:**

- `rows: List<Row>` - Table rows
- `columnWidths: List<ColumnWidth>` - Column sizing
- Row contains `cells: List<Cell>` and optional `displayRuleRef`
- Cell contains `content: List<DocumentContent>`, `mergeLeft: Boolean`, `mergeUp: Boolean`

**Example:**

```groovy
// Table using static DSL method (imported from Dsl class)
import static com.quadient.migration.api.dto.migrationmodel.builder.Dsl.table

def myTable = table {
    it.addColumnWidth(Size.ofMillimeters(50), 0.25)
    it.addColumnWidth(Size.ofMillimeters(100), 0.75)

    // Add header row
    it.row {
        it.displayRuleRef("show-if-has-data")
        it.cell {
            it.paragraph { it.string("Column 1") }
        }
        it.cell {
            it.paragraph { it.text { it.string("Column 2") } }
        }
    }

    // Add data row with merged cell
    it.row {
        it.cell {
            it.mergeLeft = true
            it.paragraph { it.string("Merged cell") }
        }
        it.cell {
            it.paragraph { it.string("Value") }
        }
    }
}
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
. metadata("DocumentInfo") { mb ->
    mb.string("Document type: Technical Example")
    mb.integer(42L)
    mb.dateTime(Instant.parse("2024-01-15T10:30:00Z"))
}
```

**Types:** `.string()`, `.integer()`, `.float()`, `.boolean()`, `.dateTime()`

## Skip Options

Mark objects that cannot be automatically migrated:

```groovy
. skip("placeholder-name", "reason for skipping")
```

---

# Additional Information

## Validation Rules

The library validates: non-empty `id`, required `dataType` for Variables, consistent table cell counts, valid Size/Color
values, and existence of referenced objects. Validation errors will fail at runtime.

## Working Example

See `migration-examples/src/main/groovy/com/quadient/migration/example/example/Import.groovy` for a complete example
demonstrating all major object types, conditional logic, language-specific content, tables, and document assembly.