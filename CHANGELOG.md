# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)

## [Unreleased]

### Added

### Changed

### Fixed
- Handle SelectByLanguage in complexity report

## [17.0.8] - 2025-11-28

### Added
- Ability to skip document objects and images on purpose

### Changed
- **Breaking** Removed the `Unsupported` document object type

### Fixed
- Language variable boolean value is now correctly parsed in VariablesImport script
- Ensure document object content always contains at least one paragraph

### Removed

## [17.0.7] - 2025-11-12

### Added
- Verify existence of base template during interactive deployment

### Changed
- Flow areas in Designer output no longer have FlowToNextPage option due to performance reasons.

### Fixed

### Removed

## [17.0.6] - 2025-11-07

### Added

- Support for ICM metadata

### Changed

### Fixed

### Removed

## [17.0.5] - 2025-11-06

### Added

- Select by language document content option to specify different content for different languages in a document (recommended minimal Designer version **17.0.635.0**)
- Project config default language option
- Option to set language variable in variable structure that is used in Designer output. Is included in variable
  export/import.

### Changed

### Fixed
- Default value of style definition path in app is now correctly set to null instead of empty string

### Removed

## [17.0.4] - 2025-10-13

### Added
- Image export/import now include an imageType field to support image formats.

### Changed
- Split document and image export/import scripts

### Fixed

- Display rule database migration
- Display rule report failing when origin content was missing

### Removed

## [17.0.3] - 2025-10-10

### Added

- Styles validation against style definition and corresponding gradle tasks
- Font identification based on used text styles and font (subfont) file localization in ICM font root folder
- Project config option to override default font root folder
- Support for function calls in display rules

### Changed

### Removed

### Fixed
- Some chars (.,:?!- etc.) in variable names/paths are now correctly transformed to underscore (_) when used in display rule scripts (condition flows) 

## [17.0.2] - 2025-09-23

### Added
- variable structure allows to override variable names that has priority over default variable name
- `styleDefinitionPath` to project config, allows to specify which style definition to use or deploy

### Changed
- New mapping column in variables import/export scripts called `inspire_name` allows to override variable name in the current variable structure  

### Removed

## [0.0.1] - 2025-09-15

### Added

- This CHANGELOG file

[unreleased]: https://github.com/quadient/migration-stack/compare/v17.0.8...HEAD
[17.0.8]: https://github.com/quadient/migration-stack/compare/v17.0.7..v17.0.8
[17.0.7]: https://github.com/quadient/migration-stack/compare/v17.0.6..v17.0.7
[17.0.6]: https://github.com/quadient/migration-stack/compare/v17.0.5..v17.0.6
[17.0.5]: https://github.com/quadient/migration-stack/compare/v17.0.4..v17.0.5
[17.0.4]: https://github.com/quadient/migration-stack/compare/v17.0.3..v17.0.4
[17.0.3]: https://github.com/quadient/migration-stack/compare/v17.0.2..v17.0.3
[17.0.2]: https://github.com/quadient/migration-stack/compare/v0.0.1..v17.0.2
[0.0.1]: https://github.com/quadient/migration-stack/releases/tag/v0.0.1
