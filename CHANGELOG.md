# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) 
and we adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2024-07-13

### Added

- Support for OpenJDK 21 added.
- Integration test for importer added.
- Metric default target column for scale added.
- Metric target columns for direction, debt target and scale added.
- Metric version developer, along with email if provided, are now stored in the 
  LDAP developer table.

### Changed

- Metric defaults uses the project export path instead of
  the root path, as these are now project-dependent.
- Metric defaults have `"base_name"` keys instead of `"class_name"`.
- The since date of metric values is now optional.
- Metric version developer and message columns are now optional.
- Working directory is inferred from `user.dir` instead of `java.class.path`.
- Importers that need a repository ID no longer throw an exception when the 
  repository is not found and instead log and ignore the data object.
- Scripts for database schema validation and recreation can be run without 
  settings configuration.

### Fixed

- Jira status incorrectly linked the status ID to itself as category instead of 
  the status category ID.
- Metric value import handles floating point values correctly.
- TFS work item import was missing encryption field.

### Removed

- Support for quality report data dropped, including metric values from old 
  history, compact history or references to local or networked files.
- Metric target type column dropped.

## [0.0.3] - 2024-06-28

### Added

- Initial release of version as used during the GROS research project. 
  Previously, versions were rolling releases based on Git commits.
- Help usage now displays importer version and defines.

### Fixed

- Local email domain from bundled property configuration can be overridden with 
  `-Dimporter.email_domain=example.org` after `java -jar importerjson.jar`, 
  like with the other properties.

[Unreleased]: 
https://github.com/grip-on-software/monetdb-import/compare/v1.0.0...HEAD
[1.0.0]: 
https://github.com/grip-on-software/monetdb-import/compare/v0.0.3...v1.0.0
[0.0.3]: https://github.com/grip-on-software/monetdb-import/releases/tag/v0.0.3
