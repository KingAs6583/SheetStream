# Changelog

All notable changes to SheetStream will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-24

### Added

- **Core Generator Engine**: Zero-dependency, constant O (1) memory streaming Excel (.xlsx)
  generator using standard Java I/O (`ZipOutputStream`, `OutputStreamWriter`).
- **Streaming SAX Reader Engine**: Built-in O (1) streaming spreadsheet reader (`readSpreadsheet`,
  `SheetReader`, `RowRecord`) supporting both `inlineStr` and Excel `sharedStrings.xml` without
  third-party XML dependencies.
- **Dynamic Custom Styles**: `customStyle { ... }` DSL supporting custom RGB hex background colors,
  font colors, bold/italic, alignment, and custom Excel number formats.
- **Pre-baked Cell Styles**: Instant styling with zero XML boilerplate (`HEADER`, `SUBHEADER`,
  `CURRENCY`, `INTEGER`, `DATE`, `PERCENTAGE`, `INCOME`, `EXPENSE`, `BOLD`).
- **Freeze Panes**: `freezeHeaderRow()` and `freezePanes(rows, cols)` to pin rows/columns on mobile
  and desktop viewports.
- **Kotlin DSL**: Fluent builders for `sheetStream`, `sheet`, `row`, `cell`, and `formula` with
  cross-sheet formula references.
- **Android Integration**: `streamPaged` helper for chunked Room DB queries without buffering entire
  tables in memory.
- **Safety Limits & Guardrails**: `ReaderConfig` and `SheetStreamLimitExceededException` protecting
  against out-of-spec worksheets (1,048,576 rows, 16,384 columns, file size limits).
- **Target Compatibility**: Java 8 bytecode targeting Android API 21+ and standard JVM with zero
  `android.*` imports in the core engine.
- **Automated CI/CD**: GitHub Actions workflows for continuous integration with Gradle dependency
  caching and automated tag-triggered GitHub Releases.
