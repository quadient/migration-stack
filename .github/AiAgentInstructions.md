# AI Agent Instructions: Creating a New Migration Parser

## 1. Objective

The primary goal of this task is to assist a developer in creating a new migration parser for the `quadient/migration-stack` project. The parser will be responsible for reading a specific source file format and converting it into the standardized migration model defined by the `migration-library`.

The AI agent should act as a knowledgeable assistant, guiding the developer through the process, helping to structure the code, and generating boilerplate/scaffolding code where appropriate.

## 2. Core Concepts

Before starting, it's essential to understand the key components of the `migration-stack`:

- **`migration-library`**: This is the core of the stack. It contains a set of helpful functions, builders, and a standardized model for defining migrations. Any new parser **must** use the components from this library to build the final migration output.

- **`migration-examples`**: This directory contains example implementations of parsers. The most important one for reference is **`migration-examples/docbook-example`**. This project serves as the "gold standard" and template for how a new parser should be structured.

- **Groovy**: The parsers are written in Groovy. The AI agent should generate Groovy code.

## 3. Step-by-Step Guide for Creating a New Parser

The agent should follow these steps to help the developer.

### Step 1: Analyze the New Source Format

- **Action:** Ask the user to provide a sample of the source file format that needs to be migrated.
- **Analysis:** The agent should analyze the structure of the provided file (e.g., XML, JSON, proprietary format). Identify the main elements, attributes, and relationships that need to be translated into the migration model.

### Step 2: Review the `docbook-example` Structure

- **Action:** Before writing any code, thoroughly review the structure of the `migration-examples/docbook-example` project.
- **Analysis:** Pay close attention to:
    - The `build.gradle.kts` file to understand project dependencies.
    - The directory structure, especially under `src/main/groovy`.
    - The main parser class (e.g., `DocBookParser.groovy`). Note how it reads the input file and uses the `migration-library` builders.
    - How different elements from the source `docbook` format are mapped to `migration-library` objects.

### Step 3: Scaffold the New Parser Project

- **Action:** Based on the `docbook-example`, propose a directory and file structure for the new parser.
- **Example Proposal:** For a new format called "NewFormat", the agent could suggest:

  ```
  migration-examples/newformat-example
  ├── build.gradle.kts
  └── src
      └── main
          └── groovy
              └── com
                  └── quadient
                      └── migration
                          └── example
                              └── newformat
                                  └── NewFormatParser.groovy
  ```

### Step 4: Generate the Groovy Parser Code

- **Action:** Begin generating the Groovy code for the `NewFormatParser.groovy` file.
- **Implementation Details:**
    1.  Create a class `NewFormatParser`.
    2.  The parser should take an input file/path as a parameter.
    3.  Use Groovy's `XmlParser` or other relevant libraries to read and traverse the source file.
    4.  For each relevant element in the source file, use the builders from `migration-library` to construct the corresponding objects in the target migration model.
    5.  The agent should start by generating a skeleton of the class and then fill in the parsing logic for a few key elements based on the analysis from Step 1.

### Step 5: Iteration and Refinement

- **Action:** Work interactively with the developer.
- **Process:** The developer can provide feedback, and the agent can refine the generated code, add more parsing logic for other elements, and help debug any issues. This is a collaborative process.

## 4. How to Use These Instructions

When a developer starts a new task with the AI agent, they should provide:
1.  A reference to this document (`.github/AIAgentInstructions.md`).
2.  A sample file of the new source format to be parsed.
3.  The desired name for the new parser (e.g., "NewFormat").

By following this guide, the AI agent can provide consistent, structured, and genuinely helpful assistance for building new migration parsers.