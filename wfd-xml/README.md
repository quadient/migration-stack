# WfdXml

## Introduction

This Java library provides support for building and converting
Designer Workflow Definition (WFD) files. You can create document definition files in XML format,
and import them into Inspire Designer. In other words, you can programmatically create
Inspire Designer document templates (WFD).

## Prerequisites

- This project is compatible with Java 21.

## Architecture

Three modules **api** and **impl**.

- **api** - This module provides user interface. The names and structures follow 'Inspire Designer GUI'.

- **impl** - The implementation of user interface. The structure corresponds to 'Inspire Designer source codes'.

- **build-logic** - Shared build logic from other modules.

## Getting Started

### Build

```
gradle clean build
```

- Builds the **impl** and **api** JAR files.
- The built artifacts will be in the `api/build/libs` and `impl/build/libs` directories.

### Publish

```
gradle clean publish
```

- Publishes the **impl** and **api** JAR files.
- The published artifacts will be in the `api/build/repo` and `impl/build/repo` directories.

### Test

- Groovy unit, integration and E2E tests.

**E2E Tests**

The implementation is designed based on the test container framework.
Before local use, check the `impl/src/test/resources/docker-compose.yml` file. All the necessary environment
values, which you need to define for a correct test run are exported into the `.env` file. There, you can set 
the version of the ICM and IPS images and license. The mentioned values require approved access to Quadient public 
docker repository and cloud license.

```
gradle clean test
```