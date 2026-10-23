# Changelog

All notable changes to SheetStream will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-23

### Added
- Initial release of SheetStream: zero-dependency, constant O(1) memory streaming Excel (.xlsx) generator.
- Multi-sheet workbook streaming with cross-sheet formula references.
- Pre-baked cell styles: HEADER, SUBHEADER, CURRENCY, INTEGER, DATE, PERCENTAGE, INCOME, EXPENSE, BOLD.
- Kotlin DSL with `sheet`, `row`, `cell`, and `formula` builders.
- Android-friendly `streamPaged` helper for chunked Room DB queries without memory buffering.
- `headerRow`, `subheaderRow`, `rows(Iterable/Sequence)`, and `streamTable` convenience methods.
- Direct `File.sheetStream` and `OutputStream.sheetStream` extension functions.
- Java 8 bytecode compatibility targeting Android API 21+ through Android 15+.
- Automated test suite validating OOXML compliance and pagination.
