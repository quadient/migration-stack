# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)

## [Unreleased]

### Added
- Image export/import now include an imageType field to support image formats.

### Changed
- Split document and image export/import scripts

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

[unreleased]: https://github.com/quadient/migration-stack/compare/v17.0.3...HEAD
[17.0.3]: https://github.com/quadient/migration-stack/compare/v17.0.2..v17.0.3
[17.0.2]: https://github.com/quadient/migration-stack/compare/v0.0.1..v17.0.2
[0.0.1]: https://github.com/quadient/migration-stack/releases/tag/v0.0.1
