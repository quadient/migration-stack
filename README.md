# Migration Stack

This repository provides a standardized approach for migrating legacy document templates to the Quadient Inspire platform. It is designed for technical users and solution developers who need to automate or streamline the migration process.

The core of the Migration Stack is a migration library, which includes builder utilities to help transform legacy templates into a designated migration data model. The transformed content can be deployed (using the library's deployment services) to Inspire Designer, Inspire Interactive, or Inspire Evolve.

## Repository Structure

| Subproject             | Description                                                                                                                                                                                                                                                                                                                                                      |
|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **migration-examples** | Provides a ready-to-use setup with utilities and configuration templates for developing and running custom migration projects. Migration scripts typically read inputs from legacy formats, convert them into the data model defined in `migration-library`, and deploy them to the Inspire platform. Example migration scripts are also provided for reference. |
| **migration-library**  | Contains the core tools needed for migration projects. It defines the **migration data model** and provides builder utilities, service classes for managing the model, and deployment services for the Inspire platform.                                                                                                                                         |
| **wfd-xml**            | A standalone library for generating Inspire Designer XML files ready for import. In the context of `migration-stack`, it functions as an internal dependency of `migration-library` and is not intended for direct use.                                                                                                                                          |

## Disclaimer

Please note that this project is under active development and breaking changes may occur without warning.
While you are free to use and adapt it, there are no guarantees of stability or backward compatibility.